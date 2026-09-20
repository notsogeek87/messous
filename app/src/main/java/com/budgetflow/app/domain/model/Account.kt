package com.budgetflow.app.domain.model

data class Account(
    val id: Long = 0,
    val name: String,
    val initialBalance: Double,
    val currency: String = "EUR",
    val isArchived: Boolean = false
)
