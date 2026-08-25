import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
abstract class TypeRef(
    @SerialName("typeRef")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val type: String
) {}

@Serializable
sealed interface Param {
    @Serializable
    data class StringParam(val key: String) : Param

    @Serializable
    data class IntParam(val key: Int) : Param

    @Serializable
    data class LongParam(val key: Long) : Param

    @Serializable
    data class FloatParam(val key: Float) : Param

    @Serializable
    data class DoubleParam(val key: Double) : Param

    @Serializable
    data class BoolParam(val key: Boolean) : Param

    @Serializable
    data class PathParam(val key: String) : Param, TypeRef("dsl.PathParam") {
    }
}


@Serializable
enum class Method {
    GET, POST, PUT, DELETE
}

@Serializable
data class Contract(val routes: List<KrpcRoute> = emptyList()) : TypeRef("dev.krpc.plugin.Contract") {}

@Serializable
class KrpcRoute(val path: String) : TypeRef("dsl.KrpcRoute") {
    val endpoints = mutableListOf<Endpoint>()
    val children = mutableListOf<KrpcRoute>()

    override fun toString(): String {
        return "Route(path=$path, endpoints=$endpoints, children=$children)"
    }
}

@Serializable
sealed interface Endpoint {
    var method: Method
    val pathParam: Param.PathParam?
    var params: List<Param>

    val requestType: String
    val responseType: String
}

@Serializable
data class TypedGetEndpoint(
    override val pathParam: Param.PathParam?,
    override var params: List<Param>,
    override val requestType: String,
    override val responseType: String,
) : Endpoint, TypeRef("dsl.TypedGetEndpoint") {
    override var method: Method = Method.GET

}

@Serializable
data class TypedPostEndpoint(
    override val pathParam: Param.PathParam?,
    override var params: List<Param>,
    override val requestType: String,
    override val responseType: String,
) : Endpoint, TypeRef("dsl.TypedPostEndpoint") {
    override var method: Method = Method.POST
}