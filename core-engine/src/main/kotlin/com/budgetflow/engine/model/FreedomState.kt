package com.budgetflow.engine.model

/**
 * The month's overall financial "weather", derived from [MonthSummary.safetyMargin].
 * Deliberately not a judgment: it only reflects distance to the user's own safety
 * threshold, never a moral verdict on their spending.
 */
enum class FreedomState {
    /** Comfortably above the safety threshold (or no threshold set and free money is positive). */
    COMFORT,
    /** Still above the safety threshold, but the cushion beyond it is thin. */
    CAUTION,
    /** Below the safety threshold: free money would not cover the buffer the user asked to keep. */
    ALERT
}
