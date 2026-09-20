package com.budgetflow.engine

import com.budgetflow.engine.model.Frequency
import com.budgetflow.engine.model.ScheduledFlow
import java.time.LocalDate
import java.time.YearMonth

/**
 * Turns a [ScheduledFlow] description into concrete calendar dates.
 *
 * This is the only place that knows how to walk months/years/weeks, which
 * keeps every edge case (short months, leap years, day-of-month clamping,
 * start/end bounds) in one tested spot instead of scattered across the app.
 */
object FrequencyProjector {

    /**
     * All dates on which [flow] falls due within [rangeStart]..[rangeEnd] (inclusive on both ends).
     * Returns an empty list for an inactive flow or an empty/reversed range.
     */
    fun occurrencesInRange(flow: ScheduledFlow, rangeStart: LocalDate, rangeEnd: LocalDate): List<LocalDate> {
        if (!flow.isActive || rangeEnd.isBefore(rangeStart)) return emptyList()

        val candidates: List<LocalDate> = when (flow.frequency) {
            Frequency.ONE_TIME -> listOfNotNull(flow.oneTimeDate)
            Frequency.WEEKLY -> weeklyOccurrences(flow, rangeStart, rangeEnd)
            Frequency.MONTHLY -> monthlyOccurrences(flow, rangeStart, rangeEnd)
            Frequency.YEARLY -> yearlyOccurrences(flow, rangeStart, rangeEnd)
        }

        return candidates
            .filter { !it.isBefore(rangeStart) && !it.isAfter(rangeEnd) }
            .filter { flow.startDate == null || !it.isBefore(flow.startDate) }
            .filter { flow.endDate == null || !it.isAfter(flow.endDate) }
            .sorted()
    }

    /** Total amount due for [flow] within [rangeStart]..[rangeEnd], i.e. occurrences x amount. */
    fun totalDueInRange(flow: ScheduledFlow, rangeStart: LocalDate, rangeEnd: LocalDate): Double =
        occurrencesInRange(flow, rangeStart, rangeEnd).size * flow.amount

    private fun weeklyOccurrences(flow: ScheduledFlow, rangeStart: LocalDate, rangeEnd: LocalDate): List<LocalDate> {
        val targetDow = flow.dayOfWeek ?: return emptyList()
        var cursor = rangeStart.with(java.time.temporal.TemporalAdjusters.nextOrSame(targetDow))
        val result = mutableListOf<LocalDate>()
        while (!cursor.isAfter(rangeEnd)) {
            result += cursor
            cursor = cursor.plusWeeks(1)
        }
        return result
    }

    private fun monthlyOccurrences(flow: ScheduledFlow, rangeStart: LocalDate, rangeEnd: LocalDate): List<LocalDate> {
        val day = flow.dayOfMonth ?: return emptyList()
        val result = mutableListOf<LocalDate>()
        var month = YearMonth.from(rangeStart)
        val lastMonth = YearMonth.from(rangeEnd)
        while (!month.isAfter(lastMonth)) {
            result += clampedDate(month, day)
            month = month.plusMonths(1)
        }
        return result
    }

    private fun yearlyOccurrences(flow: ScheduledFlow, rangeStart: LocalDate, rangeEnd: LocalDate): List<LocalDate> {
        val day = flow.dayOfMonth ?: return emptyList()
        val monthOfYear = flow.monthOfYear ?: return emptyList()
        val result = mutableListOf<LocalDate>()
        var year = rangeStart.year
        while (year <= rangeEnd.year) {
            result += clampedDate(YearMonth.of(year, monthOfYear), day)
            year += 1
        }
        return result
    }

    /** Clamps [day] to the last valid day of [month], so "31" in February safely becomes the 28th/29th. */
    private fun clampedDate(month: YearMonth, day: Int): LocalDate =
        month.atDay(minOf(day, month.lengthOfMonth()))
}
