package com.budgetflow.engine.model

/**
 * The result of evaluating a [MonthPlan]. Every field the dashboard needs is
 * pre-computed here so the UI layer never re-derives money math itself.
 *
 * [availableBudget] and [reallyAvailableNow] are deliberately two different
 * numbers and must never be presented as interchangeable:
 * - [availableBudget] / [remainingToSpend] are a *plan-based* projection for
 *   the whole month (income minus every commitment), independent of the
 *   actual bank balance.
 * - [reallyAvailableNow] is a *balance-based* snapshot: what is truly free
 *   in the accounts right now once every still-upcoming fixed commitment
 *   this month is set aside.
 */
data class MonthSummary(
    val totalIncome: Double,
    val totalFixedExpenses: Double,
    val totalVariableBudgetAllocated: Double,
    val totalVariableSpent: Double,
    val plannedSavings: Double,
    /** Revenus - dépenses fixes - budgets variables (alloués) - épargne prévue, pour tout le mois. */
    val availableBudget: Double,
    /** [availableBudget] moins ce qui a déjà été dépensé dans les enveloppes variables : combien il reste réellement à dépenser maintenant. */
    val remainingToSpend: Double,
    val remainingDaysInMonth: Int,
    /** [remainingToSpend] / [remainingDaysInMonth], 0 if there are no days left. */
    val dailyRecommendedBudget: Double,
    /** Sum of every account's current balance. Null when the month is a future forecast, not "now". */
    val currentBankBalance: Double?,
    /** Sum of future income occurrences (after [MonthPlan.today]) still expected this month. */
    val upcomingIncome: Double,
    /** Sum of future fixed expense occurrences (after [MonthPlan.today]) still due this month. */
    val upcomingFixedExpenses: Double,
    /** currentBankBalance + upcomingIncome - upcomingFixedExpenses. Null when the month is a future forecast. */
    val reallyAvailableNow: Double?
)
