package com.budgetflow.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.model.Income
import com.budgetflow.app.domain.repository.IncomeRepository
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

data class IncomeFormState(
    val incomeId: Long? = null,
    val label: String = "",
    val amount: String = "",
    val frequency: Frequency = Frequency.MONTHLY,
    val dayOfMonth: Int = 1,
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val monthOfYear: Int = 1,
    val isActive: Boolean = true,
    val isSaved: Boolean = false
)

class AddEditIncomeViewModel(
    incomeId: Long?,
    private val incomeRepository: IncomeRepository
) : ViewModel() {

    private var existingAccountId: Long? = null
    private var existingOneTimeDate: LocalDate? = null

    private val _uiState = MutableStateFlow(IncomeFormState(incomeId = incomeId))
    val uiState: StateFlow<IncomeFormState> = _uiState.asStateFlow()

    init {
        if (incomeId != null) {
            viewModelScope.launch {
                val existing = incomeRepository.observeIncomes().first().firstOrNull { it.id == incomeId }
                if (existing != null) {
                    existingAccountId = existing.accountId
                    existingOneTimeDate = existing.oneTimeDate
                    _uiState.update {
                        it.copy(
                            label = existing.label,
                            amount = existing.amount.toString(),
                            frequency = existing.frequency,
                            dayOfMonth = existing.dayOfMonth ?: 1,
                            dayOfWeek = existing.dayOfWeek ?: DayOfWeek.MONDAY,
                            monthOfYear = existing.monthOfYear ?: 1,
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
    fun updateIsActive(value: Boolean) = _uiState.update { it.copy(isActive = value) }

    fun save() {
        val state = _uiState.value
        val amount = state.amount.toAmountOrNull() ?: return
        if (amount <= 0.0) return

        viewModelScope.launch {
            incomeRepository.upsert(
                Income(
                    id = state.incomeId ?: 0,
                    label = state.label.ifBlank { "Revenu" },
                    amount = amount,
                    frequency = state.frequency,
                    dayOfMonth = if (state.frequency == Frequency.MONTHLY || state.frequency == Frequency.YEARLY) state.dayOfMonth else null,
                    dayOfWeek = if (state.frequency == Frequency.WEEKLY) state.dayOfWeek else null,
                    monthOfYear = if (state.frequency == Frequency.YEARLY) state.monthOfYear else null,
                    oneTimeDate = if (state.frequency == Frequency.ONE_TIME) (existingOneTimeDate ?: LocalDate.now()) else null,
                    accountId = existingAccountId,
                    isActive = state.isActive
                )
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
