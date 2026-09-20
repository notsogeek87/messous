package com.budgetflow.engine.model

import java.time.YearMonth

/** A projected, plan-based summary for a month that has not happened yet. */
data class MonthlyForecast(
    val month: YearMonth,
    val totalIncome: Double,
    val totalFixedExpenses: Double,
    val totalVariableBudgetAllocated: Double,
    val plannedSavings: Double,
    val availableBudget: Double
)
