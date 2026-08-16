package de.pr.loaf.software.server

import io.ktor.http.HttpMethod
import io.ktor.http.HttpMethod.Companion
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

fun main() {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
    val root = routing {
        get(path = "/", t2 = response<String>()) {
//            call.respondText("krpc server running")
            call.respondWith(listOf("krpc server running"))
        }

        route("/users") {
            get {
                call.respondText("users")
            }

            get("{id}") {
                call.respondText("user ${call.parameters["id"]}")
            }
        }
    }


}

fun Route.get(path: String, t1: KType = typeOf<Unit>(), t2: KType = typeOf<Unit>(), body: suspend RoutingContext.() -> Unit ): Route {
    return method(HttpMethod.Get) { handle(body) }
}

suspend inline fun <reified Out: Any>RoutingCall.respondWith(value: Out) {
    val type = typeOf<Out>()
    println(type)
    this.respond(value)
}

inline fun <reified T: Any>request() = typeOf<T>()
inline fun <reified T: Any>response() = typeOf<T>()