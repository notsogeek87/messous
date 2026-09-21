package com.budgetflow.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.model.Account
import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.model.Income
import com.budgetflow.app.domain.model.RecurringExpense
import com.budgetflow.app.domain.model.SavingsGoal
import com.budgetflow.app.domain.model.Transaction
import com.budgetflow.app.domain.model.TransactionType
import com.budgetflow.app.domain.model.VariableBudget
import com.budgetflow.app.domain.repository.AccountRepository
import com.budgetflow.app.domain.repository.CategoryRepository
import com.budgetflow.app.domain.repository.IncomeRepository
import com.budgetflow.app.domain.repository.RecurringExpenseRepository
import com.budgetflow.app.domain.repository.SavingsGoalRepository
import com.budgetflow.app.domain.repository.TransactionRepository
import com.budgetflow.app.domain.repository.VariableBudgetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

data class EnvelopeItem(val budget: VariableBudget, val category: Category?, val spentSoFar: Double) {
    val remaining: Double get() = budget.monthlyLimit - spentSoFar
    val progress: Float get() = if (budget.monthlyLimit <= 0) 0f else (spentSoFar / budget.monthlyLimit).toFloat().coerceIn(0f, 1f)
}

data class BudgetUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val incomes: List<Income> = emptyList(),
    val expenses: List<RecurringExpense> = emptyList(),
    val envelopes: List<EnvelopeItem> = emptyList(),
    val goals: List<SavingsGoal> = emptyList()
)

class BudgetViewModel(
    private val incomeRepository: IncomeRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val variableBudgetRepository: VariableBudgetRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<BudgetUiState> = combine(
        combine(incomeRepository.observeIncomes(), recurringExpenseRepository.observeExpenses(), variableBudgetRepository.observeBudgets()) { i, e, b -> Triple(i, e, b) },
        combine(savingsGoalRepository.observeGoals(), categoryRepository.observeCategories(), accountRepository.observeAccounts()) { g, c, a -> Triple(g, c, a) },
        transactionRepository.observeAll()
    ) { (incomes, expenses, budgets), (goals, categories, accounts), transactions ->
        buildState(incomes, expenses, budgets, goals, categories, accounts, transactions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetUiState())

    private fun buildState(
        incomes: List<Income>,
        expenses: List<RecurringExpense>,
        budgets: List<VariableBudget>,
        goals: List<SavingsGoal>,
        categories: List<Category>,
        accounts: List<Account>,
        transactions: List<Transaction>
    ): BudgetUiState {
        val month = YearMonth.now()
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()

        val envelopes = budgets.map { budget ->
            val spent = transactions.filter {
                it.categoryId == budget.categoryId && it.type == TransactionType.EXPENSE &&
                    !it.date.isBefore(monthStart) && !it.date.isAfter(monthEnd)
            }.sumOf { it.amount }
            EnvelopeItem(budget, categories.firstOrNull { it.id == budget.categoryId }, spent)
        }

        return BudgetUiState(
            isLoading = false,
            categories = categories,
            accounts = accounts,
            incomes = incomes,
            expenses = expenses,
            envelopes = envelopes,
            goals = goals
        )
    }

    fun deleteIncome(income: Income) = viewModelScope.launch { incomeRepository.delete(income) }

    fun deleteExpense(expense: RecurringExpense) = viewModelScope.launch { recurringExpenseRepository.delete(expense) }

    fun saveEnvelope(budget: VariableBudget) = viewModelScope.launch { variableBudgetRepository.upsert(budget) }
    fun deleteEnvelope(budget: VariableBudget) = viewModelScope.launch { variableBudgetRepository.delete(budget) }

    fun saveGoal(goal: SavingsGoal) = viewModelScope.launch { savingsGoalRepository.upsert(goal) }
    fun deleteGoal(goal: SavingsGoal) = viewModelScope.launch { savingsGoalRepository.delete(goal) }

    fun contributeToGoal(goal: SavingsGoal, amount: Double) = viewModelScope.launch {
        savingsGoalRepository.upsert(goal.copy(currentAmount = goal.currentAmount + amount))
    }
}
