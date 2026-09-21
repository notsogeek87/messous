package com.budgetflow.app.data.repository

import com.budgetflow.app.data.local.dao.TransactionDao
import com.budgetflow.app.data.profile.CurrentProfileProvider
import com.budgetflow.app.domain.model.Transaction
import com.budgetflow.app.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TransactionRepositoryImpl(
    private val dao: TransactionDao,
    private val currentProfile: CurrentProfileProvider
) : TransactionRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<Transaction>> =
        currentProfile.currentProfileId.flatMapLatest { profileId -> dao.observeAllForProfile(profileId) }
            .map { list -> list.map { it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeInRange(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
        currentProfile.currentProfileId.flatMapLatest { profileId ->
            dao.observeInRangeForProfile(profileId, start.toEpochDay(), end.toEpochDay())
        }.map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(transaction: Transaction): Long =
        dao.upsert(transaction.toEntity(currentProfile.currentProfileId.first()))

    override suspend fun delete(transaction: Transaction) =
        dao.delete(transaction.toEntity(currentProfile.currentProfileId.first()))
}
