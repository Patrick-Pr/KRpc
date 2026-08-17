package dsl

sealed interface QueryParam {
    data class StringParam(val value: String) : QueryParam
    data class IntParam(val value: Int) : QueryParam
    data class LongParam(val value: Long) : QueryParam
    data class FloatParam(val value: Float) : QueryParam
    data class DoubleParam(val value: Double) : QueryParam
    data class BoolParam(val value: Boolean) : QueryParam
}

data class PathParam(val name: String) : QueryParam

enum class Method {
    GET, POST, PUT, DELETE
}

@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class KRpcDSL

class Router internal constructor() {
    val routes = mutableListOf<Route>()

}

@KRpcDSL
class Route internal constructor(val path: String) {
    internal val endpoints = mutableListOf<Endpoint<*, *>>()
    val children = mutableListOf<Route>()


    override fun toString(): String {
        return "Route(path=$path, endpoints=$endpoints, children=$children)"
    }
}

data class PathSegment(val slug: String, val children: List<PathSegment>)

data class Endpoint<In : Any, Out : Any> constructor(
    internal var method: Method,
    internal val pathParam: PathParam? = null,
    internal var queryParam: QueryParam? = null,
    internal val handler: (In) -> Out
) {}

fun <Out : Any> Route.get(handler: (Unit) -> Out): Unit {
    val endpoint = Endpoint(Method.GET, null, null, handler)
    endpoints.add(endpoint)
}

fun Route.route(pathSegment: String, lambda: Route.() -> Unit) {
    val route = Route(pathSegment).apply(lambda)
    children.add(route)
}

fun Router.route(pathSegment: String, lambda: Route.() -> Unit): Unit {
    val route = Route(pathSegment).apply(lambda)
    routes.add(route)
}

fun router(lambda: Router.() -> Unit): Router {
    val router = Router().apply(lambda)
    return router
}