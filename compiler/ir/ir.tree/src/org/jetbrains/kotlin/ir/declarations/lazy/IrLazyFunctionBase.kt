/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.types.IrType
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty

interface IrLazyFunctionBase : IrLazyDeclarationBase {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: FunctionDescriptor

    val initialSignatureFunction: IrFunction?

    fun createInitialSignatureFunction(): ReadOnlyProperty<Any?, IrFunction?> = lazyVar {
        descriptor.initialSignatureDescriptor?.takeIf { it != descriptor }?.original?.let(stubGenerator::generateFunctionStub)
    }
}

internal fun <This> This.createValueParameters(): ReadWriteProperty<Any?, List<IrValueParameter>>
        where This : IrLazyFunctionBase, This : IrFunction = lazyVar {
    typeTranslator.buildWithScope(this) {
        descriptor.valueParameters.mapTo(arrayListOf()) {
            stubGenerator.generateValueParameterStub(it).apply { parent = this@createValueParameters }
        }
    }
}

internal fun <This> This.createReceiverParameter(parameter: ReceiverParameterDescriptor?): ReadWriteProperty<Any?, IrValueParameter?>
        where This : IrLazyFunctionBase, This : IrFunction = lazyVar {
    typeTranslator.buildWithScope(this) {
        parameter?.generateReceiverParameterStub()?.also { it.parent = this@createReceiverParameter }
    }
}

internal fun <This> This.createReturnType(): ReadWriteProperty<Any?, IrType>
        where This : IrLazyFunctionBase, This : IrFunction = lazyVar {
    typeTranslator.buildWithScope(this) {
        descriptor.returnType!!.toIrType()
    }
}
