package com.budgetflow.engine.model

import java.time.LocalDate

/** A single dated line in the "future expenses/incomes" calendar view. */
data class CalendarOccurrence(
    val date: LocalDate,
    val flowId: Long,
    val label: String,
    val amount: Double,
    val direction: FlowDirection
)
