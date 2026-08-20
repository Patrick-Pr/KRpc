package dev.krpc.plugin

import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

object KrpcConfigurationKeys {
    val contractOutputDirectory = CompilerConfigurationKey<String>("Krpc Output Directory")
}

@OptIn(ExperimentalCompilerApi::class)
class KrpcCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "dev.krpc.plugin"
    override val pluginOptions = listOf(
        CliOption(
            optionName = "contractOutputDir",
            valueDescription = "generated",
            description = "Directory for generated Krpc sources",
            required = false,
            allowMultipleOccurrences = false
        )
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            "contractOutputDir" -> configuration.put(KrpcConfigurationKeys.contractOutputDirectory, value)
            else -> throw CliOptionProcessingException("Unknown Krpc compiler-plugin option: ${option.optionName}")
        }
    }
}