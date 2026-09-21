package com.budgetflow.app.data.repository

import com.budgetflow.app.data.local.dao.SavingsGoalDao
import com.budgetflow.app.data.profile.CurrentProfileProvider
import com.budgetflow.app.domain.model.SavingsGoal
import com.budgetflow.app.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class SavingsGoalRepositoryImpl(
    private val dao: SavingsGoalDao,
    private val currentProfile: CurrentProfileProvider
) : SavingsGoalRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeGoals(): Flow<List<SavingsGoal>> =
        currentProfile.currentProfileId.flatMapLatest { profileId -> dao.observeAllForProfile(profileId) }
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getAllActive(): List<SavingsGoal> =
        dao.getAllActiveForProfile(currentProfile.currentProfileId.first()).map { it.toDomain() }

    override suspend fun upsert(goal: SavingsGoal): Long =
        dao.upsert(goal.toEntity(currentProfile.currentProfileId.first()))

    override suspend fun delete(goal: SavingsGoal) =
        dao.delete(goal.toEntity(currentProfile.currentProfileId.first()))
}
