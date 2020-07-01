/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirScript : FirPureAbstractElement(), FirAnnotatedDeclaration, FirCallableDeclaration<FirScript>, FirControlFlowGraphOwner {
    abstract override val source: FirSourceElement?
    abstract override val session: FirSession
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val returnTypeRef: FirTypeRef
    abstract override val receiverTypeRef: FirTypeRef?
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference
    abstract override val symbol: FirScriptSymbol
    abstract val declarations: List<FirDeclaration>
    abstract val name: Name

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitScript(this, data)

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirScript

    abstract override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirScript

    abstract override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirScript

    abstract override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirScript

    abstract fun <D> transformDeclarations(transformer: FirTransformer<D>, data: D): FirScript
}
