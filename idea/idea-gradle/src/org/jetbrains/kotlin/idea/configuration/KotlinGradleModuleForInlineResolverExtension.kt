/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.Consumer
import org.jetbrains.kotlin.idea.debugger.KotlinDebuggerSettings
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class KotlinGradleModuleForInlineResolverExtension : AbstractProjectResolverExtension() {

    override fun enhanceTaskProcessing(taskNames: MutableList<String>, jvmParametersSetup: String?, initScriptConsumer: Consumer<String>) {
        try {
            val disableInlineDebugger = KotlinDebuggerSettings.getInstance().debugDisableCoroutineAgent
            if (!disableInlineDebugger)
                setupGradleTest(initScriptConsumer)
        } catch (e: Exception) {
            log.error("Gradle: error while getting an inline debugger settings.", e)
        }
    }

    private fun setupGradleTest(initScriptConsumer: Consumer<String>) {
        val script =
            //language=Gradle
            """
                gradle.taskGraph.beforeTask { Task task ->
                    if (task instanceof Test) {
                        task.jvmArgs ("-Xbootclasspath/a:\"${'$'}{task.project.projectDir.path}/${INLINE_USE_IDENTIFIER}\"")
                    }
                }
            """.trimIndent()
        initScriptConsumer.consume(script)
    }

    companion object {
        const val INLINE_USE_IDENTIFIER = "for_inline_use_only"
        val log = Logger.getInstance(this::class.java)

    }
}