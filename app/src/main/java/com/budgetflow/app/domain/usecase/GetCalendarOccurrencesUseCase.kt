package com.budgetflow.app.domain.usecase

import com.budgetflow.app.domain.repository.IncomeRepository
import com.budgetflow.app.domain.repository.RecurringExpenseRepository
import com.budgetflow.engine.BudgetEngine
import com.budgetflow.engine.model.CalendarOccurrence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** Powers the "future expenses/incomes" calendar view (spec section 6). */
class GetCalendarOccurrencesUseCase(
    private val incomeRepository: IncomeRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository
) {
    fun observe(rangeStart: LocalDate, rangeEnd: LocalDate): Flow<List<CalendarOccurrence>> =
        combine(incomeRepository.observeIncomes(), recurringExpenseRepository.observeExpenses()) { incomes, expenses ->
            BudgetEngine.calendarOccurrences(
                incomes = incomes.filter { it.isActive }.map { it.toScheduledFlow() },
                expenses = expenses.filter { it.isActive }.map { it.toScheduledFlow() },
                rangeStart = rangeStart,
                rangeEnd = rangeEnd
            )
        }
}
