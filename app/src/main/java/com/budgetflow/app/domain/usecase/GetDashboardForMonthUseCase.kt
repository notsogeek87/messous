package com.budgetflow.app.domain.usecase

import com.budgetflow.app.domain.model.Account
import com.budgetflow.app.domain.model.Income
import com.budgetflow.app.domain.model.RecurringExpense
import com.budgetflow.app.domain.model.SavingsGoal
import com.budgetflow.app.domain.model.Transaction
import com.budgetflow.app.domain.model.TransactionType
import com.budgetflow.app.domain.model.VariableBudget
import com.budgetflow.app.domain.repository.AccountRepository
import com.budgetflow.app.domain.repository.IncomeRepository
import com.budgetflow.app.domain.repository.RecurringExpenseRepository
import com.budgetflow.app.domain.repository.SavingsGoalRepository
import com.budgetflow.app.domain.repository.TransactionRepository
import com.budgetflow.app.domain.repository.VariableBudgetRepository
import com.budgetflow.engine.BudgetEngine
import com.budgetflow.engine.model.MonthPlan
import com.budgetflow.engine.model.MonthSummary
import com.budgetflow.engine.model.VariableBudgetInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth

/**
 * Answers BudgetFlow's central question - "combien puis-je encore dépenser
 * ce mois-ci ?" - by assembling a [MonthPlan] from every repository and
 * handing it to [BudgetEngine]. Reactive: recomputes whenever any underlying
 * table changes.
 */
class GetDashboardForMonthUseCase(
    private val incomeRepository: IncomeRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val variableBudgetRepository: VariableBudgetRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    private data class Inputs(
        val incomes: List<Income>,
        val expenses: List<RecurringExpense>,
        val budgets: List<VariableBudget>,
        val goals: List<SavingsGoal>,
        val accounts: List<Account>
    )

    fun observe(month: YearMonth, today: LocalDate): Flow<MonthSummary> {
        val inputsFlow = combine(
            incomeRepository.observeIncomes(),
            recurringExpenseRepository.observeExpenses(),
            variableBudgetRepository.observeBudgets(),
            savingsGoalRepository.observeGoals(),
            accountRepository.observeAccounts()
        ) { incomes, expenses, budgets, goals, accounts ->
            Inputs(incomes, expenses, budgets, goals, accounts)
        }

        return inputsFlow.combine(transactionRepository.observeAll()) { inputs, transactions ->
            buildSummary(inputs, transactions, month, today)
        }
    }

    private fun buildSummary(
        inputs: Inputs,
        transactions: List<Transaction>,
        month: YearMonth,
        today: LocalDate
    ): MonthSummary {
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()

        val accountBalances = inputs.accounts.map { account ->
            val accountTransactions = transactions.filter { it.accountId == account.id }
            val income = accountTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = accountTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            account.initialBalance + income - expense
        }

        val variableBudgetInputs = inputs.budgets.filter { it.isActive }.map { budget ->
            val spent = transactions
                .filter {
                    it.categoryId == budget.categoryId &&
                        it.type == TransactionType.EXPENSE &&
                        !it.date.isBefore(monthStart) &&
                        !it.date.isAfter(monthEnd)
                }
                .sumOf { it.amount }
            VariableBudgetInput(id = budget.id, label = budget.label, monthlyLimit = budget.monthlyLimit, spentSoFar = spent)
        }

        val plannedSavings = inputs.goals.filter { it.isActive }.sumOf { it.monthlyContribution }

        val plan = MonthPlan(
            month = month,
            today = today,
            incomes = inputs.incomes.filter { it.isActive }.map { it.toScheduledFlow() },
            recurringExpenses = inputs.expenses.filter { it.isActive }.map { it.toScheduledFlow() },
            variableBudgets = variableBudgetInputs,
            plannedMonthlySavings = plannedSavings,
            currentAccountBalances = accountBalances
        )

        return BudgetEngine.summarizeMonth(plan)
    }
}
