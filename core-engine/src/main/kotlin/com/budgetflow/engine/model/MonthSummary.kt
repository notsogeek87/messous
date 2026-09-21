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
    /**
     * [remainingToSpend] but counting only the income/fixed expenses actually due by [MonthPlan.today] -
     * a plan-based projection like [remainingToSpend], but one that moves as the month's real
     * income/expense dates pass, instead of assuming the whole month has already happened.
     */
    val remainingToSpendToday: Double,
    /** [remainingToSpendToday] compared against [safetyThreshold], the same way [freedomState] compares [freeMoney]. */
    val remainingToSpendTodayState: FreedomState,
    /** Sum of every account's current balance. Null when the month is a future forecast, not "now". */
    val currentBankBalance: Double?,
    /** Sum of future income occurrences (after [MonthPlan.today]) still expected this month. */
    val upcomingIncome: Double,
    /** Sum of future fixed expense occurrences (after [MonthPlan.today]) still due this month. */
    val upcomingFixedExpenses: Double,
    /** currentBankBalance + upcomingIncome - upcomingFixedExpenses. Null when the month is a future forecast. */
    val reallyAvailableNow: Double?,
    /** Portion of the variable envelopes not yet spent - still spoken for, never "free" (0 if already overspent). */
    val remainingVariableBudget: Double,
    /** Planned savings not yet set aside this month. */
    val remainingPlannedSavings: Double,
    /**
     * "Ton argent libre" (spec section 3): what is truly free to spend without touching a
     * single commitment - [reallyAvailableNow] once the still-unspent variable envelopes and
     * the still-unsaved planned savings are set aside too. Null exactly when [reallyAvailableNow] is.
     */
    val freeMoney: Double?,
    /** The minimum balance the user asked to always keep untouched (spec section 5). */
    val safetyThreshold: Double,
    /** [freeMoney] minus [safetyThreshold]. Negative means the month is already under the cushion. Null when [freeMoney] is. */
    val safetyMargin: Double?,
    /** The month's overall "weather" derived from [safetyMargin] - never a moral judgment, just distance to the threshold. */
    val freedomState: FreedomState,
    /** "Tu peux dépenser environ X€/jour" (spec section 4): [freeMoney] spread over [remainingDaysInMonth]. Null when [freeMoney] is. */
    val freedomPerDay: Double?
)
