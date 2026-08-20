package dev.krpc.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class GenerateKotlinClient(
    private val contractOutputDirectory: Path,
    private val messageCollector: MessageCollector,
) : IrGenerationExtension {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        moduleFragment.transform(
            object : IrElementTransformerVoid() {
                override fun visitProperty(declaration: IrProperty): IrStatement {
                    super.visitProperty(declaration)

                    if (declaration.hasAnnotation(FqName("dsl.Krpc"))) {
                        messageCollector.report(
                            CompilerMessageSeverity.INFO,
                            "[VisitProperty -> ${declaration.name.asString()}] -> has Krpc Annotation"
                        )

                        inspectApiRoot(declaration, messageCollector)
                    }

                    return declaration
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                    super.visitSimpleFunction(declaration)

                    messageCollector.report(
                        CompilerMessageSeverity.INFO,
                        "!!!! the output directory is $contractOutputDirectory"
                    )


                    if (declaration.hasAnnotation(FqName("dsl.Krpc"))) {
                        writeContract(
                            contractOutputDirectory, """
                            {
                                "name": "Peter Lustig",
                                "company: "Loewenzahn"
                            }
                        """.trimMargin()
                        )
                    }
                    return declaration
                }
            },
            data = null
        )
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun inspectApiRoot(property: IrProperty, messageCollector: MessageCollector) {
    val initializer = property.backingField?.initializer?.expression
        ?: error("@Krpc property ${property.name.asString()} has no initializer")

    val routerCall = initializer as? IrCall
        ?: error("@Krpc property ${property.name} must call router { ... }")

    val calledFunction = routerCall.symbol.owner.fqNameWhenAvailable

    require(calledFunction == FqName("server.router")) {
        "@Krpc property ${property.name.asString()} must call server.router"
    }

//    messageCollector.report(
//        CompilerMessageSeverity.INFO,
//        "[VisitProperty.inspectApiRoot.routerCall.arg -> ${arg?.type}]"
//    )


    val routerFunction = routerCall.symbol.owner

    val routerParameter = routerFunction.parameters.single() { parameter ->
        parameter.kind == IrParameterKind.Regular && parameter.name.asString() == "lambda"
    }

    val routerArgument = routerCall.arguments[routerParameter]

    val routerLambdaFunction = when (routerArgument) {
        is IrFunctionExpression -> routerArgument.function
        is IrRichFunctionReference -> routerArgument.invokeFunction
        else -> error(
            "router requires a directly declared lambda, but found ${routerArgument?.javaClass?.simpleName}"
        )
    }

    val routerBody = routerLambdaFunction.body ?: error("router lambda has no body")


    collectRouteScope(
        body = routerBody,
        currentPath = emptyList()
    )
}

fun collectRouteScope(body: IrBody, currentPath: List<Path>) {
    visitCallsIn() { call ->

    }
}

fun writeContract(outputDir: Path, json: String) {
    Files.createDirectories(outputDir)

    Files.writeString(
        outputDir.resolve("contract_output.json"),
        json,
        Charsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
    )

}