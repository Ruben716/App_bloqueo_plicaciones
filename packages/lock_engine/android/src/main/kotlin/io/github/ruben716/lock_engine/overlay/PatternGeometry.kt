package io.github.ruben716.lock_engine.overlay

/**
 * Reglas del patrón 3x3 (idénticas a las del widget Flutter `PatternLock`):
 * nodos 0..8 por filas; si se une a -> b saltando un nodo intermedio no usado
 * (p. ej. 0 -> 2), ese nodo intermedio se añade automáticamente.
 */
object PatternGeometry {

    fun middleNode(from: Int, to: Int): Int? {
        val r1 = from / 3
        val c1 = from % 3
        val r2 = to / 3
        val c2 = to % 3
        if ((r1 + r2) % 2 != 0 || (c1 + c2) % 2 != 0) return null
        val middle = ((r1 + r2) / 2) * 3 + (c1 + c2) / 2
        return if (middle == from || middle == to) null else middle
    }

    /** Añade [node] a [path] respetando la regla del nodo intermedio. */
    fun append(path: MutableList<Int>, node: Int) {
        if (node in path) return
        val last = path.lastOrNull()
        if (last != null) {
            middleNode(last, node)?.let { middle -> if (middle !in path) path += middle }
        }
        path += node
    }

    fun encode(path: List<Int>): String = path.joinToString(separator = "")
}
