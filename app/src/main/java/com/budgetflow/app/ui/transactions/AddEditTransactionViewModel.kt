package com.budgetflow.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.data.services.ServiceCategoryMatcher
import com.budgetflow.app.domain.model.Account
import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.model.Transaction
import com.budgetflow.app.domain.model.TransactionType
import com.budgetflow.app.domain.repository.AccountRepository
import com.budgetflow.app.domain.repository.CategoryRepository
import com.budgetflow.app.domain.repository.TransactionRepository
import com.budgetflow.app.ui.components.toAmountOrNull
import com.budgetflow.engine.recognition.RecognizableService
import com.budgetflow.engine.recognition.ServiceKind
import com.budgetflow.engine.recognition.ServiceMatch
import com.budgetflow.engine.recognition.TransactionRecognitionEngine
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
    val isSaved: Boolean = false,
    /** Live suggestions for [description] (spec section 3) - never forced, just offered while typing. */
    val suggestions: List<ServiceMatch> = emptyList(),
    /** Set only right after the user taps a suggestion; cleared as soon as they edit the description again. */
    val recognizedService: RecognizableService? = null,
    val isDeleted: Boolean = false
) {
    val isAmountValid: Boolean get() = amount.toAmountOrNull()?.let { it > 0.0 } == true
    /** Gates the Save button - visible validation instead of a silent no-op on tap (spec: never fail quietly). */
    val canSave: Boolean get() = isAmountValid && accountId != null
}

class AddEditTransactionViewModel(
    transactionId: Long?,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val serviceCatalog: List<RecognizableService>,
    private val serviceCategoryMatcher: ServiceCategoryMatcher
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
    fun updateAccount(id: Long) = _uiState.update { it.copy(accountId = id) }

    fun updateDescription(value: String) {
        val extraction = TransactionRecognitionEngine.extractAmount(value)
        val searchText = extraction?.remainingText ?: value
        // Not filtered by the current type toggle: a matching suggestion corrects it on selection instead.
        val matches = TransactionRecognitionEngine.suggest(searchText, serviceCatalog)
        _uiState.update { state ->
            state.copy(
                description = value,
                amount = if (extraction != null && state.amount.isBlank()) extraction.amount.toString() else state.amount,
                suggestions = matches,
                recognizedService = null
            )
        }
    }

    /** Applies a tapped suggestion (spec section 9): the user stays free to edit every field afterwards. */
    fun selectSuggestion(service: RecognizableService) {
        viewModelScope.launch {
            val category = serviceCategoryMatcher.categoryFor(service, _uiState.value.categories)
            _uiState.update { state ->
                state.copy(
                    description = service.name,
                    categoryId = category?.id ?: state.categoryId,
                    type = if (service.kind == ServiceKind.INCOME) TransactionType.INCOME else TransactionType.EXPENSE,
                    suggestions = emptyList(),
                    recognizedService = service
                )
            }
        }
    }

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

    fun delete() {
        val state = _uiState.value
        val id = state.transactionId ?: return
        viewModelScope.launch {
            transactionRepository.delete(
                Transaction(
                    id = id,
                    amount = state.amount.toAmountOrNull() ?: 0.0,
                    type = state.type,
                    categoryId = state.categoryId,
                    date = state.date,
                    description = state.description,
                    accountId = state.accountId ?: 0,
                    createdAtEpochMillis = existingCreatedAt.value ?: System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(isDeleted = true) }
        }
    }
}
