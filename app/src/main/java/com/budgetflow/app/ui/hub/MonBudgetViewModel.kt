package com.budgetflow.app.ui.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.data.prefs.UserPreferences
import com.budgetflow.app.domain.repository.IncomeRepository
import com.budgetflow.app.domain.repository.RecurringExpenseRepository
import com.budgetflow.app.domain.repository.SavingsGoalRepository
import com.budgetflow.app.domain.repository.TransactionRepository
import com.budgetflow.app.domain.repository.VariableBudgetRepository
import com.budgetflow.app.domain.usecase.GetDashboardForMonthUseCase
import com.budgetflow.engine.model.MonthSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class MonBudgetUiState(
    val isLoading: Boolean = true,
    val summary: MonthSummary? = null,
    val safetyThreshold: Double = 0.0,
    val incomeCount: Int = 0,
    val expenseCount: Int = 0,
    val envelopeCount: Int = 0,
    val goalCount: Int = 0,
    val transactionsThisMonthCount: Int = 0
)

/**
 * Drives "Mon budget" (spec §10.2): the hub that replaced "Moi" - the user's actual plan and
 * money, each row carrying the value it leads to instead of a bare label, so this screen is a
 * dashboard of the budget rather than a table of contents for it.
 */
class MonBudgetViewModel(
    dashboardUseCase: GetDashboardForMonthUseCase,
    incomeRepository: IncomeRepository,
    recurringExpenseRepository: RecurringExpenseRepository,
    variableBudgetRepository: VariableBudgetRepository,
    savingsGoalRepository: SavingsGoalRepository,
    transactionRepository: TransactionRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val today = LocalDate.now()
    private val month = YearMonth.from(today)
    private val monthStart = month.atDay(1)
    private val monthEnd = month.atEndOfMonth()

    val uiState: StateFlow<MonBudgetUiState> = combine(
        combine(dashboardUseCase.observe(month, today), preferences.safetyThreshold) { summary, threshold -> summary to threshold },
        combine(
            incomeRepository.observeIncomes(),
            recurringExpenseRepository.observeExpenses(),
            variableBudgetRepository.observeBudgets()
        ) { incomes, expenses, budgets -> Triple(incomes, expenses, budgets) },
        combine(
            savingsGoalRepository.observeGoals(),
            transactionRepository.observeInRange(monthStart, monthEnd)
        ) { goals, transactions -> goals to transactions }
    ) { (summary, threshold), (incomes, expenses, budgets), (goals, transactions) ->
        MonBudgetUiState(
            isLoading = false,
            summary = summary,
            safetyThreshold = threshold,
            incomeCount = incomes.count { it.isActive },
            expenseCount = expenses.count { it.isActive },
            envelopeCount = budgets.count { it.isActive },
            goalCount = goals.count { it.isActive },
            transactionsThisMonthCount = transactions.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonBudgetUiState())

    fun setSafetyThreshold(amount: Double) = viewModelScope.launch { preferences.setSafetyThreshold(amount) }
}
