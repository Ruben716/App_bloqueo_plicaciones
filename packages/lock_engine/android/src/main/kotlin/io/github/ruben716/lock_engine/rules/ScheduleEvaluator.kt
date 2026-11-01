package io.github.ruben716.lock_engine.rules

import io.github.ruben716.lock_engine.model.TimeWindow

/** Evalúa si una ventana horaria está activa. Lógica pura (sin Android). */
object ScheduleEvaluator {

    const val MINUTES_PER_DAY = 24 * 60

    /**
     * @param day día ISO-8601 (1 = lunes ... 7 = domingo).
     * @param minute minuto del día (0..1439).
     * @return `true` si [window] es `null` (sin horario = siempre activo).
     */
    fun isActive(window: TimeWindow?, day: Int, minute: Int): Boolean {
        if (window == null) return true
        if (window.days.isEmpty()) return false
        val start = window.startMinute
        val end = window.endMinute
        return when {
            start == end -> day in window.days
            start < end -> day in window.days && minute >= start && minute < end
            // Cruza la medianoche: p. ej. 22:00 -> 07:00. La parte posterior a
            // medianoche pertenece al día en que empezó la ventana.
            else -> (day in window.days && minute >= start) ||
                (previousDay(day) in window.days && minute < end)
        }
    }

    fun previousDay(day: Int): Int = if (day <= 1) 7 else day - 1
}
