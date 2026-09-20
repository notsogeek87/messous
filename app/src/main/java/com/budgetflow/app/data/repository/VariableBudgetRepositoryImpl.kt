package com.budgetflow.app.data.repository

import com.budgetflow.app.data.local.dao.TransactionDao
import com.budgetflow.app.data.local.dao.VariableBudgetDao
import com.budgetflow.app.domain.model.VariableBudget
import com.budgetflow.app.domain.repository.VariableBudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class VariableBudgetRepositoryImpl(
    private val variableBudgetDao: VariableBudgetDao,
    private val transactionDao: TransactionDao
) : VariableBudgetRepository {

    override fun observeBudgets(): Flow<List<VariableBudget>> =
        variableBudgetDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllActive(): List<VariableBudget> = variableBudgetDao.getAllActive().map { it.toDomain() }

    override suspend fun upsert(budget: VariableBudget): Long = variableBudgetDao.upsert(budget.toEntity())

    override suspend fun delete(budget: VariableBudget) = variableBudgetDao.delete(budget.toEntity())

    override suspend fun spentSoFar(budget: VariableBudget, monthStart: LocalDate, monthEnd: LocalDate): Double =
        transactionDao.sumExpensesForCategoryInRange(budget.categoryId, monthStart.toEpochDay(), monthEnd.toEpochDay())
}
