package com.budgetflow.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.data.prefs.UserPreferences
import com.budgetflow.app.domain.model.Account
import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.model.Income
import com.budgetflow.app.domain.model.RecurringExpense
import com.budgetflow.app.domain.model.VariableBudget
import com.budgetflow.app.domain.repository.AccountRepository
import com.budgetflow.app.domain.repository.CategoryRepository
import com.budgetflow.app.domain.repository.IncomeRepository
import com.budgetflow.app.domain.repository.RecurringExpenseRepository
import com.budgetflow.app.domain.repository.VariableBudgetRepository
import com.budgetflow.app.ui.components.toAmountOrNull
import com.budgetflow.engine.BudgetEngine
import com.budgetflow.engine.model.Frequency
import com.budgetflow.engine.model.MonthPlan
import com.budgetflow.engine.model.MonthSummary
import com.budgetflow.engine.model.VariableBudgetInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

const val ONBOARDING_STEP_WELCOME = 0
const val ONBOARDING_STEP_PRIVACY = 1
const val ONBOARDING_STEP_INCOME = 2
const val ONBOARDING_STEP_EXPENSES = 3
const val ONBOARDING_STEP_VARIABLE = 4
const val ONBOARDING_STEP_SAFETY = 5
const val ONBOARDING_STEP_DONE = 6

data class OnboardingUiState(
    val step: Int = ONBOARDING_STEP_WELCOME,
    val categories: List<Category> = emptyList(),
    val incomeLabel: String = "Salaire",
    val incomeAmount: String = "",
    val pendingExpenses: List<RecurringExpense> = emptyList(),
    val pendingEnvelopes: List<VariableBudget> = emptyList(),
    /** "Combien as-tu sur ton compte aujourd'hui ?" (spec §6/Lot 4 P5) - optional, left blank means
     * "Ma liberté" falls back to the plan-based figure until an account balance exists. */
    val currentBalance: String = "",
    /** "Combien veux-tu ne jamais descendre en dessous ?" - prefilled with [suggestedSafetyThreshold]
     * the first time this step is reached, but always editable/clearable. */
    val safetyThreshold: String = "",
    val previewSummary: MonthSummary? = null,
    val isFinished: Boolean = false
) {
    /** A starting suggestion, not a rule: roughly one month of fixed costs, so the field never
     * opens on a blank page the user has no idea how to fill. */
    val suggestedSafetyThreshold: Double get() = pendingExpenses.sumOf { it.amount }
}

class OnboardingViewModel(
    private val categoryRepository: CategoryRepository,
    private val incomeRepository: IncomeRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val variableBudgetRepository: VariableBudgetRepository,
    private val accountRepository: AccountRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.seedDefaultsIfEmpty()
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun goToStep(step: Int) {
        _uiState.update { current ->
            val prefilledThreshold = if (step == ONBOARDING_STEP_SAFETY && current.safetyThreshold.isBlank() && current.suggestedSafetyThreshold > 0.0) {
                current.suggestedSafetyThreshold.toString()
            } else {
                current.safetyThreshold
            }
            current.copy(step = step, safetyThreshold = prefilledThreshold)
        }
        if (step == ONBOARDING_STEP_DONE) computePreview()
    }

    fun nextStep() = goToStep((_uiState.value.step + 1).coerceAtMost(ONBOARDING_STEP_DONE))
    fun previousStep() = goToStep((_uiState.value.step - 1).coerceAtLeast(ONBOARDING_STEP_WELCOME))

    fun updateIncomeLabel(label: String) = _uiState.update { it.copy(incomeLabel = label) }
    fun updateIncomeAmount(amount: String) = _uiState.update { it.copy(incomeAmount = amount) }

    fun addExpense(label: String, amount: String, categoryId: Long?, dayOfMonth: Int) {
        val value = amount.toAmountOrNull() ?: return
        if (label.isBlank()) return
        val expense = RecurringExpense(
            label = label,
            amount = value,
            frequency = Frequency.MONTHLY,
            dayOfMonth = dayOfMonth,
            categoryId = categoryId
        )
        _uiState.update { it.copy(pendingExpenses = it.pendingExpenses + expense) }
    }

    fun removeExpense(expense: RecurringExpense) =
        _uiState.update { it.copy(pendingExpenses = it.pendingExpenses - expense) }

    fun addEnvelope(label: String, categoryId: Long?, monthlyLimit: String) {
        val value = monthlyLimit.toAmountOrNull() ?: return
        if (label.isBlank() || categoryId == null) return
        val envelope = VariableBudget(label = label, monthlyLimit = value, categoryId = categoryId)
        _uiState.update { it.copy(pendingEnvelopes = it.pendingEnvelopes + envelope) }
    }

    fun removeEnvelope(envelope: VariableBudget) =
        _uiState.update { it.copy(pendingEnvelopes = it.pendingEnvelopes - envelope) }

    fun updateCurrentBalance(value: String) = _uiState.update { it.copy(currentBalance = value) }
    fun updateSafetyThreshold(value: String) = _uiState.update { it.copy(safetyThreshold = value) }

    private fun computePreview() {
        val state = _uiState.value
        val today = LocalDate.now()
        val incomeAmount = state.incomeAmount.toAmountOrNull()
        val incomes = if (incomeAmount != null && incomeAmount > 0) {
            listOf(Income(label = state.incomeLabel.ifBlank { "Revenu" }, amount = incomeAmount, frequency = Frequency.MONTHLY, dayOfMonth = 1).toScheduledFlow())
        } else emptyList()

        val balance = state.currentBalance.toAmountOrNull()
        val plan = MonthPlan(
            month = YearMonth.from(today),
            today = today,
            incomes = incomes,
            recurringExpenses = state.pendingExpenses.map { it.toScheduledFlow() },
            variableBudgets = state.pendingEnvelopes.map { VariableBudgetInput(0, it.label, it.monthlyLimit, 0.0) },
            plannedMonthlySavings = 0.0,
            currentAccountBalances = if (balance != null) listOf(balance) else emptyList(),
            safetyThreshold = state.safetyThreshold.toAmountOrNull() ?: 0.0
        )
        _uiState.update { it.copy(previewSummary = BudgetEngine.summarizeMonth(plan)) }
    }

    /** Persists everything entered during onboarding and marks it complete. Safe to call with all-empty state (spec: skippable). */
    fun finish() {
        viewModelScope.launch {
            val state = _uiState.value

            if (accountRepository.observeAccounts().first().isEmpty()) {
                // Ensure at least one account exists so incomes/expenses can be attached to it later if the user wishes.
                // Its starting balance is whatever was entered on the safety step, so "Ma liberté"
                // can show a real, balance-based figure from the very first launch (spec §6/Lot 4).
                val initialBalance = state.currentBalance.toAmountOrNull() ?: 0.0
                accountRepository.upsert(Account(name = "Compte courant", initialBalance = initialBalance))
            }

            val incomeAmount = state.incomeAmount.toAmountOrNull()
            if (incomeAmount != null && incomeAmount > 0) {
                incomeRepository.upsert(
                    Income(label = state.incomeLabel.ifBlank { "Revenu" }, amount = incomeAmount, frequency = Frequency.MONTHLY, dayOfMonth = 1)
                )
            }

            state.pendingExpenses.forEach { recurringExpenseRepository.upsert(it) }
            state.pendingEnvelopes.forEach { variableBudgetRepository.upsert(it) }

            state.safetyThreshold.toAmountOrNull()?.takeIf { it > 0.0 }?.let { threshold ->
                preferences.setSafetyThreshold(threshold)
            }

            preferences.setOnboardingDone(true)
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    fun skip() {
        viewModelScope.launch {
            if (accountRepository.observeAccounts().first().isEmpty()) {
                // Same as finish(): without at least one account, "Ma liberté" can never
                // compute a bank-balance-based free money figure, however much income/expense
                // data the user later adds directly from the Budget screen.
                accountRepository.upsert(Account(name = "Compte courant", initialBalance = 0.0))
            }
            preferences.setOnboardingDone(true)
            _uiState.update { it.copy(isFinished = true) }
        }
    }
}
