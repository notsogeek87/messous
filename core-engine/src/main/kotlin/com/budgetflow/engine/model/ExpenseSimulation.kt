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
    /** How much [MonthSummary.freeMoney] would drop. Null if there is no account balance to simulate against. */
    val freeMoneyDelta: Double?
        get() = before.freeMoney?.let { b -> after.freeMoney?.let { a -> b - a } }

    /** The "coût en liberté" (spec section 16): drop in daily spending power. */
    val freedomPerDayDelta: Double?
        get() = before.freedomPerDay?.let { b -> after.freedomPerDay?.let { a -> b - a } }

    /** True when this expense would push the month from a safe state into (or deeper into) an alert state. */
    val wouldBreachSafetyThreshold: Boolean
        get() = after.freedomState == FreedomState.ALERT

    /** How far under the safety threshold free money would land, if at all. Positive means "under by this much". */
    val amountUnderThreshold: Double?
        get() = after.safetyMargin?.let { if (it < 0) -it else null }
}
