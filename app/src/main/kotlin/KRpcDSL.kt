import Endpoint.Delete
import Endpoint.Get
import Endpoint.Post
import Endpoint.Put
import kotlin.reflect.KType

sealed interface QueryParam {
    data class StringParam(val value: String) : QueryParam
    data class IntParam(val value: Int) : QueryParam
    data class LongParam(val value: Long) : QueryParam
    data class FloatParam(val value: Float) : QueryParam
    data class DoubleParam(val value: Double) : QueryParam
    data class BoolParam(val value: Boolean) : QueryParam
}

enum class EndpointType {
    GET, POST, PUT, DELETE
}

@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class KRpcDSL

@KRpcDSL
class Resource {
    val subRoutes: List<Resource> = arrayListOf()
    var get: Endpoint? = null
    var post: Endpoint? = null
    var delete: Endpoint? = null
    var put: Endpoint? = null

    infix fun add(endpoint: Endpoint) {
        when (endpoint) {
            is Get -> get = endpoint
            is Put -> put = endpoint
            is Post -> post = endpoint
            is Delete -> delete = endpoint
        }
    }

    fun get(getEndpoint: Endpoint.() -> Get): Get {
        val get = Get()
        return get.getEndpoint()
    }
}

//class Endpoint {
//    val handler: ((String) -> String)? = null
//    val queryParams: MutableMap<String, String> = mutableMapOf()
//}

sealed interface Endpoint {
    val handler: ((String) -> String)?
    val queryParams: MutableMap<String, String>

    class Get: Endpoint {
        override val handler: ((String) -> String)? = null
        override val queryParams: MutableMap<String, String> = mutableMapOf()
    }
    class Post: Endpoint {
        override val handler: ((String) -> String)? = null
        override val queryParams: MutableMap<String, String> = mutableMapOf()
    }
    class Put: Endpoint {
        override val handler: ((String) -> String)? = null
        override val queryParams: MutableMap<String, String> = mutableMapOf()
    }
    class Delete: Endpoint {
        override val handler: ((String) -> String)? = null
        override val queryParams: MutableMap<String, String> = mutableMapOf()
    }

}

//sealed interface Endpoint {
// @KRpcDSL
// class Get(
//  val handler: (KType) -> KType
// ): Endpoint, Resource() {
//  private val routes = listOf<String>()
//  operator fun String.unaryPlus() {
//   this.get = this
//  }
// }
// @KRpcDSL
// class Post(
//  val handler: (KType) -> KType
// ): Endpoint {
//  private val routes = listOf<String>()
//  operator fun String.unaryPlus() {}
// }
// @KRpcDSL
// class Put(
//  val handler: (KType) -> KType
// ): Endpoint {
//  private val routes = listOf<String>()
//  operator fun String.unaryPlus() {}
// }
// @KRpcDSL
// class Delete(
//  val handler: (KType) -> KType
// ): Endpoint {
//  private val routes = listOf<String>()
//  operator fun String.unaryPlus() {}
// }
//}


fun root(rootResource: Resource.() -> Unit) {

}
