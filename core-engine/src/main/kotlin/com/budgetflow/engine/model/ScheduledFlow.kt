package com.budgetflow.engine.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A recurring (or one-off) income or expense, described in a way that lets
 * [com.budgetflow.engine.FrequencyProjector] compute every calendar date on
 * which it actually falls due, for any date range.
 *
 * The same shape is used for incomes and for recurring expenses: which one
 * it represents depends only on which list it is placed in when building a
 * [MonthPlan] - the amount itself is always a positive magnitude.
 */
data class ScheduledFlow(
    val id: Long,
    val label: String,
    val amount: Double,
    val frequency: Frequency,
    /** MONTHLY / YEARLY: day of month the flow is due, 1..31 (clamped to the last day of shorter months). */
    val dayOfMonth: Int? = null,
    /** WEEKLY: which weekday the flow is due. */
    val dayOfWeek: DayOfWeek? = null,
    /** YEARLY: which month (1=January..12=December) the flow is due. */
    val monthOfYear: Int? = null,
    /** ONE_TIME: the exact date the flow occurs. */
    val oneTimeDate: LocalDate? = null,
    /** Flow does not exist before this date (inclusive). Null means "always existed". */
    val startDate: LocalDate? = null,
    /** Flow does not exist after this date (inclusive). Null means "no end". */
    val endDate: LocalDate? = null,
    val isActive: Boolean = true
) {
    init {
        when (frequency) {
            Frequency.MONTHLY -> requireNotNull(dayOfMonth) { "dayOfMonth is required for a MONTHLY flow" }
            Frequency.YEARLY -> {
                requireNotNull(dayOfMonth) { "dayOfMonth is required for a YEARLY flow" }
                requireNotNull(monthOfYear) { "monthOfYear is required for a YEARLY flow" }
            }
            Frequency.WEEKLY -> requireNotNull(dayOfWeek) { "dayOfWeek is required for a WEEKLY flow" }
            Frequency.ONE_TIME -> requireNotNull(oneTimeDate) { "oneTimeDate is required for a ONE_TIME flow" }
        }
        require(dayOfMonth == null || dayOfMonth in 1..31) { "dayOfMonth must be in 1..31" }
        require(monthOfYear == null || monthOfYear in 1..12) { "monthOfYear must be in 1..12" }
    }
}

/** True direction (in vs out) a [ScheduledFlow] represents once placed in a [MonthPlan]. */
enum class FlowDirection { INCOME, EXPENSE }
