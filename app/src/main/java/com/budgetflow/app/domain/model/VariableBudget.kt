package com.budgetflow.app.domain.model

data class VariableBudget(
    val id: Long = 0,
    val label: String,
    val monthlyLimit: Double,
    val categoryId: Long,
    val isActive: Boolean = true
)
