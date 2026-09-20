package com.budgetflow.app.data.repository

import com.budgetflow.app.data.local.dao.TransactionDao
import com.budgetflow.app.domain.model.Transaction
import com.budgetflow.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TransactionRepositoryImpl(private val dao: TransactionDao) : TransactionRepository {
    override fun observeAll(): Flow<List<Transaction>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeInRange(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
        dao.observeInRange(start.toEpochDay(), end.toEpochDay()).map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(transaction: Transaction): Long = dao.upsert(transaction.toEntity())

    override suspend fun delete(transaction: Transaction) = dao.delete(transaction.toEntity())
}
