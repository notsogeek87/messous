package com.budgetflow.engine

import com.budgetflow.engine.model.Frequency
import com.budgetflow.engine.model.ScheduledFlow
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FrequencyProjectorTest {

    private fun monthly(day: Int, amount: Double = 100.0) = ScheduledFlow(
        id = 1, label = "monthly", amount = amount, frequency = Frequency.MONTHLY, dayOfMonth = day
    )

    @Test
    fun `monthly occurrence falls on the given day`() {
        val flow = monthly(day = 12)
        val occurrences = FrequencyProjector.occurrencesInRange(flow, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))
        assertEquals(listOf(LocalDate.of(2026, 3, 12)), occurrences)
    }

    @Test
    fun `monthly day 31 is clamped to 28 in a non-leap february`() {
        val flow = monthly(day = 31)
        val occurrences = FrequencyProjector.occurrencesInRange(flow, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))
        assertEquals(listOf(LocalDate.of(2026, 2, 28)), occurrences)
    }

    @Test
    fun `monthly day 31 is clamped to 29 in a leap february`() {
        val flow = monthly(day = 31)
        val occurrences = FrequencyProjector.occurrencesInRange(flow, LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29))
        assertEquals(listOf(LocalDate.of(2028, 2, 29)), occurrences)
    }

    @Test
    fun `monthly day 31 is clamped to 30 in a 30-day month`() {
        val flow = monthly(day = 31)
        val occurrences = FrequencyProjector.occurrencesInRange(flow, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))
        assertEquals(listOf(LocalDate.of(2026, 4, 30)), occurrences)
    }

    @Test
    fun `monthly day 31 falls exactly on the 31st in a 31-day month`() {
        val flow = monthly(day = 31)
        val occurrences = FrequencyProjector.occurrencesInRange(flow, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        assertEquals(listOf(LocalDate.of(2026, 1, 31)), occurrences)
    }

    @Test
    fun `monthly flow spanning several months yields one occurrence per month`() {
        val flow = monthly(day = 5)
        val occurrences = FrequencyProjector.occurrencesInRange(flow, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))
        assertEquals(
            listOf(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 5), LocalDate.of(2026, 3, 5)),
            occurrences
        )
    }

    @Test
    fun `weekly occurrence lists every matching weekday in range`() {
        val flow = ScheduledFlow(
            id = 2, label = "weekly", amount = 50.0, frequency = Frequency.WEEKLY, dayOfWeek = DayOfWeek.MONDAY
        )
        // September 2026: Mondays fall on 7, 14, 21, 28.
        val occurrences = FrequencyProjector.occurrencesInRange(flow, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 28)
            ),
            occurrences
        )
    }

    @Test
    fun `yearly occurrence falls once per year on the given month and day`() {
        val flow = ScheduledFlow(
            id = 3, label = "insurance", amount = 300.0, frequency = Frequency.YEARLY, dayOfMonth = 15, monthOfYear = 6
        )
        val occurrences = FrequencyProjector.occurrencesInRange(flow, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31))
        assertEquals(listOf(LocalDate.of(2026, 6, 15), LocalDate.of(2027, 6, 15)), occurrences)
    }

    @Test
    fun `yearly leap day clamps to 28 in a non-leap year`() {
        val flow = ScheduledFlow(
            id = 4, label = "leap", amount = 10.0, frequency = Frequency.YEARLY, dayOfMonth = 29, monthOfYear = 2
        )
        val occurrences = FrequencyProjector.occurrencesInRange(flow, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(listOf(LocalDate.of(2026, 2, 28)), occurrences)
    }

    @Test
    fun `one-time flow occurs only on its exact date`() {
        val date = LocalDate.of(2026, 9, 25)
        val flow = ScheduledFlow(id = 5, label = "one-off", amount = 200.0, frequency = Frequency.ONE_TIME, oneTimeDate = date)
        assertEquals(listOf(date), FrequencyProjector.occurrencesInRange(flow, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
        assertTrue(FrequencyProjector.occurrencesInRange(flow, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)).isEmpty())
    }

    @Test
    fun `inactive flow never occurs`() {
        val flow = monthly(day = 1).copy(isActive = false)
        assertTrue(FrequencyProjector.occurrencesInRange(flow, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)).isEmpty())
    }

    @Test
    fun `start and end date bound the occurrences`() {
        val flow = monthly(day = 10).copy(startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 5, 31))
        val occurrences = FrequencyProjector.occurrencesInRange(flow, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(
            listOf(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 4, 10), LocalDate.of(2026, 5, 10)),
            occurrences
        )
    }

    @Test
    fun `total due in range multiplies occurrence count by amount`() {
        val flow = monthly(day = 5, amount = 17.99)
        val total = FrequencyProjector.totalDueInRange(flow, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))
        assertEquals(53.97, total, 0.0001)
    }

    @Test
    fun `empty or reversed range yields no occurrences`() {
        val flow = monthly(day = 5)
        assertTrue(FrequencyProjector.occurrencesInRange(flow, LocalDate.of(2026, 3, 31), LocalDate.of(2026, 3, 1)).isEmpty())
    }
}
