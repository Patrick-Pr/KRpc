package demo

import kotlin.test.Test
import kotlin.test.assertEquals

class IrDemoTest {
    @Test
    fun `IR plugin replaces only functions marked with the annotation`() {
        assertEquals("produced by the IR plugin", markedIrDemo())
        assertEquals("not marked", irDemo())
    }
}
