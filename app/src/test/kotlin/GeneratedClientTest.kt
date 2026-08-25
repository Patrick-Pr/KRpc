package demo

import krpc.generated.KrpcClient
import kotlin.test.Test
import kotlin.test.assertEquals

class GeneratedClientTest {
    @Test
    fun `test syntax example`() {
        val client = KrpcClient("http://localhost")
        assertEquals("http://localhost/health", client.health.get())
        assertEquals("http://localhost/users", client.users.get())
        assertEquals("http://localhost/users/15", client.users.byId("15").get())
    }
}