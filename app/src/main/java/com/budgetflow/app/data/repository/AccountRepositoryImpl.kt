package com.budgetflow.app.data.repository

import com.budgetflow.app.data.local.dao.AccountDao
import com.budgetflow.app.data.local.dao.TransactionDao
import com.budgetflow.app.domain.model.Account
import com.budgetflow.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepositoryImpl(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAccount(id: Long): Account? = accountDao.getById(id)?.toDomain()

    override suspend fun upsert(account: Account): Long = accountDao.upsert(account.toEntity())

    override suspend fun delete(account: Account) = accountDao.delete(account.toEntity())

    override suspend fun computeCurrentBalance(accountId: Long): Double {
        val account = accountDao.getById(accountId) ?: return 0.0
        val income = transactionDao.sumIncomeForAccount(accountId)
        val expense = transactionDao.sumExpenseForAccount(accountId)
        return account.initialBalance + income - expense
    }
}
