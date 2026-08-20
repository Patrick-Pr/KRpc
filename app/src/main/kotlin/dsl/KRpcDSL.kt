package dsl

import kotlinx.serialization.Serializable
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Target(AnnotationTarget.PROPERTY)
annotation class Krpc

sealed interface QueryParam {
    data class StringParam(val key: String) : QueryParam
    data class IntParam(val key: Int) : QueryParam
    data class LongParam(val key: Long) : QueryParam
    data class FloatParam(val key: Float) : QueryParam
    data class DoubleParam(val key: Double) : QueryParam
    data class BoolParam(val key: Boolean) : QueryParam
}

@Serializable
data class PathParam(val name: String) : QueryParam

enum class Method {
    GET, POST, PUT, DELETE
}

@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class KRpcDSL

@Serializable
class Router internal constructor() {
    val krpcRoutes = mutableListOf<KrpcRoute>()

}

@KRpcDSL
@Serializable
class KrpcRoute internal constructor(val path: String) {
    val endpoints = mutableListOf<Endpoint>()
    val children = mutableListOf<KrpcRoute>()


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

@Serializable
data class TypedGetEndpoint<Out : Any> constructor(
    override var method: Method,
    override val pathParam: PathParam?,
    override var queryParams: List<QueryParam>,
    override val requestType: KType,
    override val responseType: KType,
    val handler: suspend () -> Out,
) : Endpoint {}

data class TypedPostEndpoint<In : Any, Out : Any> constructor(
    override var method: Method,
    override val pathParam: PathParam?,
    override var queryParams: List<QueryParam>,
    override val requestType: KType,
    override val responseType: KType,
    val handler: suspend (In) -> Out,
) : Endpoint {}

inline fun <reified Out : Any> KrpcRoute.get(
    queryParams: List<QueryParam> = emptyList(),
    noinline handler: suspend () -> Out
): Unit {
    endpoints += TypedGetEndpoint(Method.GET, null, queryParams, typeOf<Unit>(), typeOf<Out>(), handler)
}

inline fun <reified In : Any, reified Out : Any> KrpcRoute.post(
    noinline handler: suspend (In) -> Out
): Unit {
    endpoints += TypedPostEndpoint(Method.POST, null, arrayListOf(), typeOf<In>(), typeOf<Out>(), handler)
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

fun KrpcRoute.krpcRoute(pathSegment: String, lambda: KrpcRoute.() -> Unit) {
    val krpcRoute = KrpcRoute(pathSegment).apply(lambda)
    children.add(krpcRoute)
}

fun Router.krpcRoute(pathSegment: String, lambda: KrpcRoute.() -> Unit): Unit {
    val krpcRoute = KrpcRoute(pathSegment).apply(lambda)
    krpcRoutes.add(krpcRoute)
}