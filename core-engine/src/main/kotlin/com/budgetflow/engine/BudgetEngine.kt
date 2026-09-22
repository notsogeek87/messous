package com.budgetflow.engine

import com.budgetflow.engine.model.CalendarOccurrence
import com.budgetflow.engine.model.DailyProjection
import com.budgetflow.engine.model.ExpenseSimulation
import com.budgetflow.engine.model.FlowDirection
import com.budgetflow.engine.model.FreedomState
import com.budgetflow.engine.model.MonthPlan
import com.budgetflow.engine.model.MonthSummary
import com.budgetflow.engine.model.MonthlyForecast
import com.budgetflow.engine.model.ScheduledFlow
import com.budgetflow.engine.model.VariableBudgetInput
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

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

        val remainingToSpendToday = remainingToSpendAsOf(plan, plan.today)
        val remainingToSpendTodayState = freedomStateFor(remainingToSpendToday, plan.safetyThreshold)

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

        // Money already spoken for beyond fixed expenses: envelopes not yet spent, savings not yet set aside.
        val remainingVariableBudget = (totalVariableBudgetAllocated - totalVariableSpent).coerceAtLeast(0.0)
        val remainingPlannedSavings = plannedSavings.coerceAtLeast(0.0)

        val freeMoney = reallyAvailableNow?.let { it - remainingVariableBudget - remainingPlannedSavings }
        val safetyMargin = freeMoney?.let { it - plan.safetyThreshold }
        val freedomState = freedomState(safetyMargin, plan.safetyThreshold)
        val freedomPerDay = if (freeMoney != null && remainingDaysInMonth > 0) freeMoney / remainingDaysInMonth else null

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
            remainingToSpendToday = remainingToSpendToday,
            remainingToSpendTodayState = remainingToSpendTodayState,
            currentBankBalance = currentBankBalance,
            upcomingIncome = upcomingIncome,
            upcomingFixedExpenses = upcomingFixedExpenses,
            reallyAvailableNow = reallyAvailableNow,
            remainingVariableBudget = remainingVariableBudget,
            remainingPlannedSavings = remainingPlannedSavings,
            freeMoney = freeMoney,
            safetyThreshold = plan.safetyThreshold,
            safetyMargin = safetyMargin,
            freedomState = freedomState,
            freedomPerDay = freedomPerDay
        )
    }

    /**
     * [remainingToSpend] but counting only the income/fixed expenses actually due by [asOfDate] -
     * the same plan-based projection [summarizeMonth] uses for "today" ([MonthSummary.remainingToSpendToday]),
     * generalized to any day within [plan]'s month so an interactive gauge can preview any day the
     * user picks, independently of the real "today". Before the month starts nothing has landed
     * yet (0.0); on or after its last day, every flow has (same as [remainingToSpend]).
     */
    fun remainingToSpendAsOf(plan: MonthPlan, asOfDate: LocalDate): Double {
        val monthStart = plan.monthStart
        val monthEnd = plan.monthEnd
        if (asOfDate.isBefore(monthStart)) return 0.0
        val clamped = if (asOfDate.isAfter(monthEnd)) monthEnd else asOfDate

        val incomeSoFar = plan.incomes.sumOf { FrequencyProjector.totalDueInRange(it, monthStart, clamped) }
        val fixedExpensesSoFar = plan.recurringExpenses.sumOf { FrequencyProjector.totalDueInRange(it, monthStart, clamped) }
        val totalVariableBudgetAllocated = plan.variableBudgets.sumOf(VariableBudgetInput::monthlyLimit)
        val totalVariableSpent = plan.variableBudgets.sumOf(VariableBudgetInput::spentSoFar)
        return incomeSoFar - fixedExpensesSoFar - totalVariableBudgetAllocated - plan.plannedMonthlySavings - totalVariableSpent
    }

    /** [freedomState] applied to an already-computed amount, e.g. [remainingToSpendAsOf] on a day other than today. */
    fun freedomStateFor(amount: Double, safetyThreshold: Double): FreedomState = freedomState(amount - safetyThreshold, safetyThreshold)

    /**
     * [FreedomState] is deliberately relative to the user's own threshold, never absolute:
     * - no margin data (no accounts) -> COMFORT, so an empty ledger never reads as an alarm.
     * - a negative margin (free money already under the threshold) -> ALERT.
     * - a positive margin smaller than the threshold itself (less than one more "cushion's worth"
     *   of buffer beyond it) -> CAUTION.
     * - anything else -> COMFORT.
     */
    private fun freedomState(safetyMargin: Double?, safetyThreshold: Double): FreedomState = when {
        safetyMargin == null -> FreedomState.COMFORT
        safetyMargin < 0 -> FreedomState.ALERT
        safetyThreshold > 0 && safetyMargin < safetyThreshold -> FreedomState.CAUTION
        else -> FreedomState.COMFORT
    }

    /**
     * Simulates spending [amount] right now, without mutating anything (spec section 23).
     * Moves the account balance total, exactly as a real expense recorded today would (for
     * users who keep one up to date) - but the primary, account-independent figure is
     * [MonthSummary.remainingToSpend] ("reste à vivre" = revenus - dépenses - budgets - épargne),
     * which the account-balance copy alone never touches. Any expense, planned or not, reduces
     * what is left to live on by its own amount, so that quantity (and everything derived from
     * it: [MonthSummary.remainingToSpendToday], [MonthSummary.dailyRecommendedBudget],
     * [MonthSummary.remainingToSpendTodayState]) is adjusted directly by [amount] here.
     */
    fun simulateExpense(plan: MonthPlan, amount: Double): ExpenseSimulation {
        val before = summarizeMonth(plan)
        val accountAdjustedPlan = plan.copy(
            currentAccountBalances = if (plan.currentAccountBalances.isEmpty()) {
                emptyList()
            } else {
                listOf(plan.currentAccountBalances.sum() - amount)
            }
        )
        val afterAccountBased = summarizeMonth(accountAdjustedPlan)

        val remainingToSpend = afterAccountBased.remainingToSpend - amount
        val remainingToSpendToday = afterAccountBased.remainingToSpendToday - amount
        val dailyRecommendedBudget = if (afterAccountBased.remainingDaysInMonth > 0) {
            remainingToSpend / afterAccountBased.remainingDaysInMonth
        } else {
            0.0
        }
        val remainingToSpendTodayState = freedomStateFor(remainingToSpendToday, plan.safetyThreshold)

        val after = afterAccountBased.copy(
            remainingToSpend = remainingToSpend,
            remainingToSpendToday = remainingToSpendToday,
            dailyRecommendedBudget = dailyRecommendedBudget,
            remainingToSpendTodayState = remainingToSpendTodayState
        )
        return ExpenseSimulation(amount = amount, before = before, after = after)
    }

    /**
     * How many days a savings goal would slip if [amount] were diverted away from it,
     * assuming its current [monthlyContribution] rate. Null when the goal has no active
     * contribution rate to measure a delay against.
     */
    fun goalDelayDays(amount: Double, monthlyContribution: Double): Int? {
        if (monthlyContribution <= 0.0 || amount <= 0.0) return null
        val dailyContribution = monthlyContribution / 30.0
        return (amount / dailyContribution).roundToInt()
    }

    /**
     * Day-by-day running balance from [MonthPlan.today] to [MonthPlan.monthEnd] (spec sections
     * 8 & 9, "Mon futur" / "voyage dans le temps"). The first entry is always today itself, with
     * no occurrences attached (today's balance already reflects whatever happened today).
     * Empty when there is no account balance to project from.
     */
    fun projectDailyBalances(plan: MonthPlan): List<DailyProjection> {
        if (plan.currentAccountBalances.isEmpty()) return emptyList()
        val today = plan.today
        val monthEnd = plan.monthEnd
        if (today.isAfter(monthEnd)) return emptyList()

        val futureStart = today.plusDays(1)
        val occurrencesByDate = if (!futureStart.isAfter(monthEnd)) {
            calendarOccurrences(plan.incomes, plan.recurringExpenses, futureStart, monthEnd).groupBy { it.date }
        } else {
            emptyMap()
        }

        var runningBalance = plan.currentAccountBalances.sum()
        val projections = mutableListOf(DailyProjection(today, runningBalance, emptyList()))

        var date = futureStart
        while (!date.isAfter(monthEnd)) {
            val todaysOccurrences = occurrencesByDate[date].orEmpty()
            val net = todaysOccurrences.sumOf { if (it.direction == FlowDirection.INCOME) it.amount else -it.amount }
            runningBalance += net
            projections += DailyProjection(date, runningBalance, todaysOccurrences)
            date = date.plusDays(1)
        }
        return projections
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
