package com.budgetflow.app.ui.whatif

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.model.Account
import com.budgetflow.app.domain.model.SavingsGoal
import com.budgetflow.app.domain.model.Transaction
import com.budgetflow.app.domain.model.TransactionType
import com.budgetflow.app.domain.repository.AccountRepository
import com.budgetflow.app.domain.repository.SavingsGoalRepository
import com.budgetflow.app.domain.repository.TransactionRepository
import com.budgetflow.app.domain.usecase.SimulateExpenseUseCase
import com.budgetflow.app.ui.components.toAmountOrNull
import com.budgetflow.engine.BudgetEngine
import com.budgetflow.engine.model.ExpenseSimulation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class GoalImpact(val goal: SavingsGoal, val delayDays: Int)

data class WhatIfScenarios(
    val now: ExpenseSimulation? = null,
    val in15Days: ExpenseSimulation? = null,
    val nextMonth: ExpenseSimulation? = null
)

data class WhatIfUiState(
    val amountInput: String = "",
    val labelInput: String = "",
    val scenarios: WhatIfScenarios = WhatIfScenarios(),
    val goalImpacts: List<GoalImpact> = emptyList(),
    val accounts: List<Account> = emptyList()
) {
    val amount: Double? get() = amountInput.toAmountOrNull()?.takeIf { it > 0.0 }
}

/** Drives "Et si...?" (spec sections 10-13, 16, 23): a pure, throwaway simulation until the user explicitly commits it. */
@OptIn(ExperimentalCoroutinesApi::class)
class WhatIfViewModel(
    private val simulateExpenseUseCase: SimulateExpenseUseCase,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val amountInput = MutableStateFlow("")
    private val labelInput = MutableStateFlow("")

    private val scenariosFlow = amountInput.flatMapLatest { input ->
        val parsed = input.toAmountOrNull()?.takeIf { it > 0.0 }
        if (parsed == null) {
            flowOf(WhatIfScenarios())
        } else {
            combine(
                simulateExpenseUseCase.observeNow(parsed, today),
                simulateExpenseUseCase.observeOn(parsed, today.plusDays(15)),
                simulateExpenseUseCase.observeOn(parsed, YearMonth.from(today).plusMonths(1).atDay(1))
            ) { now, in15, nextMonth -> WhatIfScenarios(now, in15, nextMonth) }
        }
    }

    private val goalImpactsFlow = amountInput.flatMapLatest { input ->
        val parsed = input.toAmountOrNull()?.takeIf { it > 0.0 }
        if (parsed == null) {
            flowOf(emptyList<GoalImpact>())
        } else {
            savingsGoalRepository.observeGoals().map { goals ->
                goals.filter { it.isActive }.mapNotNull { goal ->
                    BudgetEngine.goalDelayDays(parsed, goal.monthlyContribution)?.let { days -> GoalImpact(goal, days) }
                }
            }
        }
    }

    val uiState: StateFlow<WhatIfUiState> = combine(
        amountInput,
        labelInput,
        scenariosFlow,
        goalImpactsFlow,
        accountRepository.observeAccounts()
    ) { amountText, label, scenarios, goalImpacts, accounts ->
        WhatIfUiState(
            amountInput = amountText,
            labelInput = label,
            scenarios = scenarios,
            goalImpacts = goalImpacts,
            accounts = accounts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WhatIfUiState())

    fun setAmountInput(value: String) {
        amountInput.value = value
    }

    fun setLabelInput(value: String) {
        labelInput.value = value
    }

    fun reset() {
        amountInput.value = ""
        labelInput.value = ""
    }

    /** Turns the simulation into a real expense (spec section 23): the only path that ever writes data. */
    fun confirmRealExpense(onAdded: () -> Unit) {
        val state = uiState.value
        val amount = state.amount ?: return
        val accountId = state.accounts.firstOrNull { !it.isArchived }?.id ?: return
        viewModelScope.launch {
            transactionRepository.upsert(
                Transaction(
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    date = today,
                    description = state.labelInput,
                    accountId = accountId
                )
            )
            reset()
            onAdded()
        }
    }
}
