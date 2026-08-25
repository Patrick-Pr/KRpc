package de.pr.loaf.software.server

import api.Krpc
import api.get
import api.krpcRoute
import api.post
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
        get<Int> {
            15
        }
        krpcRoute("health") {
            get<String> {
                "Healthy"
            }
        }
        krpcRoute("/users") {
            get<Out> {
                println("sldkjfsldjflksjd")
                Out("Hello", "slkdjflsd")
            }
            post<String, Out> { input ->
                println("POST skljdf;lsj")
                Out(input, "POST")
            }
            krpcRoute("{id}") {
                get<String> {
                    "skdjfsldj"
                }
                krpcRoute("/users") {
                    get<String> {
                        "lskdjflsdj"
                    }
                }
            }
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