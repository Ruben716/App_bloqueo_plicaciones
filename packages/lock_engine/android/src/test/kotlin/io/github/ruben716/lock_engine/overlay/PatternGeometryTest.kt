package io.github.ruben716.lock_engine.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PatternGeometryTest {

    private fun draw(vararg nodes: Int): String {
        val path = mutableListOf<Int>()
        nodes.forEach { PatternGeometry.append(path, it) }
        return PatternGeometry.encode(path)
    }

    @Test
    fun `nodo intermedio se agrega automaticamente`() {
        assertEquals("012", draw(0, 2))
        assertEquals("048", draw(0, 8))
        assertEquals("036", draw(0, 6))
        assertEquals("246", draw(2, 6))
    }

    @Test
    fun `nodos no alineados no agregan intermedio`() {
        assertNull(PatternGeometry.middleNode(0, 5))
        assertEquals("05", draw(0, 5))
    }

    @Test
    fun `intermedio ya usado no se repite`() {
        assertEquals("1402", draw(1, 4, 0, 2))
        assertEquals("410", draw(4, 1, 0, 1))
    }
}
