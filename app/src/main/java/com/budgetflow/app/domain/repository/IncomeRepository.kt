package com.budgetflow.app.domain.repository

import com.budgetflow.app.domain.model.Income
import kotlinx.coroutines.flow.Flow

interface IncomeRepository {
    fun observeIncomes(): Flow<List<Income>>
    suspend fun getAllActive(): List<Income>
    suspend fun upsert(income: Income): Long
    suspend fun delete(income: Income)
}
