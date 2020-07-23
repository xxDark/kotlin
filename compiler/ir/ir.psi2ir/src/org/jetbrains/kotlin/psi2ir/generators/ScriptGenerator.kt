/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.varargElementType
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.psiUtil.pureEndOffset
import org.jetbrains.kotlin.psi.psiUtil.pureStartOffset
import org.jetbrains.kotlin.resolve.BindingContext

class ScriptGenerator(declarationGenerator: DeclarationGenerator) : DeclarationGeneratorExtension(declarationGenerator) {
    fun generateScriptDeclaration(ktScript: KtScript): IrDeclaration? {
        val descriptor = getOrFail(BindingContext.DECLARATION_TO_DESCRIPTOR, ktScript) as ScriptDescriptor

        val existedScripts = context.symbolTable.listExistedScripts()

        return context.symbolTable.declareScript(descriptor).buildWithScope { irScript ->

            // TODO: since script could reference instances of previous one their receivers have to be enlisted in its scope
            // Remove this code once script is no longer represented by Class
            existedScripts.forEach { context.symbolTable.introduceValueParameter(it.owner.thisReceiver) }

            val startOffset = ktScript.pureStartOffset
            val endOffset = ktScript.pureEndOffset

            fun makeReceiver(descriptor: ClassDescriptor): IrValueParameter {
                val receiverParameterDescriptor = descriptor.thisAsReceiverParameter
                return context.symbolTable.declareValueParameter(
                    startOffset, endOffset,
                    IrDeclarationOrigin.INSTANCE_RECEIVER,
                    receiverParameterDescriptor,
                    receiverParameterDescriptor.type.toIrType()
                ).also { it.parent = irScript }
            }

            irScript.thisReceiver = makeReceiver(descriptor)

            irScript.baseClass = descriptor.typeConstructor.supertypes.single().toIrType()
            irScript.implicitReceivers = descriptor.implicitReceivers.map(::makeReceiver)

            for (d in ktScript.declarations) {
                if (d is KtScriptInitializer) irScript.statements += BodyGenerator(
                    irScript.symbol,
                    context
                ).generateExpressionBody(d.body!!).expression
                else irScript.declarations += declarationGenerator.generateMemberDeclaration(d)!!
            }

            descriptor.resultValue?.let { resultDescriptor ->
                val resultProperty = resultDescriptor.toIrProperty(startOffset, endOffset, IrDeclarationOrigin.SCRIPT_RESULT_PROPERTY)
                irScript.declarations += resultProperty
                irScript.resultProperty = resultProperty.symbol
            }

            irScript.explicitCallParameters = descriptor.unsubstitutedPrimaryConstructor.valueParameters.map { valueParameterDescriptor ->
                valueParameterDescriptor.toIrValueParameter(startOffset, endOffset, IrDeclarationOrigin.SCRIPT_CALL_PARAMETER).also {
//                    irScript.declarations += it
                }
            }

            irScript.implicitReceivers = descriptor.implicitReceivers.map { implicitReceiver ->
                implicitReceiver.thisAsReceiverParameter.toIrValueParameter(startOffset, endOffset, IrDeclarationOrigin.SCRIPT_IMPLICIT_RECEIVER)
            }

            irScript.providedProperties = descriptor.scriptProvidedProperties.map { providedProperty ->
                val irProperty =  providedProperty.toIrProperty(startOffset, endOffset, IrDeclarationOrigin.SCRIPT_PROVIDED_PROPERTY)
                irScript.declarations += irProperty
                irProperty.symbol
            }
        }
    }

    private fun PropertyDescriptor.toIrProperty(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin): IrProperty =
        context.symbolTable.declareProperty(
            startOffset, endOffset, origin,
            this,
            isDelegated = false
        )

    private fun ParameterDescriptor.toIrValueParameter(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin) =
        context.symbolTable.declareValueParameter(
            startOffset, endOffset, origin,
            this,
            type.toIrType(),
            varargElementType?.toIrType()
        )
}
