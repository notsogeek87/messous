package com.budgetflow.engine

import com.budgetflow.engine.model.FlowDirection
import com.budgetflow.engine.model.Frequency
import com.budgetflow.engine.model.FreedomState
import com.budgetflow.engine.model.MonthPlan
import com.budgetflow.engine.model.ScheduledFlow
import com.budgetflow.engine.model.VariableBudgetInput
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    // --- Reste à vivre au jour le jour, selon la date des flux -----------------------------

    @Test
    fun `remaining to spend today only counts income and fixed expenses due so far`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 10),
            today = LocalDate.of(2026, 10, 10),
            incomes = listOf(monthlyIncome(day = 28, amount = 4000.0)), // not paid yet
            recurringExpenses = listOf(monthlyExpense(day = 5, amount = 1650.0, label = "fixed")), // already paid
            variableBudgets = listOf(VariableBudgetInput(id = 1, label = "Courses", monthlyLimit = 1050.0, spentSoFar = 0.0)),
            plannedMonthlySavings = 500.0
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        // Nothing received yet, the fixed expense already fell due, envelope/savings reserved from day 1:
        // 0 - 1650 - 1050 - 500 = -3200, very different from the whole-month remainingToSpend of 800.
        assertEquals(800.0, summary.remainingToSpend, 0.0001)
        assertEquals(-3200.0, summary.remainingToSpendToday, 0.0001)
    }

    @Test
    fun `remaining to spend today matches remaining to spend once every flow has landed`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 10),
            today = LocalDate.of(2026, 10, 31),
            incomes = listOf(monthlyIncome(day = 1, amount = 4000.0)),
            recurringExpenses = listOf(monthlyExpense(day = 5, amount = 1650.0, label = "fixed")),
            variableBudgets = listOf(VariableBudgetInput(id = 1, label = "Courses", monthlyLimit = 1050.0, spentSoFar = 0.0)),
            plannedMonthlySavings = 500.0
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(summary.remainingToSpend, summary.remainingToSpendToday, 0.0001)
    }

    @Test
    fun `remaining to spend today is zero before the month starts, like an untouched month`() {
        val plan = basePlan(YearMonth.of(2026, 12), today = LocalDate.of(2026, 9, 1)).copy(
            incomes = listOf(monthlyIncome(day = 1, amount = 4000.0))
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(0.0, summary.remainingToSpendToday, 0.0001)
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

    // --- Free money (spec section 3) --------------------------------------------------------

    @Test
    fun `free money subtracts unspent envelopes and unsaved savings from really available now`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9),
            today = LocalDate.of(2026, 9, 15),
            incomes = emptyList(),
            recurringExpenses = emptyList(),
            variableBudgets = listOf(VariableBudgetInput(id = 1, label = "Courses", monthlyLimit = 500.0, spentSoFar = 200.0)),
            plannedMonthlySavings = 100.0,
            currentAccountBalances = listOf(1000.0)
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(1000.0, summary.reallyAvailableNow!!, 0.0001)
        assertEquals(300.0, summary.remainingVariableBudget, 0.0001)
        assertEquals(100.0, summary.remainingPlannedSavings, 0.0001)
        assertEquals(600.0, summary.freeMoney!!, 0.0001) // 1000 - 300 - 100
    }

    @Test
    fun `an already overspent envelope reserves nothing further from free money`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9),
            today = LocalDate.of(2026, 9, 15),
            incomes = emptyList(),
            recurringExpenses = emptyList(),
            variableBudgets = listOf(VariableBudgetInput(id = 1, label = "Courses", monthlyLimit = 200.0, spentSoFar = 350.0)),
            plannedMonthlySavings = 0.0,
            currentAccountBalances = listOf(1000.0)
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(0.0, summary.remainingVariableBudget, 0.0001)
        assertEquals(1000.0, summary.freeMoney!!, 0.0001)
    }

    @Test
    fun `free money is null exactly when there are no accounts, like really available now`() {
        val plan = basePlan(YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 15))
        val summary = BudgetEngine.summarizeMonth(plan)
        assertNull(summary.reallyAvailableNow)
        assertNull(summary.freeMoney)
        assertNull(summary.safetyMargin)
        assertNull(summary.freedomPerDay)
    }

    // --- Daily freedom (spec section 4) -----------------------------------------------------

    @Test
    fun `daily freedom divides free money by remaining days, ignoring future expenses that already happened`() {
        // September 2026 has 30 days; the 21st leaves exactly 10 days (21..30 inclusive).
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9),
            today = LocalDate.of(2026, 9, 21),
            incomes = emptyList(),
            recurringExpenses = emptyList(),
            variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0,
            currentAccountBalances = listOf(1247.0)
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(1247.0, summary.freeMoney!!, 0.0001)
        assertEquals(10, summary.remainingDaysInMonth)
        assertEquals(124.7, summary.freedomPerDay!!, 0.0001)
    }

    @Test
    fun `daily freedom shrinks once a known future expense is taken into account`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9),
            today = LocalDate.of(2026, 9, 21),
            incomes = emptyList(),
            recurringExpenses = listOf(monthlyExpense(day = 25, amount = 827.0, label = "Grosse dépense")),
            variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0,
            currentAccountBalances = listOf(1247.0)
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(420.0, summary.freeMoney!!, 0.0001) // 1247 - 827
        assertEquals(42.0, summary.freedomPerDay!!, 0.0001)
    }

    // --- Safety threshold and freedom state (spec sections 5 & 6) --------------------------

    @Test
    fun `margin below the threshold is flagged as caution, the spec's headline example`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9),
            today = LocalDate.of(2026, 9, 21),
            incomes = emptyList(),
            recurringExpenses = emptyList(),
            variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0,
            currentAccountBalances = listOf(1247.0),
            safetyThreshold = 1000.0
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(247.0, summary.safetyMargin!!, 0.0001)
        assertEquals(FreedomState.CAUTION, summary.freedomState)
    }

    @Test
    fun `margin comfortably above the threshold reads as comfort`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9),
            today = LocalDate.of(2026, 9, 21),
            incomes = emptyList(),
            recurringExpenses = emptyList(),
            variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0,
            currentAccountBalances = listOf(3000.0),
            safetyThreshold = 500.0
        )
        assertEquals(FreedomState.COMFORT, BudgetEngine.summarizeMonth(plan).freedomState)
    }

    @Test
    fun `free money under the threshold reads as alert`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9),
            today = LocalDate.of(2026, 9, 21),
            incomes = emptyList(),
            recurringExpenses = emptyList(),
            variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0,
            currentAccountBalances = listOf(800.0),
            safetyThreshold = 1000.0
        )
        val summary = BudgetEngine.summarizeMonth(plan)
        assertEquals(-200.0, summary.safetyMargin!!, 0.0001)
        assertEquals(FreedomState.ALERT, summary.freedomState)
    }

    @Test
    fun `with no threshold set, only negative free money triggers alert`() {
        val comfortable = MonthPlan(
            month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 21),
            incomes = emptyList(), recurringExpenses = emptyList(), variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0, currentAccountBalances = listOf(50.0)
        )
        val overdrawn = comfortable.copy(currentAccountBalances = listOf(-50.0))
        assertEquals(FreedomState.COMFORT, BudgetEngine.summarizeMonth(comfortable).freedomState)
        assertEquals(FreedomState.ALERT, BudgetEngine.summarizeMonth(overdrawn).freedomState)
    }

    // --- "Et si...?" expense simulation (spec sections 10, 11, 16, 23) ----------------------

    @Test
    fun `simulating an expense reduces free money by exactly the amount, other things equal`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 21),
            incomes = emptyList(), recurringExpenses = emptyList(), variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0, currentAccountBalances = listOf(1000.0)
        )
        val simulation = BudgetEngine.simulateExpense(plan, amount = 300.0)
        assertEquals(1000.0, simulation.before.freeMoney!!, 0.0001)
        assertEquals(700.0, simulation.after.freeMoney!!, 0.0001)
        assertEquals(300.0, simulation.freeMoneyDelta!!, 0.0001)
        // "-11 € de liberté quotidienne" style figure: 300 spread over the 10 remaining days.
        assertEquals(30.0, simulation.freedomPerDayDelta!!, 0.0001)
    }

    @Test
    fun `an expense that would breach the safety threshold is flagged, not judged`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 21),
            incomes = emptyList(), recurringExpenses = emptyList(), variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0, currentAccountBalances = listOf(1000.0), safetyThreshold = 900.0
        )
        val simulation = BudgetEngine.simulateExpense(plan, amount = 200.0)
        assertEquals(FreedomState.ALERT, simulation.after.freedomState)
        assertTrue(simulation.wouldBreachSafetyThreshold)
        assertEquals(100.0, simulation.amountUnderThreshold!!, 0.0001)
    }

    @Test
    fun `an expense that stays above the safety threshold is not flagged`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 21),
            incomes = emptyList(), recurringExpenses = emptyList(), variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0, currentAccountBalances = listOf(1000.0), safetyThreshold = 200.0
        )
        val simulation = BudgetEngine.simulateExpense(plan, amount = 300.0)
        assertFalse(simulation.wouldBreachSafetyThreshold)
        assertNull(simulation.amountUnderThreshold)
    }

    @Test
    fun `simulating an expense never mutates the original plan`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 21),
            incomes = emptyList(), recurringExpenses = emptyList(), variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0, currentAccountBalances = listOf(500.0, 500.0)
        )
        BudgetEngine.simulateExpense(plan, amount = 200.0)
        assertEquals(listOf(500.0, 500.0), plan.currentAccountBalances)
        assertEquals(1000.0, BudgetEngine.summarizeMonth(plan).freeMoney!!, 0.0001)
    }

    @Test
    fun `simulating an expense collapses multiple accounts into their total`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 21),
            incomes = emptyList(), recurringExpenses = emptyList(), variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0, currentAccountBalances = listOf(500.0, 500.0)
        )
        val simulation = BudgetEngine.simulateExpense(plan, amount = 200.0)
        assertEquals(800.0, simulation.after.currentBankBalance!!, 0.0001)
    }

    // --- "Et si...?" plan-based reste à vivre - works with no account configured ------------

    @Test
    fun `simulating an expense reduces reste a vivre even with no account configured`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 22),
            incomes = listOf(monthlyIncome(day = 1, amount = 3300.0)),
            recurringExpenses = emptyList(), variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0, currentAccountBalances = emptyList()
        )
        val simulation = BudgetEngine.simulateExpense(plan, amount = 659.0)
        assertEquals(3300.0, simulation.before.remainingToSpend, 0.0001)
        assertEquals(2641.0, simulation.after.remainingToSpend, 0.0001)
        assertEquals(659.0, simulation.remainingToSpendDelta, 0.0001)
        // No account at all -> the balance-based figure stays null throughout, unaffected.
        assertNull(simulation.before.freeMoney)
        assertNull(simulation.after.freeMoney)
    }

    @Test
    fun `plan-based daily budget drops by the amount spread over the remaining days`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 21),
            incomes = emptyList(), recurringExpenses = emptyList(), variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0, currentAccountBalances = emptyList()
        )
        val simulation = BudgetEngine.simulateExpense(plan, amount = 100.0)
        // 10 days remain in September from the 21st.
        assertEquals(-10.0, simulation.after.dailyRecommendedBudget, 0.0001)
        assertEquals(10.0, simulation.dailyRecommendedBudgetDelta, 0.0001)
    }

    @Test
    fun `an expense that would breach the plan safety threshold is flagged`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 21),
            incomes = listOf(monthlyIncome(day = 1, amount = 1000.0)),
            recurringExpenses = emptyList(), variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0, currentAccountBalances = emptyList(), safetyThreshold = 900.0
        )
        val simulation = BudgetEngine.simulateExpense(plan, amount = 200.0)
        assertTrue(simulation.wouldBreachPlanSafetyThreshold)
        assertEquals(100.0, simulation.planAmountUnderThreshold!!, 0.0001)
    }

    @Test
    fun `an expense that stays above the plan safety threshold is not flagged`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 21),
            incomes = listOf(monthlyIncome(day = 1, amount = 1000.0)),
            recurringExpenses = emptyList(), variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0, currentAccountBalances = emptyList(), safetyThreshold = 200.0
        )
        val simulation = BudgetEngine.simulateExpense(plan, amount = 300.0)
        assertFalse(simulation.wouldBreachPlanSafetyThreshold)
        assertNull(simulation.planAmountUnderThreshold)
    }

    // --- Savings goal impact (spec section 15) ----------------------------------------------

    @Test
    fun `goal delay matches the spec's about-12-days example`() {
        assertEquals(12, BudgetEngine.goalDelayDays(amount = 120.0, monthlyContribution = 300.0))
    }

    @Test
    fun `a goal with no active contribution has no measurable delay`() {
        assertNull(BudgetEngine.goalDelayDays(amount = 120.0, monthlyContribution = 0.0))
    }

    // --- Future timeline / time travel (spec sections 8 & 9) --------------------------------

    @Test
    fun `daily projection starts on today with no occurrences attached`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 30),
            incomes = emptyList(), recurringExpenses = emptyList(), variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0, currentAccountBalances = listOf(500.0)
        )
        val projection = BudgetEngine.projectDailyBalances(plan)
        assertEquals(listOf(DailyPoint(LocalDate.of(2026, 9, 30), 500.0)), projection.map { DailyPoint(it.date, it.balance) })
        assertTrue(projection.single().occurrences.isEmpty())
    }

    @Test
    fun `daily projection walks the running balance across every future occurrence`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 20),
            incomes = emptyList(),
            recurringExpenses = listOf(
                monthlyExpense(day = 22, amount = 80.0, label = "Courses"),
                monthlyExpense(day = 25, amount = 120.0, label = "Assurance"),
                monthlyExpense(day = 30, amount = 1200.0, label = "Credit")
            ),
            variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0,
            currentAccountBalances = listOf(2438.0)
        )
        val projection = BudgetEngine.projectDailyBalances(plan)
        val byDate = projection.associateBy { it.date }

        assertEquals(2438.0, byDate.getValue(LocalDate.of(2026, 9, 20)).balance, 0.0001)
        assertEquals(2438.0, byDate.getValue(LocalDate.of(2026, 9, 21)).balance, 0.0001) // no occurrence yet
        assertEquals(2358.0, byDate.getValue(LocalDate.of(2026, 9, 22)).balance, 0.0001) // -80
        assertEquals(2238.0, byDate.getValue(LocalDate.of(2026, 9, 25)).balance, 0.0001) // -120
        assertEquals(1038.0, byDate.getValue(LocalDate.of(2026, 9, 30)).balance, 0.0001) // -1200, month-end estimate
        assertEquals(11, projection.size) // 20..30 inclusive
        assertEquals("Courses", byDate.getValue(LocalDate.of(2026, 9, 22)).occurrences.single().label)
    }

    @Test
    fun `daily projection includes future income alongside expenses`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 20),
            incomes = listOf(monthlyIncome(day = 28, amount = 500.0, label = "Prime")),
            recurringExpenses = emptyList(),
            variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0,
            currentAccountBalances = listOf(1000.0)
        )
        val projection = BudgetEngine.projectDailyBalances(plan)
        val byDate = projection.associateBy { it.date }
        assertEquals(1500.0, byDate.getValue(LocalDate.of(2026, 9, 28)).balance, 0.0001)
        assertEquals(1500.0, byDate.getValue(LocalDate.of(2026, 9, 30)).balance, 0.0001)
    }

    @Test
    fun `daily projection is empty without any account to project from`() {
        val plan = basePlan(YearMonth.of(2026, 9), today = LocalDate.of(2026, 9, 20))
        assertTrue(BudgetEngine.projectDailyBalances(plan).isEmpty())
    }

    @Test
    fun `daily projection is empty once today is past the month being projected`() {
        val plan = MonthPlan(
            month = YearMonth.of(2026, 9), today = LocalDate.of(2026, 10, 1),
            incomes = emptyList(), recurringExpenses = emptyList(), variableBudgets = emptyList(),
            plannedMonthlySavings = 0.0, currentAccountBalances = listOf(500.0)
        )
        assertTrue(BudgetEngine.projectDailyBalances(plan).isEmpty())
    }

    private data class DailyPoint(val date: LocalDate, val balance: Double)

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
