package com.budgetflow.app.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.model.Account
import com.budgetflow.app.domain.model.TransactionType
import com.budgetflow.app.domain.repository.AccountRepository
import com.budgetflow.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountItem(val account: Account, val currentBalance: Double)

class AccountsViewModel(
    private val accountRepository: AccountRepository,
    transactionRepository: TransactionRepository
) : ViewModel() {

    val accounts: StateFlow<List<AccountItem>> = combine(
        accountRepository.observeAccounts(),
        transactionRepository.observeAll()
    ) { accounts, transactions ->
        accounts.map { account ->
            val accountTransactions = transactions.filter { it.accountId == account.id }
            val income = accountTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = accountTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            AccountItem(account, account.initialBalance + income - expense)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(account: Account) = viewModelScope.launch { accountRepository.upsert(account) }
    fun delete(account: Account) = viewModelScope.launch { accountRepository.delete(account) }
}
