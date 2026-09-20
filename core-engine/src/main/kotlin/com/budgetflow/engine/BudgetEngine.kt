package com.budgetflow.engine

import com.budgetflow.engine.model.CalendarOccurrence
import com.budgetflow.engine.model.FlowDirection
import com.budgetflow.engine.model.MonthPlan
import com.budgetflow.engine.model.MonthSummary
import com.budgetflow.engine.model.MonthlyForecast
import com.budgetflow.engine.model.ScheduledFlow
import com.budgetflow.engine.model.VariableBudgetInput
import java.time.LocalDate
import java.time.YearMonth

/**
 * BudgetFlow's financial calculation engine.
 *
 * This is deliberately UI-free and Android-free: it is pure functions over
 * plain data classes, so it can be exercised by fast JVM unit tests that
 * cover every edge case listed in the product spec (start/end of month,
 * 28/29/30/31-day months, future income/expense, no income, negative
 * balance, multiple accounts, yearly/weekly expenses...).
 */
object BudgetEngine {

    /** Computes every headline dashboard number for a single month. */
    fun summarizeMonth(plan: MonthPlan): MonthSummary {
        val monthStart = plan.monthStart
        val monthEnd = plan.monthEnd

        val totalIncome = plan.incomes.sumOf { FrequencyProjector.totalDueInRange(it, monthStart, monthEnd) }
        val totalFixedExpenses = plan.recurringExpenses.sumOf { FrequencyProjector.totalDueInRange(it, monthStart, monthEnd) }
        val totalVariableBudgetAllocated = plan.variableBudgets.sumOf(VariableBudgetInput::monthlyLimit)
        val totalVariableSpent = plan.variableBudgets.sumOf(VariableBudgetInput::spentSoFar)
        val plannedSavings = plan.plannedMonthlySavings

        val availableBudget = totalIncome - totalFixedExpenses - totalVariableBudgetAllocated - plannedSavings
        val remainingToSpend = availableBudget - totalVariableSpent

        val remainingDaysInMonth = remainingDaysInMonth(plan.today, monthStart, monthEnd)
        val dailyRecommendedBudget = if (remainingDaysInMonth > 0) remainingToSpend / remainingDaysInMonth else 0.0

        val futureWindowStart = maxOf(plan.today.plusDays(1), monthStart)
        val hasFutureWindow = !futureWindowStart.isAfter(monthEnd)
        val upcomingIncome = if (hasFutureWindow) {
            plan.incomes.sumOf { FrequencyProjector.totalDueInRange(it, futureWindowStart, monthEnd) }
        } else 0.0
        val upcomingFixedExpenses = if (hasFutureWindow) {
            plan.recurringExpenses.sumOf { FrequencyProjector.totalDueInRange(it, futureWindowStart, monthEnd) }
        } else 0.0

        val currentBankBalance = if (plan.currentAccountBalances.isEmpty()) null else plan.currentAccountBalances.sum()
        val reallyAvailableNow = currentBankBalance?.let { it + upcomingIncome - upcomingFixedExpenses }

        return MonthSummary(
            totalIncome = totalIncome,
            totalFixedExpenses = totalFixedExpenses,
            totalVariableBudgetAllocated = totalVariableBudgetAllocated,
            totalVariableSpent = totalVariableSpent,
            plannedSavings = plannedSavings,
            availableBudget = availableBudget,
            remainingToSpend = remainingToSpend,
            remainingDaysInMonth = remainingDaysInMonth,
            dailyRecommendedBudget = dailyRecommendedBudget,
            currentBankBalance = currentBankBalance,
            upcomingIncome = upcomingIncome,
            upcomingFixedExpenses = upcomingFixedExpenses,
            reallyAvailableNow = reallyAvailableNow
        )
    }

    /**
     * Number of days left in the month counting [today] itself, e.g. on the
     * last day of the month exactly 1 day remains. Returns 0 once [today] is
     * strictly after [monthEnd], and the full month length when [today] is
     * before [monthStart] (a not-yet-started month).
     */
    fun remainingDaysInMonth(today: LocalDate, monthStart: LocalDate, monthEnd: LocalDate): Int = when {
        today.isBefore(monthStart) -> (monthEnd.toEpochDay() - monthStart.toEpochDay() + 1).toInt()
        today.isAfter(monthEnd) -> 0
        else -> (monthEnd.toEpochDay() - today.toEpochDay() + 1).toInt()
    }

    /** Every dated income/expense occurrence between [rangeStart] and [rangeEnd], sorted by date. */
    fun calendarOccurrences(
        incomes: List<ScheduledFlow>,
        expenses: List<ScheduledFlow>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate
    ): List<CalendarOccurrence> {
        val incomeOccurrences = incomes.flatMap { flow ->
            FrequencyProjector.occurrencesInRange(flow, rangeStart, rangeEnd).map { date ->
                CalendarOccurrence(date, flow.id, flow.label, flow.amount, FlowDirection.INCOME)
            }
        }
        val expenseOccurrences = expenses.flatMap { flow ->
            FrequencyProjector.occurrencesInRange(flow, rangeStart, rangeEnd).map { date ->
                CalendarOccurrence(date, flow.id, flow.label, flow.amount, FlowDirection.EXPENSE)
            }
        }
        return (incomeOccurrences + expenseOccurrences).sortedWith(compareBy({ it.date }, { it.label }))
    }

    /**
     * Projects [monthsCount] consecutive months starting at [startMonth],
     * assuming the same recurring incomes/expenses/envelopes stay unchanged.
     * Used for the "future months" screen (section 12 of the spec).
     */
    fun forecastMonths(
        startMonth: YearMonth,
        monthsCount: Int,
        incomes: List<ScheduledFlow>,
        recurringExpenses: List<ScheduledFlow>,
        variableBudgets: List<VariableBudgetInput>,
        plannedMonthlySavings: Double
    ): List<MonthlyForecast> {
        require(monthsCount >= 0) { "monthsCount must not be negative" }
        val totalVariableBudgetAllocated = variableBudgets.sumOf(VariableBudgetInput::monthlyLimit)

        return (0 until monthsCount).map { offset ->
            val month = startMonth.plusMonths(offset.toLong())
            val monthStart = month.atDay(1)
            val monthEnd = month.atEndOfMonth()

            val totalIncome = incomes.sumOf { FrequencyProjector.totalDueInRange(it, monthStart, monthEnd) }
            val totalFixedExpenses = recurringExpenses.sumOf { FrequencyProjector.totalDueInRange(it, monthStart, monthEnd) }
            val availableBudget = totalIncome - totalFixedExpenses - totalVariableBudgetAllocated - plannedMonthlySavings

            MonthlyForecast(
                month = month,
                totalIncome = totalIncome,
                totalFixedExpenses = totalFixedExpenses,
                totalVariableBudgetAllocated = totalVariableBudgetAllocated,
                plannedSavings = plannedMonthlySavings,
                availableBudget = availableBudget
            )
        }
    }
}
