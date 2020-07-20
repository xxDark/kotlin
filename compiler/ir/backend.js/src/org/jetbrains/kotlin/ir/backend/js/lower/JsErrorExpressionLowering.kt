/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.ErrorDeclarationLowering
import org.jetbrains.kotlin.backend.common.lower.ErrorExpressionLowering
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrErrorDeclaration
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.name.Name

class JsErrorDeclarationLowering(context: JsIrBackendContext) : ErrorDeclarationLowering() {
    private val nothingType = context.irBuiltIns.nothingType
    private val stringType = context.irBuiltIns.stringType
    private val errorSymbol = context.errorSymbol

    override fun transformErrorDeclaration(declaration: IrErrorDeclaration): IrDeclaration {
        require(errorSymbol != null) { "Should be non-null if errors are allowed" }
        return with(IrFunctionBuilder()) {
            updateFrom(declaration)
            returnType = nothingType
            name = Name.identifier("\$errorDeclaration")
            buildFun().also {
                it.parent = declaration.parent
                it.body = IrBlockBodyImpl(startOffset, endOffset) {
                    statements += IrCallImpl(startOffset, endOffset, nothingType, errorSymbol, 0, 1, null, null).apply {
                        putValueArgument(0, IrConstImpl.string(startOffset, endOffset, stringType, "ERROR DECLARATION"))
                    }
                }
            }
        }
    }
}

class JsErrorExpressionLowering(context: JsIrBackendContext) : ErrorExpressionLowering(context) {

    private val stringType = context.irBuiltIns.nothingType
    private val errorSymbol = context.errorSymbol

    override fun transformErrorExpression(expression: IrExpression, nodeString: String): IrExpression {
        val errorExpression = expression as? IrErrorExpression
        val description = errorExpression?.let { "$nodeString: ${it.description}" } ?: nodeString
        return buildThrowError(expression, description)
    }

    private fun buildThrowError(element: IrElement, description: String): IrExpression {
        require(errorSymbol != null) { "Should be non-null if errors are allowed" }
        return element.run {
            IrCallImpl(startOffset, endOffset, nothingType, errorSymbol, 0, 1, null, null).apply {
                putValueArgument(0, IrConstImpl.string(startOffset, endOffset, stringType, description))
            }
        }
    }
}