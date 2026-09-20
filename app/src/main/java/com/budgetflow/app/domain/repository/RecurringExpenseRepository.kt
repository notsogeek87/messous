package com.budgetflow.app.domain.repository

import com.budgetflow.app.domain.model.RecurringExpense
import kotlinx.coroutines.flow.Flow

interface RecurringExpenseRepository {
    fun observeExpenses(): Flow<List<RecurringExpense>>
    suspend fun getAllActive(): List<RecurringExpense>
    suspend fun upsert(expense: RecurringExpense): Long
    suspend fun delete(expense: RecurringExpense)
}
