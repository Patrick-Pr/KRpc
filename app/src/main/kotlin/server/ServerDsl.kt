package server

import dsl.KrpcRoute
import dsl.Router
import dsl.TypedGetEndpoint
import dsl.TypedPostEndpoint
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.util.reflect.*
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure
import io.ktor.server.routing.get as ktorGet
import io.ktor.server.routing.post as ktorPost

private fun KType.toKtorTypeInfo(): TypeInfo = TypeInfo(jvmErasure, this)


suspend fun <Out : Any> TypedGetEndpoint<Out>.execute(call: ApplicationCall) {
    val response = handler()
    call.respond(response, responseType.toKtorTypeInfo())
}

suspend fun <In : Any, Out : Any> TypedPostEndpoint<In, Out>.execute(call: ApplicationCall) {
    val body = call.receive<In>(requestType.toKtorTypeInfo())
    val response = handler(body)
    call.respond(response, responseType.toKtorTypeInfo())
}

fun Router.installInto(parent: Route) {
    krpcRoutes.forEach { route ->
        route.installInto(parent)
    }
}

fun KrpcRoute.installInto(parent: Route) {
    parent.route(path) {
        endpoints.forEach { endpoint ->
            when (endpoint) {
                is TypedGetEndpoint<*> -> ktorGet { endpoint.execute(call) }
                is TypedPostEndpoint<*, *> -> ktorPost { endpoint.execute(call) }
                else -> parent.route(path) {}
            }
        }
    }

    children.forEach { child ->
        child.installInto(parent)
    }
}

fun router(lambda: Router.() -> Unit): Router {
    val router = Router().apply(lambda)
    return router
}