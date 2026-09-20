package com.budgetflow.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.model.Account
import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.model.Transaction
import com.budgetflow.app.domain.model.TransactionType
import com.budgetflow.app.domain.repository.AccountRepository
import com.budgetflow.app.domain.repository.CategoryRepository
import com.budgetflow.app.domain.repository.TransactionRepository
import com.budgetflow.app.ui.components.toAmountOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TransactionFormState(
    val transactionId: Long? = null,
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    val accountId: Long? = null,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isSaved: Boolean = false
)

class AddEditTransactionViewModel(
    transactionId: Long?,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val existingCreatedAt: MutableStateFlow<Long?> = MutableStateFlow(null)
    private val _uiState = MutableStateFlow(TransactionFormState(transactionId = transactionId))
    val uiState: StateFlow<TransactionFormState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        viewModelScope.launch {
            accountRepository.observeAccounts().collect { accounts ->
                _uiState.update { current ->
                    current.copy(accounts = accounts, accountId = current.accountId ?: accounts.firstOrNull()?.id)
                }
            }
        }
        if (transactionId != null) {
            viewModelScope.launch {
                val existing = transactionRepository.observeAll().first().firstOrNull { it.id == transactionId }
                if (existing != null) {
                    existingCreatedAt.value = existing.createdAtEpochMillis
                    _uiState.update {
                        it.copy(
                            amount = existing.amount.toString(),
                            type = existing.type,
                            categoryId = existing.categoryId,
                            date = existing.date,
                            description = existing.description,
                            accountId = existing.accountId
                        )
                    }
                }
            }
        }
    }

    fun updateAmount(value: String) = _uiState.update { it.copy(amount = value) }
    fun updateType(value: TransactionType) = _uiState.update { it.copy(type = value) }
    fun updateCategory(id: Long) = _uiState.update { it.copy(categoryId = id) }
    fun updateDate(date: LocalDate) = _uiState.update { it.copy(date = date) }
    fun updateDescription(value: String) = _uiState.update { it.copy(description = value) }
    fun updateAccount(id: Long) = _uiState.update { it.copy(accountId = id) }

    fun save() {
        val state = _uiState.value
        val amount = state.amount.toAmountOrNull() ?: return
        val accountId = state.accountId ?: return
        if (amount <= 0.0) return

        viewModelScope.launch {
            transactionRepository.upsert(
                Transaction(
                    id = state.transactionId ?: 0,
                    amount = amount,
                    type = state.type,
                    categoryId = state.categoryId,
                    date = state.date,
                    description = state.description,
                    accountId = accountId,
                    createdAtEpochMillis = existingCreatedAt.value ?: System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
