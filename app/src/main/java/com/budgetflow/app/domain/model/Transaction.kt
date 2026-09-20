package com.budgetflow.app.domain.model

import java.time.LocalDate

enum class TransactionType { INCOME, EXPENSE }

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long? = null,
    val date: LocalDate,
    val description: String = "",
    val accountId: Long,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
