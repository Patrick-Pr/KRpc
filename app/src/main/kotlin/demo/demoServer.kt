package demo

import dsl.get
import dsl.krpcRoute
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import server.installInto
import server.router

fun main() {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

val api = router {
    krpcRoute("/") {
        get<String> {
            "Hello World"
        }
    }
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }

    routing {
        api.installInto(this)
    }
}