package com.budgetflow.app.data.repository

import com.budgetflow.app.data.local.dao.SavingsGoalDao
import com.budgetflow.app.domain.model.SavingsGoal
import com.budgetflow.app.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavingsGoalRepositoryImpl(private val dao: SavingsGoalDao) : SavingsGoalRepository {
    override fun observeGoals(): Flow<List<SavingsGoal>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllActive(): List<SavingsGoal> = dao.getAllActive().map { it.toDomain() }

    override suspend fun upsert(goal: SavingsGoal): Long = dao.upsert(goal.toEntity())

    override suspend fun delete(goal: SavingsGoal) = dao.delete(goal.toEntity())
}
