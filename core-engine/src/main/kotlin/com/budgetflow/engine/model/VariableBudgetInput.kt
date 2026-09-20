package com.budgetflow.engine.model

/**
 * A monthly spending envelope (e.g. "Courses: 500 EUR") and how much of it
 * has already been spent this month.
 */
data class VariableBudgetInput(
    val id: Long,
    val label: String,
    val monthlyLimit: Double,
    val spentSoFar: Double
) {
    val remaining: Double get() = monthlyLimit - spentSoFar
}
