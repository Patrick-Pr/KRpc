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
    append(renderRouteClasses(contract.route.children, contract.route))
    appendLine("class KrpcClient(private val baseUrl: String) {")
//        appendLine($$"val $${if (route.path == "/") "root" else route.path} = $${(route.toClassname())}(\"$baseUrl/$$route.path\") {")
    contract.route.endpoints.forEach { endpoint ->
        when (endpoint) {
            is TypedGetEndpoint -> {
                appendLine("\t fun get() = \"get\"")
            }

            is TypedPostEndpoint -> {
                appendLine("\t fun post() = \"post\"")
            }
        }
    }
    contract.route.children.forEach { child ->
        appendLine($$"\t val $${pathToClassname(child.path).firstCharToLowerCase()} = $${pathToClassname(child.path)}Route(\"$baseUrl/$${child.path}\")")
    }
    appendLine("}")
}

fun renderRouteClasses(routes: List<KrpcRoute>, parentRoute: KrpcRoute): String {
    val childRouteClasses = routes.joinToString("\n\n") { route -> renderRouteClasses(route.children, route) }
    return buildString {
        routes.forEach { route ->
            val className = pathToClassname("${parentRoute.path}/${route.path}").let { str -> "${str}Route" }
            if (route.path.trim().startsWith("{") && route.path.trim().endsWith("}")) {
                appendLine("class $className(val path: String, val pathValue: String) {")
            } else {
                appendLine("class $className(val path: String) {")
            }
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
            route.children.forEach { child ->
                val childClassName = pathToClassname("${route.path}/${child.path}").let { str -> "${str}Route" }
                if (child.path.trim().startsWith("{") && child.path.trim().endsWith("}")) {
                    val dynamicParam = child.path.substringAfter("{").substringBefore("}").firstCharToupperCase()
                    appendLine($$"\tval by$${dynamicParam} = fun(value: String) = UsersByIdRoute(pathValue = value, path = \"$path/$value\")")
                } else {
                    appendLine($$"\t val $${pathToClassname(child.path).firstCharToLowerCase()} = $${childClassName}(\"$path/$${child.path}\")")
                }
            }
            appendLine("}")
        }
        append(childRouteClasses)
    }
}

//fun renderNested(routes: List<KrpcRoute>, parentRoute: KrpcRoute): String {
//    val nestedProperties = routes.joinToString("\n\n") { route -> renderNested(route.children, parentRoute) }
//    return buildString {
//        routes.forEach { route ->
//            route.endpoints.forEach { endpoint ->
//                when (endpoint) {
//                    is TypedGetEndpoint -> {
//                        appendLine("\t fun get() = \"get\"")
//                    }
//
//                    is TypedPostEndpoint -> {
//                        appendLine("\t fun post() = \"post\"")
//                    }
//                }
//            }
//            route.children.forEach { child ->
//                appendLine($$"\t val $${pathToClassname(child.path).firstCharToLowerCase()}() = $${pathToClassname(child.path)}(\"$path/$${child.path}\")")
//            }
//        }
//        append(nestedProperties)
//    }
//
//}

fun KrpcRoute.toClassname(): String {
    val name = if (path == "/") "root" else path
    return name.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.titlecase(
            Locale.getDefault()
        )
    }
}

fun pathToClassname(path: String): String = path
    .removePrefix("/")
    .removeSuffix("/")
    .replace("/", " ")
    .split(" ")
    .joinToString("") { str ->
        if (str.trim().startsWith("") && str.trim().endsWith("}"))
            "By${str.removePrefix("{").removeSuffix("}").firstCharToupperCase()}"
        else
            str.firstCharToupperCase()
    }

fun String.firstCharToupperCase(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

fun String.firstCharToLowerCase(): String {
    return this.replaceFirstChar {
        if (it.isUpperCase()) it.lowercase(Locale.getDefault()) else it.toString()
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