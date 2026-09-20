package com.budgetflow.app.domain.repository

import com.budgetflow.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    fun observeInRange(start: LocalDate, end: LocalDate): Flow<List<Transaction>>
    suspend fun upsert(transaction: Transaction): Long
    suspend fun delete(transaction: Transaction)
}
