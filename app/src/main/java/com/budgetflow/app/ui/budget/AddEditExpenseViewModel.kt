package com.budgetflow.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.model.RecurringExpense
import com.budgetflow.app.domain.repository.CategoryRepository
import com.budgetflow.app.domain.repository.RecurringExpenseRepository
import com.budgetflow.app.ui.components.toAmountOrNull
import com.budgetflow.engine.model.Frequency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

data class ExpenseFormState(
    val expenseId: Long? = null,
    val label: String = "",
    val amount: String = "",
    val frequency: Frequency = Frequency.MONTHLY,
    val dayOfMonth: Int = 1,
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val monthOfYear: Int = 1,
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val isActive: Boolean = true,
    val isSaved: Boolean = false
)

class AddEditExpenseViewModel(
    expenseId: Long?,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private var existingAccountId: Long? = null
    private var existingOneTimeDate: LocalDate? = null
    private var existingIsFixedAmount: Boolean = true

    private val _uiState = MutableStateFlow(ExpenseFormState(expenseId = expenseId))
    val uiState: StateFlow<ExpenseFormState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        if (expenseId != null) {
            viewModelScope.launch {
                val existing = recurringExpenseRepository.observeExpenses().first().firstOrNull { it.id == expenseId }
                if (existing != null) {
                    existingAccountId = existing.accountId
                    existingOneTimeDate = existing.oneTimeDate
                    existingIsFixedAmount = existing.isFixedAmount
                    _uiState.update {
                        it.copy(
                            label = existing.label,
                            amount = existing.amount.toString(),
                            frequency = existing.frequency,
                            dayOfMonth = existing.dayOfMonth ?: 1,
                            dayOfWeek = existing.dayOfWeek ?: DayOfWeek.MONDAY,
                            monthOfYear = existing.monthOfYear ?: 1,
                            categoryId = existing.categoryId,
                            isActive = existing.isActive
                        )
                    }
                }
            }
        }
    }

    fun updateLabel(value: String) = _uiState.update { it.copy(label = value) }
    fun updateAmount(value: String) = _uiState.update { it.copy(amount = value) }
    fun updateFrequency(value: Frequency) = _uiState.update { it.copy(frequency = value) }
    fun updateDayOfMonth(value: Int) = _uiState.update { it.copy(dayOfMonth = value) }
    fun updateDayOfWeek(value: DayOfWeek) = _uiState.update { it.copy(dayOfWeek = value) }
    fun updateMonthOfYear(value: Int) = _uiState.update { it.copy(monthOfYear = value) }
    fun updateCategory(id: Long) = _uiState.update { it.copy(categoryId = id) }
    fun updateIsActive(value: Boolean) = _uiState.update { it.copy(isActive = value) }

    fun save() {
        val state = _uiState.value
        val amount = state.amount.toAmountOrNull() ?: return
        if (amount <= 0.0) return

        viewModelScope.launch {
            recurringExpenseRepository.upsert(
                RecurringExpense(
                    id = state.expenseId ?: 0,
                    label = state.label.ifBlank { "Dépense" },
                    amount = amount,
                    frequency = state.frequency,
                    dayOfMonth = if (state.frequency == Frequency.MONTHLY || state.frequency == Frequency.YEARLY) state.dayOfMonth else null,
                    dayOfWeek = if (state.frequency == Frequency.WEEKLY) state.dayOfWeek else null,
                    monthOfYear = if (state.frequency == Frequency.YEARLY) state.monthOfYear else null,
                    oneTimeDate = if (state.frequency == Frequency.ONE_TIME) (existingOneTimeDate ?: LocalDate.now()) else null,
                    accountId = existingAccountId,
                    categoryId = state.categoryId,
                    isFixedAmount = existingIsFixedAmount,
                    isActive = state.isActive
                )
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
