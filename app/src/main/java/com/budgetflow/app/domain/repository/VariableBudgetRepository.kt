package com.budgetflow.app.domain.repository

import com.budgetflow.app.domain.model.VariableBudget
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface VariableBudgetRepository {
    fun observeBudgets(): Flow<List<VariableBudget>>
    suspend fun getAllActive(): List<VariableBudget>
    suspend fun upsert(budget: VariableBudget): Long
    suspend fun delete(budget: VariableBudget)

    /** Sum of expense transactions tagged with [budget]'s category within [monthStart]..[monthEnd]. */
    suspend fun spentSoFar(budget: VariableBudget, monthStart: LocalDate, monthEnd: LocalDate): Double
}
