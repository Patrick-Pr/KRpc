package dsl

import kotlin.reflect.KType
import kotlin.reflect.typeOf

sealed interface QueryParam {
    data class StringParam(val key: String) : QueryParam
    data class IntParam(val key: Int) : QueryParam
    data class LongParam(val key: Long) : QueryParam
    data class FloatParam(val key: Float) : QueryParam
    data class DoubleParam(val key: Double) : QueryParam
    data class BoolParam(val key: Boolean) : QueryParam
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
    val endpoints = mutableListOf<TypedEndpoint<*, *>>()
    val children = mutableListOf<Route>()


    override fun toString(): String {
        return "Route(path=$path, endpoints=$endpoints, children=$children)"
    }
}

data class PathSegment(val slug: String, val children: List<PathSegment>)


interface Endpoint {
    var method: Method
    val pathParam: PathParam?
    var queryParams: List<QueryParam>

    val requestType: KType
    val responseType: KType
}

data class TypedEndpoint<In : Any, Out : Any> constructor(
    override var method: Method,
    override val pathParam: PathParam?,
    override var queryParams: List<QueryParam>,
    override val requestType: KType,
    override val responseType: KType,
    val handler: suspend (In) -> Out,
) : Endpoint {}

inline fun <reified Out : Any> Route.get(queryParams: List<QueryParam>, noinline handler: suspend (Unit) -> Out): Unit {
    val endpoint = TypedEndpoint(Method.GET, null, queryParams, typeOf<Unit>(), typeOf<Out>(), handler)
    endpoints.add(endpoint)
}

inline fun <reified In : Any, reified Out : Any> Route.post(
    noinline handler: suspend (In) -> Out
): Unit {
    val endpoint = TypedEndpoint(Method.POST, null, arrayListOf(), typeOf<In>(), typeOf<Out>(), handler)
    endpoints.add(endpoint)
}

//inline fun <Out : Any> Route.put(noinline handler: suspend (Unit) -> Out): Unit {
//    val endpoint = TypedEndpoint(Method.PUT, null, arrayListOf(), handler)
//    endpoints.add(endpoint)
//}
//
//inline fun <Out : Any> Route.delete(noinline handler: suspend (Unit) -> Out): Unit {
//    val endpoint = TypedEndpoint(Method.DELETE, null, arrayListOf(), handler)
//    endpoints.add(endpoint)
//}

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