package com.budgetflow.app.domain.usecase

import com.budgetflow.engine.BudgetEngine
import com.budgetflow.engine.model.ExpenseSimulation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

/**
 * Powers "Et si...?" (spec sections 10, 11, 16): shows what a hypothetical expense would do
 * to the user's free money and daily freedom, without ever writing a transaction (section 23).
 */
class SimulateExpenseUseCase(private val monthPlanSource: GetDashboardForMonthUseCase) {

    /** Simulates spending [amount] right now, in the current month. */
    fun observeNow(amount: Double, today: LocalDate = LocalDate.now()): Flow<ExpenseSimulation> =
        monthPlanSource.observePlan(YearMonth.from(today), today).map { plan ->
            BudgetEngine.simulateExpense(plan, amount)
        }

    /** Simulates spending [amount] on an arbitrary future [date] - used for the "dans 15 jours" / "le mois prochain" scenarios. */
    fun observeOn(amount: Double, date: LocalDate): Flow<ExpenseSimulation> =
        monthPlanSource.observePlan(YearMonth.from(date), date).map { plan ->
            BudgetEngine.simulateExpense(plan, amount)
        }
}
