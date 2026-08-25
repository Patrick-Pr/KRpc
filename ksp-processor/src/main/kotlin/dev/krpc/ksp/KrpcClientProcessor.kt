package dev.krpc.ksp

import Contract
import KrpcRoute
import TypedGetEndpoint
import TypedPostEndpoint
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists

class KrpcClientProcessor(val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    var generated = false
    override fun process(resolver: Resolver): List<KSAnnotated> {
        environment.logger.info("[krpc] Starting KRPC client processing")
        if (generated) return emptyList()

        val contractDirectory = environment.options["krpc.contractDirectory"]
            ?: error("Missing KSP option: krpc.contractDirectory")

        val manifestPathStr = environment.options["krpc.manifest"] ?: error("Missing KSP option: krpc.manifest")

        val manifest = Path.of(manifestPathStr)

        if (!manifest.exists()) {
            environment.logger.warn("Krpc compiler output Manifest not found")
            return emptyList()
        }

        environment.logger.info("[krpc] reading Manifest file")
        val contract = readContract(manifest)

        environment.logger.info("[krpc] writing client")
        environment.codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true),
            packageName = "krpc.generated",
            fileName = "KrpcClient",
        ).bufferedWriter().use { output ->
            output.append(renderClient(contract))
        }

        generated = true
        return emptyList()
    }

}

private fun readContract(path: Path): Contract =
    Json.decodeFromString(Files.readString(path))

private fun renderClient(contract: Contract): String = buildString {
    appendLine("package krpc.generated")
    appendLine()
    append(renderRouteClasses(route))
    appendLine("class KrpcClient(private val baseUrl: String) {")
    contract.routes.forEach { route ->
//        appendLine($$"val $${if (route.path == "/") "root" else route.path} = $${(route.toClassname())}(\"$baseUrl/$$route.path\") {")
        route.endpoints.forEach { endpoint ->
            when (endpoint) {
                is TypedGetEndpoint -> {
                    appendLine("\t fun get() = \"get\"")
                }

                is TypedPostEndpoint -> {
                    appendLine("\t fun post() = \"post\"")
                }
            }
        }
    }
    appendLine("}")
}

fun renderRouteClasses(routes: List<KrpcRoute>): String {
    val childRouteClasses = routes.joinToString("\n\n") { route -> renderRouteClasses(route.children) }
    return buildString {
        routes.forEach { route ->
            val className = route.toClassname()
            appendLine("class $className(val path: String) {")
            route.endpoints.forEach { endpoint ->
                when (endpoint) {
                    is TypedGetEndpoint -> {
                        appendLine($$"\t fun get() = \"$path/get\"")
                    }

                    is TypedPostEndpoint -> {
                        appendLine($$"\t fun post() = \"$path/post\"")
                    }
                }
            }
        }
        append(childRouteClasses)
    }
}

fun renderNested(route: KrpcRoute): String {
    val endpoints = buildString {
        route.endpoints.forEach { endpoint ->
            when (endpoint) {
                is TypedGetEndpoint -> {
                    appendLine("\t fun get() = \"get\"")
                }

                is TypedPostEndpoint -> {
                    appendLine("\t fun post() = \"post\"")
                }
            }
        }
    }
}

fun KrpcRoute.toClassname(): String {
    val name = if (path == "/") "root" else path
    return name.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.titlecase(
            Locale.getDefault()
        )
    }
}


class KrpcClient(val baseUrl: String) {
    val health = HealthRoute("$baseUrl/health")

    val users = UsersRoute("$baseUrl/users")
}

class HealthRoute(val path: String) {
    fun get() = path
}

class UsersRoute(val path: String) {
    fun get() = path

    val byId = fun(idVal: String) = UsersByIdRoute(idValue = idVal, path = "$path/$idVal")
}

class UsersByIdRoute(val idValue: String, val path: String) {
    fun get() = path
}