package com.budgetflow.engine.model

import java.time.LocalDate

/**
 * One day of the "future" timeline: the running bank balance once every
 * income/expense occurrence up to and including [date] has been applied,
 * plus whichever occurrences actually land on [date] (empty on [MonthPlan.today]
 * itself, since that balance already reflects the present).
 */
data class DailyProjection(
    val date: LocalDate,
    val balance: Double,
    val occurrences: List<CalendarOccurrence>
)
