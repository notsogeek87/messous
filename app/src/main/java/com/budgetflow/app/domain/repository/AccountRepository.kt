package com.budgetflow.app.domain.repository

import com.budgetflow.app.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    suspend fun getAccount(id: Long): Account?
    suspend fun upsert(account: Account): Long
    suspend fun delete(account: Account)

    /** initialBalance + every income transaction - every expense transaction recorded against this account. */
    suspend fun computeCurrentBalance(accountId: Long): Double
}
