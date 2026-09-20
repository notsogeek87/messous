package com.budgetflow.app.domain.repository

import com.budgetflow.app.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {
    fun observeGoals(): Flow<List<SavingsGoal>>
    suspend fun getAllActive(): List<SavingsGoal>
    suspend fun upsert(goal: SavingsGoal): Long
    suspend fun delete(goal: SavingsGoal)
}
