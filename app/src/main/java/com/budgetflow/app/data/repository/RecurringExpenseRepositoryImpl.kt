package com.budgetflow.app.data.repository

import com.budgetflow.app.data.local.dao.RecurringExpenseDao
import com.budgetflow.app.domain.model.RecurringExpense
import com.budgetflow.app.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecurringExpenseRepositoryImpl(private val dao: RecurringExpenseDao) : RecurringExpenseRepository {
    override fun observeExpenses(): Flow<List<RecurringExpense>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllActive(): List<RecurringExpense> = dao.getAllActive().map { it.toDomain() }

    override suspend fun upsert(expense: RecurringExpense): Long = dao.upsert(expense.toEntity())

    override suspend fun delete(expense: RecurringExpense) = dao.delete(expense.toEntity())
}
