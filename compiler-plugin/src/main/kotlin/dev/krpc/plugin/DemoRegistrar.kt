package dev.krpc.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.nio.file.Path

@OptIn(ExperimentalCompilerApi::class)
class DemoRegistrar : CompilerPluginRegistrar() {
    override val pluginId = "dev.krpc.ir-demo"
    override val supportsK2 = true

    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        val messages = configuration[CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE]
        val contractOutputDirectory = configuration[KrpcConfigurationKeys.contractOutputDirectory]?.let(Path::of)

        messages.report(
            CompilerMessageSeverity.INFO,
            "1!!!! the output directory is $contractOutputDirectory"
        )

        if (contractOutputDirectory != null) {
            IrGenerationExtension.registerExtension(GenerateKotlinClient(contractOutputDirectory, messages))
        }

        IrGenerationExtension.registerExtension(ReplaceBodyExtension(messages))

    }
}
