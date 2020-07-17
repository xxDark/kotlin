/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedClassDescriptor
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrScriptSymbolImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.psi.psiUtil.pureEndOffset
import org.jetbrains.kotlin.psi.psiUtil.pureStartOffset
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement

internal val scriptToClassPhase = makeIrModulePhase(
    ::ScriptToClassLowering,
    name = "ScriptToClass",
    description = "Put script declarations into a class",
    stickyPostconditions = setOf(::checkAllFileLevelDeclarationsAreClasses)
)

private class ScriptToClassLowering(val context: JvmBackendContext) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        val scriptClasses = irFile.declarations.filterIsInstance<IrScript>().map {
            makeScriptClass(irFile, it)
        }
        irFile.declarations.clear()
        irFile.declarations.addAll(scriptClasses)
    }

    private fun makeScriptClass(irFile: IrFile, irScript: IrScript): IrClass {
        val fileEntry = irFile.fileEntry
        val ktFile = context.psiSourceManager.getKtFile(fileEntry as PsiSourceManager.PsiFileEntry)
            ?: throw AssertionError("Unexpected file entry: $fileEntry")
        val descriptor = WrappedClassDescriptor(sourceElement = KotlinSourceElement(ktFile))
        return IrClassImpl(
            0, fileEntry.maxOffset,
            IrDeclarationOrigin.SCRIPT_CLASS,
            symbol = IrClassSymbolImpl(descriptor),
            name = irScript.name,
            kind = ClassKind.CLASS,
            visibility = Visibilities.PUBLIC,
            modality = Modality.FINAL
        ).also { irScriptClass ->
            descriptor.bind(irScriptClass)
            irScriptClass.superTypes += context.irBuiltIns.anyType
            irScriptClass.parent = irFile
            irScriptClass.createImplicitParameterDeclarationWithWrappedDescriptor()
            val symbolRemapper = DeepCopySymbolRemapperToScriptClass(irScript.symbol, irScriptClass.symbol)
            val deepCopyTransformer = DeepCopyIrTreeWithSymbols(symbolRemapper, DeepCopyTypeRemapper(symbolRemapper))
            irScriptClass.thisReceiver = irScript.thisReceiver.run {
                acceptVoid(symbolRemapper)
                transform(deepCopyTransformer, null)
            }
            irScript.declarations.forEach {
                it.acceptVoid(symbolRemapper)
                val copy = it.transform(deepCopyTransformer, null).patchDeclarationParents<IrElement>(irScriptClass) as IrDeclaration
                irScriptClass.declarations.add(copy)
            }
//            val initializer =
//                IrAnonymousInitializerImpl(
//                    irScript.startOffset, irScript.endOffset,
//                    IrDeclarationOrigin.SCRIPT_STATEMENT,
//                    IrAnonymousInitializerSymbolImpl(descriptor)
//                ).apply {
//                    body = context.createIrBuilder(this.symbol).irBlockBody {
//                        irScript.statements.forEach {
//                            it.acceptVoid(symbolRemapper)
//                            +((it.transform(deepCopyTransformer, null).patchDeclarationParents<IrElement>(irScriptClass)) as IrStatement)
//                        }
//                    }
//                    parent = irScriptClass
//                }
//            irScriptClass.declarations.add(initializer)
            irScriptClass.addConstructor {
                isPrimary = true
            }.apply {
                addValueParameter(name = "args", type = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.stringType))
                body = context.createIrBuilder(this.symbol).irBlockBody {
                    +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                    +IrInstanceInitializerCallImpl(
                        irScript.startOffset, irScript.endOffset,
                        irScriptClass.symbol,
                        context.irBuiltIns.unitType
                    )
                    irScript.statements.forEach {
                        it.acceptVoid(symbolRemapper)
                        +((it.transform(deepCopyTransformer, null).patchDeclarationParents<IrElement>(irScriptClass)) as IrStatement)
                    }
                }
            }
            irScriptClass.annotations += irFile.annotations
            irScriptClass.metadata = irFile.metadata
        }
    }

    object DECLARATION_ORIGIN_FIELD_FOR_SCRIPT_VARIABLE :
        IrDeclarationOriginImpl("FIELD_FOR_SCRIPT_VARIABL", isSynthetic = true)

    private class DeepCopySymbolRemapperToScriptClass(
        val scriptSymbol: IrScriptSymbol,
        val scriptClassSymbol: IrClassSymbol
    ) : DeepCopySymbolRemapper() {

        override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol =
            super.getReferencedClassifier(
                if (symbol == scriptSymbol) scriptClassSymbol
                else symbol
            )

        private val scripts = hashMapOf<IrScriptSymbol, IrScriptSymbol>()

        override fun visitScript(declaration: IrScript) {
            remapSymbol(scripts, declaration) {
                IrScriptSymbolImpl(it.descriptor)
            }
            declaration.acceptChildrenVoid(this)
        }
    }

    fun <T : IrElement> T.patchDeclarationParentsToScriptClass(
        script: IrScript,
        scriptClass: IrClass
    ) = apply {
        val visitor = object : IrElementVisitorVoid {

            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitDeclaration(declaration: IrDeclaration) {
                if (declaration.parent == script) {
                    declaration.parent = scriptClass
                }
                super.visitDeclaration(declaration)
            }
        }
        acceptVoid(visitor)
    }
}
