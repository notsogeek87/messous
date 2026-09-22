package com.budgetflow.app.domain.usecase

import com.budgetflow.app.data.prefs.UserPreferences
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
import com.budgetflow.engine.FrequencyProjector
import com.budgetflow.engine.model.MonthPlan
import com.budgetflow.engine.model.MonthSummary
import com.budgetflow.engine.model.VariableBudgetInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

/**
 * Answers BudgetFlow's central question - "combien puis-je encore dépenser
 * ce mois-ci ?" - by assembling a [MonthPlan] from every repository and
 * handing it to [BudgetEngine]. Reactive: recomputes whenever any underlying
 * table changes.
 *
 * [observePlan] is exposed separately from [observe] so other features (the
 * "Et si...?" simulator, the future timeline) can reuse the exact same plan
 * construction logic without duplicating it or touching the database.
 */
class GetDashboardForMonthUseCase(
    private val incomeRepository: IncomeRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val variableBudgetRepository: VariableBudgetRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val userPreferences: UserPreferences
) {
    private data class RepositoryInputs(
        val incomes: List<Income>,
        val expenses: List<RecurringExpense>,
        val budgets: List<VariableBudget>,
        val goals: List<SavingsGoal>,
        val accounts: List<Account>
    )

    private data class Inputs(
        val incomes: List<Income>,
        val expenses: List<RecurringExpense>,
        val budgets: List<VariableBudget>,
        val goals: List<SavingsGoal>,
        val accounts: List<Account>,
        val safetyThreshold: Double
    )

    fun observe(month: YearMonth, today: LocalDate): Flow<MonthSummary> =
        observePlan(month, today).map(BudgetEngine::summarizeMonth)

    /**
     * @param today the date being evaluated - usually the real "now", but a future date for a
     *   "Et si...?" scenario ([realToday] then stays the real one).
     * @param realToday the actual current date, used to fetch the real account balance and to
     *   know how much of the gap between now and [today] still needs to be projected. Defaults
     *   to [today] so every caller that only ever evaluates the real current month (the
     *   dashboard, "Mon futur", "Mon budget"...) keeps behaving exactly as before.
     */
    fun observePlan(month: YearMonth, today: LocalDate, realToday: LocalDate = today): Flow<MonthPlan> {
        val repositoryInputsFlow = combine(
            incomeRepository.observeIncomes(),
            recurringExpenseRepository.observeExpenses(),
            variableBudgetRepository.observeBudgets(),
            savingsGoalRepository.observeGoals(),
            accountRepository.observeAccounts()
        ) { incomes, expenses, budgets, goals, accounts ->
            RepositoryInputs(incomes, expenses, budgets, goals, accounts)
        }

        val inputsFlow = combine(repositoryInputsFlow, userPreferences.safetyThreshold) { repositoryInputs, safetyThreshold ->
            Inputs(
                incomes = repositoryInputs.incomes,
                expenses = repositoryInputs.expenses,
                budgets = repositoryInputs.budgets,
                goals = repositoryInputs.goals,
                accounts = repositoryInputs.accounts,
                safetyThreshold = safetyThreshold
            )
        }

        return inputsFlow.combine(transactionRepository.observeAll()) { inputs, transactions ->
            buildPlan(inputs, transactions, month, today, realToday)
        }
    }

    private fun buildPlan(
        inputs: Inputs,
        transactions: List<Transaction>,
        month: YearMonth,
        today: LocalDate,
        realToday: LocalDate
    ): MonthPlan {
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
        val activeIncomes = inputs.incomes.filter { it.isActive }.map { it.toScheduledFlow() }
        val activeExpenses = inputs.expenses.filter { it.isActive }.map { it.toScheduledFlow() }

        // A "Et si...?" scenario asks about a date beyond realToday: the recorded account
        // balance only reflects transactions entered so far, so project every recurring
        // income/expense due between realToday and today (which can span into a later month -
        // FrequencyProjector.totalDueInRange handles that on its own) onto that balance before
        // handing it to BudgetEngine. Without this, e.g. "le mois prochain" would silently skip
        // this month's remaining salary/rent and start from today's raw balance instead.
        val projectionStart = realToday.plusDays(1)
        val projectedDelta = if (!projectionStart.isAfter(today)) {
            activeIncomes.sumOf { FrequencyProjector.totalDueInRange(it, projectionStart, today) } -
                activeExpenses.sumOf { FrequencyProjector.totalDueInRange(it, projectionStart, today) }
        } else 0.0
        val projectedAccountBalances = if (projectedDelta != 0.0 && accountBalances.isNotEmpty()) {
            listOf(accountBalances.sum() + projectedDelta)
        } else accountBalances

        return MonthPlan(
            month = month,
            today = today,
            incomes = activeIncomes,
            recurringExpenses = activeExpenses,
            variableBudgets = variableBudgetInputs,
            plannedMonthlySavings = plannedSavings,
            currentAccountBalances = projectedAccountBalances,
            safetyThreshold = inputs.safetyThreshold
        )
    }
}
