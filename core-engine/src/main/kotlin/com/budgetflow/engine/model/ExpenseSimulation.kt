package com.budgetflow.engine.model

/**
 * The result of simulating a hypothetical expense against a [MonthPlan],
 * without ever touching real data (spec section 23: "Et si...?" is always
 * a pure, throwaway computation).
 *
 * [before] and [after] are full [MonthSummary] snapshots so the UI can show
 * the impact on every headline number, not just free money.
 */
data class ExpenseSimulation(
    val amount: Double,
    val before: MonthSummary,
    val after: MonthSummary
) {
    /** How much [MonthSummary.freeMoney] (account-balance-based) would drop. Null if there is no account balance to simulate against. */
    val freeMoneyDelta: Double?
        get() = before.freeMoney?.let { b -> after.freeMoney?.let { a -> b - a } }

    /** The account-balance-based "coût en liberté" (spec section 16): drop in daily spending power. */
    val freedomPerDayDelta: Double?
        get() = before.freedomPerDay?.let { b -> after.freedomPerDay?.let { a -> b - a } }

    /** True when this expense would push the month from a safe state into (or deeper into) an alert state, by account balance. */
    val wouldBreachSafetyThreshold: Boolean
        get() = after.freedomState == FreedomState.ALERT

    /** How far under the safety threshold free money (account-balance-based) would land, if at all. Positive means "under by this much". */
    val amountUnderThreshold: Double?
        get() = after.safetyMargin?.let { if (it < 0) -it else null }

    /**
     * How much [MonthSummary.remainingToSpend] ("reste à vivre" = revenus - dépenses - budgets -
     * épargne, account-independent) would drop. Unlike [freeMoneyDelta], this is never null: it
     * doesn't need an account balance to mean something.
     */
    val remainingToSpendDelta: Double
        get() = before.remainingToSpend - after.remainingToSpend

    /** [remainingToSpendDelta] spread over a day: drop in the plan-based daily budget. */
    val dailyRecommendedBudgetDelta: Double
        get() = before.dailyRecommendedBudget - after.dailyRecommendedBudget

    /** True when this expense would push the plan-based reste à vivre under the safety threshold. */
    val wouldBreachPlanSafetyThreshold: Boolean
        get() = after.remainingToSpend < after.safetyThreshold

    /** How far under the safety threshold the plan-based reste à vivre would land, if at all. Positive means "under by this much". */
    val planAmountUnderThreshold: Double?
        get() = (after.safetyThreshold - after.remainingToSpend).let { if (it > 0) it else null }
}
