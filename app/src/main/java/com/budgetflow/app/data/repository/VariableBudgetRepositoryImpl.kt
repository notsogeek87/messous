package com.budgetflow.app.data.repository

import com.budgetflow.app.data.local.dao.TransactionDao
import com.budgetflow.app.data.local.dao.VariableBudgetDao
import com.budgetflow.app.data.profile.CurrentProfileProvider
import com.budgetflow.app.domain.model.VariableBudget
import com.budgetflow.app.domain.repository.VariableBudgetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class VariableBudgetRepositoryImpl(
    private val variableBudgetDao: VariableBudgetDao,
    private val transactionDao: TransactionDao,
    private val currentProfile: CurrentProfileProvider
) : VariableBudgetRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeBudgets(): Flow<List<VariableBudget>> =
        currentProfile.currentProfileId.flatMapLatest { profileId -> variableBudgetDao.observeAllForProfile(profileId) }
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getAllActive(): List<VariableBudget> =
        variableBudgetDao.getAllActiveForProfile(currentProfile.currentProfileId.first()).map { it.toDomain() }

    override suspend fun upsert(budget: VariableBudget): Long =
        variableBudgetDao.upsert(budget.toEntity(currentProfile.currentProfileId.first()))

    override suspend fun delete(budget: VariableBudget) =
        variableBudgetDao.delete(budget.toEntity(currentProfile.currentProfileId.first()))

    override suspend fun spentSoFar(budget: VariableBudget, monthStart: LocalDate, monthEnd: LocalDate): Double =
        transactionDao.sumExpensesForCategoryInRangeForProfile(
            currentProfile.currentProfileId.first(),
            budget.categoryId,
            monthStart.toEpochDay(),
            monthEnd.toEpochDay()
        )
}
