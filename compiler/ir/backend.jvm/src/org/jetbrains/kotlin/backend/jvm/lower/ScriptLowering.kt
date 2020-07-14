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
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedClassDescriptor
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
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
            irScript.statements.forEach { scriptStatement ->
                if (scriptStatement is IrDeclaration) {
                    irScriptClass.declarations.add(scriptStatement.apply { parent = irScriptClass })
                } else {
                    val initializer =
                        IrAnonymousInitializerImpl(
                            scriptStatement.startOffset, scriptStatement.endOffset,
                            IrDeclarationOrigin.SCRIPT_STATEMENT,
                            IrAnonymousInitializerSymbolImpl(descriptor)
                        ).also { initializer ->
                            initializer.body =
                                IrBlockBodyImpl(scriptStatement.startOffset, scriptStatement.endOffset, listOf(scriptStatement))
                            initializer.parent = irScriptClass
                        }
                    irScriptClass.declarations.add(initializer)
                }
            }
            irScriptClass.addConstructor {
                isPrimary = true
            }.apply {
                addValueParameter(name = "args", type = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.stringType))
                body = context.createIrBuilder(this.symbol).irBlockBody {
                    +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                }
            }
            irScriptClass.annotations += irFile.annotations
            irScriptClass.metadata = irFile.metadata
        }
    }
}
