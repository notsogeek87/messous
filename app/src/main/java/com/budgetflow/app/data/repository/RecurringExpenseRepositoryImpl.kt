package com.budgetflow.app.data.repository

import com.budgetflow.app.data.local.dao.RecurringExpenseDao
import com.budgetflow.app.data.profile.CurrentProfileProvider
import com.budgetflow.app.domain.model.RecurringExpense
import com.budgetflow.app.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class RecurringExpenseRepositoryImpl(
    private val dao: RecurringExpenseDao,
    private val currentProfile: CurrentProfileProvider
) : RecurringExpenseRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeExpenses(): Flow<List<RecurringExpense>> =
        currentProfile.currentProfileId.flatMapLatest { profileId -> dao.observeAllForProfile(profileId) }
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getAllActive(): List<RecurringExpense> =
        dao.getAllActiveForProfile(currentProfile.currentProfileId.first()).map { it.toDomain() }

    override suspend fun upsert(expense: RecurringExpense): Long =
        dao.upsert(expense.toEntity(currentProfile.currentProfileId.first()))

    override suspend fun delete(expense: RecurringExpense) =
        dao.delete(expense.toEntity(currentProfile.currentProfileId.first()))
}
