package com.budgetflow.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.model.Account
import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.model.Transaction
import com.budgetflow.app.domain.repository.AccountRepository
import com.budgetflow.app.domain.repository.CategoryRepository
import com.budgetflow.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionListItem(
    val transaction: Transaction,
    val category: Category?,
    val account: Account?
)

data class TransactionsUiState(
    val isLoading: Boolean = true,
    val items: List<TransactionListItem> = emptyList()
)

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository
) : ViewModel() {

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeCategories(),
        accountRepository.observeAccounts()
    ) { transactions, categories, accounts ->
        TransactionsUiState(
            isLoading = false,
            items = transactions.map { transaction ->
                TransactionListItem(
                    transaction = transaction,
                    category = categories.firstOrNull { it.id == transaction.categoryId },
                    account = accounts.firstOrNull { it.id == transaction.accountId }
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionsUiState())

    fun delete(transaction: Transaction) {
        viewModelScope.launch { transactionRepository.delete(transaction) }
    }
}
