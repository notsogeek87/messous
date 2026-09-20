package com.budgetflow.app.domain.model

import kotlin.math.roundToInt

data class SavingsGoal(
    val id: Long = 0,
    val label: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val monthlyContribution: Double = 0.0,
    val isActive: Boolean = true
) {
    val progressPercent: Int
        get() = if (targetAmount <= 0.0) 0 else ((currentAmount / targetAmount) * 100).roundToInt().coerceIn(0, 100)
}
