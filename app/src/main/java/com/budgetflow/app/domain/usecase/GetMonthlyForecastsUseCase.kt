package com.budgetflow.app.domain.usecase

import com.budgetflow.app.domain.repository.IncomeRepository
import com.budgetflow.app.domain.repository.RecurringExpenseRepository
import com.budgetflow.app.domain.repository.SavingsGoalRepository
import com.budgetflow.app.domain.repository.VariableBudgetRepository
import com.budgetflow.engine.BudgetEngine
import com.budgetflow.engine.model.MonthlyForecast
import com.budgetflow.engine.model.VariableBudgetInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth

/** Projects several months ahead assuming today's recurring incomes/expenses stay unchanged (spec section 12). */
class GetMonthlyForecastsUseCase(
    private val incomeRepository: IncomeRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val variableBudgetRepository: VariableBudgetRepository,
    private val savingsGoalRepository: SavingsGoalRepository
) {
    fun observe(startMonth: YearMonth, monthsCount: Int): Flow<List<MonthlyForecast>> = combine(
        incomeRepository.observeIncomes(),
        recurringExpenseRepository.observeExpenses(),
        variableBudgetRepository.observeBudgets(),
        savingsGoalRepository.observeGoals()
    ) { incomes, expenses, budgets, goals ->
        BudgetEngine.forecastMonths(
            startMonth = startMonth,
            monthsCount = monthsCount,
            incomes = incomes.filter { it.isActive }.map { it.toScheduledFlow() },
            recurringExpenses = expenses.filter { it.isActive }.map { it.toScheduledFlow() },
            variableBudgets = budgets.filter { it.isActive }.map { VariableBudgetInput(it.id, it.label, it.monthlyLimit, 0.0) },
            plannedMonthlySavings = goals.filter { it.isActive }.sumOf { it.monthlyContribution }
        )
    }
}
