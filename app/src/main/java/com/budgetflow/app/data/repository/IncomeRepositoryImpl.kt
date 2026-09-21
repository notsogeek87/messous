package com.budgetflow.app.data.repository

import com.budgetflow.app.data.local.dao.IncomeDao
import com.budgetflow.app.data.profile.CurrentProfileProvider
import com.budgetflow.app.domain.model.Income
import com.budgetflow.app.domain.repository.IncomeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class IncomeRepositoryImpl(
    private val incomeDao: IncomeDao,
    private val currentProfile: CurrentProfileProvider
) : IncomeRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeIncomes(): Flow<List<Income>> =
        currentProfile.currentProfileId.flatMapLatest { profileId -> incomeDao.observeAllForProfile(profileId) }
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getAllActive(): List<Income> =
        incomeDao.getAllActiveForProfile(currentProfile.currentProfileId.first()).map { it.toDomain() }

    override suspend fun upsert(income: Income): Long =
        incomeDao.upsert(income.toEntity(currentProfile.currentProfileId.first()))

    override suspend fun delete(income: Income) =
        incomeDao.delete(income.toEntity(currentProfile.currentProfileId.first()))
}
