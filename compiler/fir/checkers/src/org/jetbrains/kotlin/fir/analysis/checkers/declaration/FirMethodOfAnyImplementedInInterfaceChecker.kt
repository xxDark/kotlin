/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirMethodOfAnyImplementedInInterfaceChecker : FirMemberDeclarationChecker() {
    private var anyMethods: Set<Name>? = null

    private fun getAnyMethods(context: CheckerContext): Set<Name> {
        var methods = anyMethods

        return if (methods != null) {
            methods
        } else {
            val anyClassId = context.session.builtinTypes.anyType.id
            val anyMethods = context.session.firSymbolProvider.getClassLikeSymbolByFqName(anyClassId)
                ?.fir.safeAs<FirRegularClass>()
                ?.declarations
                ?.filterIsInstance<FirSimpleFunction>()
                ?.filter { it !is FirConstructor }
                ?.map { it.name }
            methods = anyMethods?.toSet() ?: emptySet()
            this.anyMethods = methods
            methods
        }
    }

    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirClass<*> || declaration.classKind != ClassKind.INTERFACE) {
            return
        }

        for (it in declaration.declarations) {
            val methods = getAnyMethods(context)

            if (it is FirSimpleFunction && it.name in methods && it.body != null && !it.isOperator && it.isOverride) {
                reporter.report(it.source)
            }
        }
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let { report(FirErrors.ANY_METHOD_IMPLEMENTED_IN_INTERFACE.on(it)) }
    }
}