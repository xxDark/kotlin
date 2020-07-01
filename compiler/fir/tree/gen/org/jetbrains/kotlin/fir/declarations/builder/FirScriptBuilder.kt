/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.impl.FirScriptImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirScriptBuilder : FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    lateinit var session: FirSession
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    lateinit var origin: FirDeclarationOrigin
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    lateinit var returnTypeRef: FirTypeRef
    var receiverTypeRef: FirTypeRef? = null
    lateinit var symbol: FirScriptSymbol
    val declarations: MutableList<FirDeclaration> = mutableListOf()
    lateinit var name: Name

    override fun build(): FirScript {
        return FirScriptImpl(
            source,
            session,
            resolvePhase,
            origin,
            annotations,
            returnTypeRef,
            receiverTypeRef,
            symbol,
            declarations,
            name,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildScript(init: FirScriptBuilder.() -> Unit): FirScript {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirScriptBuilder().apply(init).build()
}
