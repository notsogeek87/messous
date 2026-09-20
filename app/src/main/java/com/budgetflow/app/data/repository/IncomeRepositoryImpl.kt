package com.budgetflow.app.data.repository

import com.budgetflow.app.data.local.dao.IncomeDao
import com.budgetflow.app.domain.model.Income
import com.budgetflow.app.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IncomeRepositoryImpl(private val incomeDao: IncomeDao) : IncomeRepository {
    override fun observeIncomes(): Flow<List<Income>> =
        incomeDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllActive(): List<Income> = incomeDao.getAllActive().map { it.toDomain() }

    override suspend fun upsert(income: Income): Long = incomeDao.upsert(income.toEntity())

    override suspend fun delete(income: Income) = incomeDao.delete(income.toEntity())
}
