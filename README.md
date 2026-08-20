# Krpc

# Disclaimer

I am very early in development and actively experimenting with how I want to solve this problem aswell as learning new
things alongside. Therefore, nothing is
final
and the code in this repository might not have the
highest quality yet.

# Introduction

The aim of this repository is to deepen my knowledge of Kotlin while trying to create something that I
miss dearly from fullstack Typescript.

## What

In fullstack Typescript there is this `fullstack type safety` term which came up some time ago. It describes the
pursuit of closing the air gap between the client and server. Here I am trying to bring this feeling to Kotlin. My goal
is for the API declaration on the server to be the single source of truth for how the API and the DTO's coming in and
out of the server are shaped. To archive this, I want to generate a client from this API description.
Regarding developer experience, I am looking at [trpc](https://trpc.io/)
and [Orpc](https://www.google.com/search?client=firefox-b-d&q=orpc) from the Typescript ecosystem.

in [Goals](#Goals) I will show a bit more how I want things to look and feel.

## Why

I am a big fan of the idea of fullstack type safety. Starting my career with Java
development, and to be honest sometimes later as well, I worked in projects where the client and server were virtually
air gapped.
They both had their own set of DTO's written and drift between these DTO's happened always.

At the time I discovered that something like [trpc](https://trpc.io/) exists I was delighted and wondered why it is not
as big a deal in the JVM ecosystem. Recently I was able to use some of these tools in a work context. This
strengthened my belief that fullstack type safety is something worth considering and adopting at least some parts of it.

## Goals

My goal for this project is to be able to write code roughly like this using a Ktor server

```kotlin
val api = router {
    krpcRoute("/") {
        krpcRoute("action") {
            get<Out> {
                Out("Hello", "Peter")
            }
            post<String, Out> { input ->
                Out("POST", input)
            }
        }
    }
}

fun Application.module() {
    routing {
        api.installInto(this)
    }
}
```

and be able to import, instantiate, and use a client straight away with the same DTOs used in the route handlers.

```kotlin
import krpc.generated.Client

val client = Client("https://my.server.domain.com")

val result = client.action.get()
```
