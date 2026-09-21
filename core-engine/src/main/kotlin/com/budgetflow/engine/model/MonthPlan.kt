package com.budgetflow.engine.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * Everything [com.budgetflow.engine.BudgetEngine] needs to answer
 * "how much can I still spend this month?" for a single calendar month.
 *
 * @param month the calendar month being evaluated.
 * @param today the reference "now" date used to split past/future within [month].
 *   For a future month (forecast), pass the first day of that month so the whole
 *   month is treated as not-yet-happened.
 * @param incomes every recurring/one-off income, active or not.
 * @param recurringExpenses every recurring/one-off fixed expense, active or not.
 * @param variableBudgets the discretionary spending envelopes for the month.
 * @param plannedMonthlySavings amount the user intends to set aside this month.
 * @param currentAccountBalances the current real balance of every account (only
 *   meaningful when [today] actually falls inside [month]; otherwise pass an empty list).
 * @param safetyThreshold the minimum amount the user always wants to keep untouched
 *   (spec section 5, "mon seuil de sécurité"). Defaults to 0, i.e. no cushion requested.
 */
data class MonthPlan(
    val month: YearMonth,
    val today: LocalDate,
    val incomes: List<ScheduledFlow>,
    val recurringExpenses: List<ScheduledFlow>,
    val variableBudgets: List<VariableBudgetInput>,
    val plannedMonthlySavings: Double,
    val currentAccountBalances: List<Double> = emptyList(),
    val safetyThreshold: Double = 0.0
) {
    val monthStart: LocalDate get() = month.atDay(1)
    val monthEnd: LocalDate get() = month.atEndOfMonth()
}
