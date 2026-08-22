package dev.krpc.plugin

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@Serializable
abstract class TypeRef(
    @SerialName("typeRef")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val type: String
) {}

@Serializable
sealed interface Param {
    @Serializable
    data class StringParam(val key: String) : Param

    @Serializable
    data class IntParam(val key: Int) : Param

    @Serializable
    data class LongParam(val key: Long) : Param

    @Serializable
    data class FloatParam(val key: Float) : Param

    @Serializable
    data class DoubleParam(val key: Double) : Param

    @Serializable
    data class BoolParam(val key: Boolean) : Param

    @Serializable
    data class PathParam(val key: String) : Param, TypeRef("dsl.PathParam") {
    }
}


@Serializable
enum class Method {
    GET, POST, PUT, DELETE
}

@Serializable
data class Contract(val routes: List<KrpcRoute> = emptyList()) {}

@Serializable
class KrpcRoute(val path: String) : TypeRef("dsl.KrpcRoute") {
    val endpoints = mutableListOf<Endpoint>()
    val children = mutableListOf<KrpcRoute>()

    override fun toString(): String {
        return "Route(path=$path, endpoints=$endpoints, children=$children)"
    }
}

@Serializable
sealed interface Endpoint {
    var method: Method
    val pathParam: Param.PathParam?
    var params: List<Param>

    val requestType: String
    val responseType: String
}

@Serializable
data class TypedGetEndpoint(
    override val pathParam: Param.PathParam?,
    override var params: List<Param>,
    override val requestType: String,
    override val responseType: String,
) : Endpoint, TypeRef("dsl.TypedGetEndpoint") {
    override var method: Method = Method.GET

}

@Serializable
data class TypedPostEndpoint(
    override val pathParam: Param.PathParam?,
    override var params: List<Param>,
    override val requestType: String,
    override val responseType: String,
) : Endpoint, TypeRef("dsl.TypedPostEndpoint") {
    override var method: Method = Method.POST
}


class GenerateKotlinClient(
    private val contractOutputDirectory: Path,
    private val messageCollector: MessageCollector,
) : IrGenerationExtension {
    val rootRoutes = mutableListOf<KrpcRoute>()

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

                        messageCollector.report(
                            CompilerMessageSeverity.INFO,
                            "[GenerateKotlinClient] -> routes ${rootRoutes.joinToString("\n")}"
                        )

                        writeContract(contractOutputDirectory, rootRoutes)
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
                            contractOutputDirectory, rootRoutes
                        )
                    }
                    return declaration
                }
            },
            data = null
        )
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


        val routerFunction = routerCall.symbol.owner

        val routerParameter = routerFunction.parameters.single() { parameter ->
            parameter.kind == IrParameterKind.Regular && parameter.name.asString() == "lambda"
        }

        val routerArgument = routerCall.arguments[routerParameter]

        val routerLambdaFunction = routerArgument.asLambdaFunction() ?: error(
            "router requires a directly declared lambda, but found ${routerArgument?.javaClass?.simpleName}"
        )

        val routerBody = routerLambdaFunction.body ?: error("router lambda has no body")

        collectRouteScope(
            body = routerBody,
            parentRoute = null
        )
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun collectRouteScope(body: IrBody, parentRoute: KrpcRoute?): Boolean {
        visitCallsIn(body) { call ->
            val calledFunction = call.symbol.owner.fqNameWhenAvailable
            when (calledFunction) {
                FqName("dsl.krpcRoute") -> {
                    val pathSegment = (
                            call.regularArgument("pathSegment") as? IrConst
                            )?.value as? String
                        ?: error("krpcRoute path must be a constant String")
                    messageCollector.report(
                        CompilerMessageSeverity.INFO,
                        "[GenerateKotlinClient.collecRoouteScope] -> visit Route $pathSegment"
                    )

                    val route = KrpcRoute(pathSegment)
                    if (parentRoute == null) {
                        rootRoutes += route
                    }

                    parentRoute?.children += route

                    val lambda = call.regularArgument("lambda")
                        .asLambdaFunction()
                        ?: error("krpcRoute requires a directly declared lambda")

                    val lambdaBody = lambda.body
                        ?: error("krpcRoute lambda has no body")

                    messageCollector.report(
                        CompilerMessageSeverity.INFO,
                        "[GenerateKotlinClient.collectRouteScope] called fun dsl.KrpcRoute -> ${call.symbol.owner.fqNameWhenAvailable}"
                    )

                    collectRouteScope(
                        body = lambdaBody,
                        parentRoute = route
                    )


                    false
                }

                FqName("dsl.get") -> {

                    val responseType = call.typeArgumentNamed("Out")
                        ?: error("Could not resolve GET response type")

                    val endpoint = TypedGetEndpoint(
                        pathParam = Param.PathParam(""),
                        params = emptyList(),
                        requestType = "",
                        responseType = responseType.render()
                    )

                    parentRoute?.endpoints += endpoint

                    messageCollector.report(
                        CompilerMessageSeverity.INFO,
                        "[GenerateKotlinClient.collectRouteScope] -> GET ${parentRoute?.path} -> ${responseType.render()}"
                    )

                    // Don't inspect arbitrary calls inside the handler.
                    false
                }

                FqName("dsl.post") -> {
                    val typedArgument = call.typeArgumentNamed("In") ?: error(
                        "Could not resolve POST response type"
                    )
                    val responseType = call.typeArgumentNamed("Out")
                        ?: error("Could not resolve GET response type")

//                    val endpoint = TypedPostEndpoint(
//                        pathParam = PathParam(""),
//                        queryParams = emptyList(),
//                        requestType = typedArgument.render(),
//                        responseType = responseType.render()
//                    )
//
//                    parentRoute?.endpoints += endpoint

                    messageCollector.report(
                        CompilerMessageSeverity.INFO,
                        "[GenerateKotlinClient.collectRouteScope] Post | In type -> ${typedArgument.render()}"
                    )
                    false
                }

                else -> true
            }
        }
        return false
    }

    fun visitCallsIn(root: IrElement, onCall: (IrCall) -> Boolean) {
        root.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                val shouldVisitChildren = onCall(expression)
                if (shouldVisitChildren) {
                    expression.acceptChildrenVoid(this)
                }
            }
        })
    }
}


fun writeContract(outputDir: Path, routes: List<KrpcRoute>) {
    Files.createDirectories(outputDir)

    Files.writeString(
        outputDir.resolve("contract_output.json"),
        Json.encodeToString(routes),
        Charsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
    )

}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrCall.regularArgument(name: String): IrExpression? {
    val parameter = symbol.owner.parameters.singleOrNull {
        it.kind == IrParameterKind.Regular &&
                it.name.asString() == name
    } ?: return null

    return arguments[parameter]
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrCall.typeArgumentNamed(name: String): IrType? {
    val index = symbol.owner.typeParameters.indexOfFirst {
        it.name.asString() == name
    }

    return if (index >= 0) typeArguments.getOrNull(index) else null
}

private fun IrExpression?.asLambdaFunction(): IrSimpleFunction? =
    when (this) {
        is IrFunctionExpression -> function
        is IrRichFunctionReference -> invokeFunction
        else -> null
    }