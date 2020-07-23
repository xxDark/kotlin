/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.types.IrType

//TODO: make IrScript as IrPackageFragment, because script is used as a file, not as a class
//NOTE: declarations and statements stored separately
interface IrScript : IrSymbolDeclaration<IrScriptSymbol>, IrDeclarationContainer, IrDeclarationWithName, IrDeclarationParent, IrSymbolOwner {
    // TODO: represent the script as a body
    val statements: MutableList<IrStatement>

    // NOTE: is the result of the FE conversion, because there script interpreted as a class and has receiver
    // TODO: consider removing from here and handle appropriately in the lowering
    var thisReceiver: IrValueParameter

    var baseClass: IrType

    var explicitCallParameters: List<IrValueParameter>

    var implicitReceivers: List<IrValueParameter>

    var providedProperties: List<IrPropertySymbol>

    var resultProperty: IrPropertySymbol?
}
