package demo

import dsl.Krpc
import dsl.get
import dsl.krpcRoute
import dsl.post
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import server.installInto
import server.router

fun main() {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

@Serializable
data class Out(val value: String, val type: String)

@Krpc
val api = router {
    krpcRoute("/") {
        get<Out> {
            println("sldkjfsldjflksjd")
            Out("Hello", "slkdjflsd")
        }
        post<String, Out> { input ->
            println("POST skljdf;lsj")
            Out(input, "POST")
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