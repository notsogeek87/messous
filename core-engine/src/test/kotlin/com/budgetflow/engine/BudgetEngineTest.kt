package com.budgetflow.engine

import com.budgetflow.engine.model.FlowDirection
import com.budgetflow.engine.model.Frequency
import com.budgetflow.engine.model.MonthPlan
import com.budgetflow.engine.model.ScheduledFlow
import com.budgetflow.engine.model.VariableBudgetInput
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BudgetEngineTest {

    private fun monthlyIncome(day: Int, amount: Double, label: String = "salaire") = ScheduledFlow(
        id = 1, label = label, amount = amount, frequency = Frequency.MONTHLY, dayOfMonth = day
    )

    private fun monthlyExpense(day: Int, amount: Double, label: String) = ScheduledFlow(
        id = 2, label = label, amount = amount, frequency = Frequency.MONTHLY, dayOfMonth = day
    )

    // --- Section 5 dashboard example -----------------------------------------------------

    @Test
    fun `available budget and remaining to spend match the spec example`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 10),
            today = LocalDate.of(2026, 10, 1),
            incomes = listOf(monthlyIncome(day = 1, amount = 4000.0)),
            recurringExpenses = listOf(monthlyExpense(day = 5, amount = 1650.0, label = "fixed")),
            variableBudgets = listOf(VariableBudgetInput(id = 1, label = "Courses", monthlyLimit = 1050.0, spentSoFar = 0.0)),
            plannedMonthlySavings = 500.0
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(800.0, summary.availableBudget, 0.0001)
        assertEquals(800.0, summary.remainingToSpend, 0.0001)
    }

    @Test
    fun `daily recommended budget divides remaining to spend by remaining days`() {
        // October 2026 has 31 days; on the 22nd, 10 days remain (22..31 inclusive).
        val plan = MonthPlan(
            month = YearMonth.of(2026, 10),
            today = LocalDate.of(2026, 10, 22),
            incomes = listOf(monthlyIncome(day = 1, amount = 4000.0)),
            recurringExpenses = listOf(monthlyExpense(day = 5, amount = 1650.0, label = "fixed")),
            variableBudgets = listOf(VariableBudgetInput(id = 1, label = "Courses", monthlyLimit = 1050.0, spentSoFar = 0.0)),
            plannedMonthlySavings = 500.0
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(10, summary.remainingDaysInMonth)
        assertEquals(80.0, summary.dailyRecommendedBudget, 0.0001)
    }

    @Test
    fun `remaining to spend decreases as variable envelopes are actually spent`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 10),
            today = LocalDate.of(2026, 10, 1),
            incomes = listOf(monthlyIncome(day = 1, amount = 4000.0)),
            recurringExpenses = listOf(monthlyExpense(day = 5, amount = 1650.0, label = "fixed")),
            variableBudgets = listOf(VariableBudgetInput(id = 1, label = "Courses", monthlyLimit = 500.0, spentSoFar = 227.0)),
            plannedMonthlySavings = 0.0
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        // availableBudget = 4000 - 1650 - 500 - 0 = 1850 ; remainingToSpend = 1850 - 227 = 1623
        assertEquals(1850.0, summary.availableBudget, 0.0001)
        assertEquals(1623.0, summary.remainingToSpend, 0.0001)
    }

    // --- Section 5 "really available" example ---------------------------------------------

    @Test
    fun `really available now subtracts only upcoming fixed expenses from the bank balance`() {
        val today = LocalDate.of(2026, 10, 18)
        val plan = MonthPlan(
            month = YearMonth.of(2026, 10),
            today = today,
            incomes = listOf(monthlyIncome(day = 1, amount = 4000.0)), // already received, in the past
            recurringExpenses = listOf(
                monthlyExpense(day = 30, amount = 800.0, label = "Credit"),
                monthlyExpense(day = 25, amount = 100.0, label = "Assurance"),
                monthlyExpense(day = 20, amount = 20.0, label = "Abonnement")
            ),
            variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0,
            currentAccountBalances = listOf(1500.0)
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(1500.0, summary.currentBankBalance)
        assertEquals(920.0, summary.upcomingFixedExpenses, 0.0001)
        assertEquals(580.0, summary.reallyAvailableNow!!, 0.0001)
    }

    @Test
    fun `future income is added to really available now`() {
        val today = LocalDate.of(2026, 10, 10)
        val plan = MonthPlan(
            month = YearMonth.of(2026, 10),
            today = today,
            incomes = listOf(monthlyIncome(day = 28, amount = 4000.0)), // salary not yet paid
            recurringExpenses = listOf(monthlyExpense(day = 30, amount = 800.0, label = "Credit")),
            variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0,
            currentAccountBalances = listOf(100.0)
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(4000.0, summary.upcomingIncome, 0.0001)
        assertEquals(800.0, summary.upcomingFixedExpenses, 0.0001)
        assertEquals(3300.0, summary.reallyAvailableNow!!, 0.0001) // 100 + 4000 - 800
    }

    @Test
    fun `expense already past today is not counted as upcoming`() {
        val today = LocalDate.of(2026, 10, 15)
        val plan = MonthPlan(
            month = YearMonth.of(2026, 10),
            today = today,
            incomes = emptyList(),
            recurringExpenses = listOf(monthlyExpense(day = 5, amount = 200.0, label = "already paid")),
            variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0,
            currentAccountBalances = listOf(1000.0)
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(0.0, summary.upcomingFixedExpenses, 0.0001)
        assertEquals(1000.0, summary.reallyAvailableNow!!, 0.0001)
    }

    // --- Start / end of month, and month lengths ------------------------------------------

    @Test
    fun `start of month counts the full month as remaining days`() {
        val plan = basePlan(YearMonth.of(2026, 4), today = LocalDate.of(2026, 4, 1))
        assertEquals(30, BudgetEngine.summarizeMonth(plan).remainingDaysInMonth)
    }

    @Test
    fun `end of month counts exactly one remaining day`() {
        val plan = basePlan(YearMonth.of(2026, 4), today = LocalDate.of(2026, 4, 30))
        assertEquals(1, BudgetEngine.summarizeMonth(plan).remainingDaysInMonth)
    }

    @Test
    fun `28-day february is handled`() {
        val plan = basePlan(YearMonth.of(2026, 2), today = LocalDate.of(2026, 2, 1))
        assertEquals(28, BudgetEngine.summarizeMonth(plan).remainingDaysInMonth)
    }

    @Test
    fun `29-day leap february is handled`() {
        val plan = basePlan(YearMonth.of(2028, 2), today = LocalDate.of(2028, 2, 1))
        assertEquals(29, BudgetEngine.summarizeMonth(plan).remainingDaysInMonth)
    }

    @Test
    fun `30-day month is handled`() {
        val plan = basePlan(YearMonth.of(2026, 4), today = LocalDate.of(2026, 4, 1))
        assertEquals(30, BudgetEngine.summarizeMonth(plan).remainingDaysInMonth)
    }

    @Test
    fun `31-day month is handled`() {
        val plan = basePlan(YearMonth.of(2026, 1), today = LocalDate.of(2026, 1, 1))
        assertEquals(31, BudgetEngine.summarizeMonth(plan).remainingDaysInMonth)
    }

    @Test
    fun `today before the evaluated month returns the full month length`() {
        val plan = basePlan(YearMonth.of(2026, 12), today = LocalDate.of(2026, 9, 1))
        assertEquals(31, BudgetEngine.summarizeMonth(plan).remainingDaysInMonth)
    }

    @Test
    fun `today after the evaluated month returns zero remaining days and zero daily budget`() {
        val plan = basePlan(YearMonth.of(2026, 1), today = LocalDate.of(2026, 3, 1))
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(0, summary.remainingDaysInMonth)
        assertEquals(0.0, summary.dailyRecommendedBudget)
    }

    // --- No income / negative balance / multiple accounts ---------------------------------

    @Test
    fun `a month with no income yields a negative available budget without crashing`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 10),
            today = LocalDate.of(2026, 10, 1),
            incomes = emptyList(),
            recurringExpenses = listOf(monthlyExpense(day = 5, amount = 300.0, label = "rent")),
            variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(0.0, summary.totalIncome)
        assertEquals(-300.0, summary.availableBudget, 0.0001)
    }

    @Test
    fun `a negative account balance flows through really available now`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 10),
            today = LocalDate.of(2026, 10, 20),
            incomes = emptyList(),
            recurringExpenses = emptyList(),
            variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0,
            currentAccountBalances = listOf(-200.0)
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(-200.0, summary.currentBankBalance)
        assertEquals(-200.0, summary.reallyAvailableNow!!, 0.0001)
    }

    @Test
    fun `multiple accounts are summed into the current bank balance`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 10),
            today = LocalDate.of(2026, 10, 20),
            incomes = emptyList(),
            recurringExpenses = emptyList(),
            variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0,
            currentAccountBalances = listOf(1000.0, 500.0, -50.0)
        )
        assertEquals(1450.0, BudgetEngine.summarizeMonth(plan).currentBankBalance)
    }

    @Test
    fun `no accounts means really available now is null, unlike a zero balance`() {
        val plan = basePlan(YearMonth.of(2026, 10), today = LocalDate.of(2026, 10, 20))
        val summary = BudgetEngine.summarizeMonth(plan)
        assertNull(summary.currentBankBalance)
        assertNull(summary.reallyAvailableNow)
    }

    // --- Yearly / weekly expenses ------------------------------------------------------------

    @Test
    fun `yearly expense only counts in the month it falls due`() {
        val yearlyTax = ScheduledFlow(id = 9, label = "impots", amount = 1200.0, frequency = Frequency.YEARLY, dayOfMonth = 15, monthOfYear = 9)
        val septemberPlan = basePlan(YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 1), extraExpenses = listOf(yearlyTax))
        val octoberPlan = basePlan(YearMonth.of(2026, 10), today = LocalDate.of(2026, 10, 1), extraExpenses = listOf(yearlyTax))

        assertEquals(1200.0, BudgetEngine.summarizeMonth(septemberPlan).totalFixedExpenses, 0.0001)
        assertEquals(0.0, BudgetEngine.summarizeMonth(octoberPlan).totalFixedExpenses, 0.0001)
    }

    @Test
    fun `weekly expense is counted once per occurrence in the month`() {
        val weeklyGroceries = ScheduledFlow(id = 10, label = "essence", amount = 40.0, frequency = Frequency.WEEKLY, dayOfWeek = DayOfWeek.FRIDAY)
        // September 2026 has 5 Fridays: 4, 11, 18, 25 -> wait check: Sept 1 2026 is a Tuesday, Fridays: 4,11,18,25 = 4 occurrences.
        val plan = basePlan(YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 1), extraExpenses = listOf(weeklyGroceries))
        assertEquals(160.0, BudgetEngine.summarizeMonth(plan).totalFixedExpenses, 0.0001)
    }

    // --- Calendar view (section 6) ---------------------------------------------------------

    @Test
    fun `calendar occurrences are sorted by date across incomes and expenses`() {
        val incomes = listOf(monthlyIncome(day = 1, amount = 4000.0, label = "Salaire"))
        val expenses = listOf(
            monthlyExpense(day = 30, amount = 800.0, label = "Credit"),
            monthlyExpense(day = 25, amount = 120.0, label = "Assurance"),
            monthlyExpense(day = 20, amount = 80.0, label = "Courses")
        )
        val occurrences = BudgetEngine.calendarOccurrences(
            incomes, expenses, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)
        )
        assertEquals(
            listOf("Salaire" to FlowDirection.INCOME, "Courses" to FlowDirection.EXPENSE, "Assurance" to FlowDirection.EXPENSE, "Credit" to FlowDirection.EXPENSE),
            occurrences.map { it.label to it.direction }
        )
        assertEquals(LocalDate.of(2026, 9, 20), occurrences[1].date)
        assertEquals(LocalDate.of(2026, 9, 25), occurrences[2].date)
        assertEquals(LocalDate.of(2026, 9, 30), occurrences[3].date)
    }

    // --- Future months forecast (section 12) ------------------------------------------------

    @Test
    fun `forecast months project stable recurring flows unchanged`() {
        val incomes = listOf(monthlyIncome(day = 1, amount = 4000.0, label = "Salaire"))
        val expenses = listOf(monthlyExpense(day = 5, amount = 1200.0, label = "Credit"))
        val forecasts = BudgetEngine.forecastMonths(
            startMonth = YearMonth.of(2026, 9),
            monthsCount = 4,
            incomes = incomes,
            recurringExpenses = expenses,
            variableBudgets = listOf(VariableBudgetInput(1, "Courses", 500.0, 0.0)),
            plannedMonthlySavings = 300.0
        )
        assertEquals(4, forecasts.size)
        assertEquals(
            listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11), YearMonth.of(2026, 12)),
            forecasts.map { it.month }
        )
        forecasts.forEach {
            assertEquals(4000.0, it.totalIncome, 0.0001)
            assertEquals(1200.0, it.totalFixedExpenses, 0.0001)
            assertEquals(2000.0, it.availableBudget, 0.0001) // 4000 - 1200 - 500 - 300
        }
    }

    @Test
    fun `forecast of zero months returns an empty list`() {
        val forecasts = BudgetEngine.forecastMonths(
            startMonth = YearMonth.of(2026, 9),
            monthsCount = 0,
            incomes = emptyList(),
            recurringExpenses = emptyList(),
            variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0
        )
        assertEquals(emptyList(), forecasts)
    }

    // --- helpers -----------------------------------------------------------------------------

    private fun basePlan(month: YearMonth, today: LocalDate, extraExpenses: List<ScheduledFlow> = emptyList()) = MonthPlan(
        month = month,
        today = today,
        incomes = emptyList(),
        recurringExpenses = extraExpenses,
        variableBudgets = emptyList(),
        plannedMonthlySavings = 0.0
    )
}
