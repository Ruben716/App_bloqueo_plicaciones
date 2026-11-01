package io.github.ruben716.lock_engine.rules

import io.github.ruben716.lock_engine.model.TimeWindow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleEvaluatorTest {

    private val weekdays = setOf(1, 2, 3, 4, 5)

    @Test
    fun `sin horario siempre esta activo`() {
        assertTrue(ScheduleEvaluator.isActive(null, day = 7, minute = 0))
    }

    @Test
    fun `ventana diurna respeta dias y horas`() {
        val work = TimeWindow(weekdays, startMinute = 9 * 60, endMinute = 18 * 60)
        assertTrue(ScheduleEvaluator.isActive(work, day = 1, minute = 9 * 60))
        assertTrue(ScheduleEvaluator.isActive(work, day = 5, minute = 17 * 60 + 59))
        assertFalse(ScheduleEvaluator.isActive(work, day = 5, minute = 18 * 60))
        assertFalse(ScheduleEvaluator.isActive(work, day = 6, minute = 12 * 60))
        assertFalse(ScheduleEvaluator.isActive(work, day = 3, minute = 8 * 60 + 59))
    }

    @Test
    fun `ventana nocturna cruza la medianoche`() {
        // Viernes 22:00 -> sábado 07:00
        val night = TimeWindow(setOf(5), startMinute = 22 * 60, endMinute = 7 * 60)
        assertTrue(ScheduleEvaluator.isActive(night, day = 5, minute = 23 * 60))
        assertTrue(ScheduleEvaluator.isActive(night, day = 6, minute = 6 * 60))
        assertFalse(ScheduleEvaluator.isActive(night, day = 6, minute = 7 * 60))
        assertFalse(ScheduleEvaluator.isActive(night, day = 5, minute = 6 * 60))
    }

    @Test
    fun `domingo a lunes cruza la semana`() {
        val sundayNight = TimeWindow(setOf(7), startMinute = 23 * 60, endMinute = 60)
        assertTrue(ScheduleEvaluator.isActive(sundayNight, day = 1, minute = 30))
    }

    @Test
    fun `inicio igual a fin significa todo el dia`() {
        val allDay = TimeWindow(setOf(6, 7), startMinute = 0, endMinute = 0)
        assertTrue(ScheduleEvaluator.isActive(allDay, day = 6, minute = 15 * 60))
        assertFalse(ScheduleEvaluator.isActive(allDay, day = 2, minute = 15 * 60))
    }

    @Test
    fun `sin dias nunca esta activo`() {
        assertFalse(ScheduleEvaluator.isActive(TimeWindow(emptySet(), 0, 60), day = 1, minute = 30))
    }
}
