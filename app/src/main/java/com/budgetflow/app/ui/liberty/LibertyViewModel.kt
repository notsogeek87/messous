package com.budgetflow.app.ui.liberty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.repository.AccountRepository
import com.budgetflow.app.domain.repository.CategoryRepository
import com.budgetflow.app.domain.repository.TransactionRepository
import com.budgetflow.app.domain.usecase.GetCalendarOccurrencesUseCase
import com.budgetflow.app.domain.usecase.GetDashboardForMonthUseCase
import com.budgetflow.app.ui.transactions.TransactionListItem
import com.budgetflow.engine.BudgetEngine
import com.budgetflow.engine.model.CalendarOccurrence
import com.budgetflow.engine.model.FlowDirection
import com.budgetflow.engine.model.FreedomState
import com.budgetflow.engine.model.MonthSummary
import com.budgetflow.engine.model.ScheduledFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class LibertyUiState(
    val isLoading: Boolean = true,
    val summary: MonthSummary? = null,
    val today: LocalDate = LocalDate.now(),
    /** The nearest sizeable upcoming expense, if any, used for the "grosse dépense arrive" message. */
    val notableUpcomingExpense: CalendarOccurrence? = null,
    val upcomingOccurrences: List<CalendarOccurrence> = emptyList(),
    /** Every active income/fixed expense this month, for the "tout d'un coup d'œil" recap at the bottom. */
    val incomes: List<ScheduledFlow> = emptyList(),
    val recurringExpenses: List<ScheduledFlow> = emptyList(),
    /** [BudgetEngine.remainingToSpendAsOf] for every day of the month, indexed 0 = day 1 - backs the interactive gauge. */
    val remainingToSpendByDay: List<Double> = emptyList(),
    val remainingToSpendStateByDay: List<FreedomState> = emptyList(),
    /** Every transaction actually recorded this month, most recent first. */
    val transactions: List<TransactionListItem> = emptyList()
) {
    val hasAnyData: Boolean
        get() = summary != null && (summary.totalIncome != 0.0 || summary.totalFixedExpenses != 0.0 || summary.totalVariableBudgetAllocated != 0.0)
}

/** Drives "Ma liberté" (spec section 3): the whole app boils down to this one screen's numbers. */
class LibertyViewModel(
    dashboardUseCase: GetDashboardForMonthUseCase,
    calendarUseCase: GetCalendarOccurrencesUseCase,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val month = YearMonth.from(today)
    private val monthStart = month.atDay(1)
    private val monthEnd = month.atEndOfMonth()

    val uiState: StateFlow<LibertyUiState> = combine(
        combine(
            dashboardUseCase.observePlan(month, today),
            calendarUseCase.observe(today, monthEnd)
        ) { plan, occurrences -> plan to occurrences },
        combine(
            transactionRepository.observeInRange(monthStart, monthEnd),
            categoryRepository.observeCategories(),
            accountRepository.observeAccounts()
        ) { transactions, categories, accounts -> Triple(transactions, categories, accounts) }
    ) { (plan, occurrences), (transactions, categories, accounts) ->
        val summary = BudgetEngine.summarizeMonth(plan)
        val upcoming = occurrences.filter { it.date.isAfter(today) }
        val freedomPerDay = summary.freedomPerDay
        // "Grosse dépense" = the single largest expense landing within the next 5 days,
        // as long as it would meaningfully dent a day's worth of free money.
        val notable = upcoming
            .filter { it.direction == FlowDirection.EXPENSE && !it.date.isAfter(today.plusDays(5)) }
            .filter { freedomPerDay == null || it.amount > freedomPerDay * 2 }
            .maxByOrNull { it.amount }

        val daysInMonth = plan.monthEnd.dayOfMonth
        val remainingToSpendByDay = (1..daysInMonth).map { day ->
            BudgetEngine.remainingToSpendAsOf(plan, plan.monthStart.plusDays((day - 1).toLong()))
        }
        val remainingToSpendStateByDay = remainingToSpendByDay.map { BudgetEngine.freedomStateFor(it, plan.safetyThreshold) }

        val transactionItems = transactions
            .sortedByDescending { it.date }
            .map { transaction ->
                TransactionListItem(
                    transaction = transaction,
                    category = categories.firstOrNull { it.id == transaction.categoryId },
                    account = accounts.firstOrNull { it.id == transaction.accountId }
                )
            }

        LibertyUiState(
            isLoading = false,
            summary = summary,
            today = today,
            notableUpcomingExpense = notable,
            upcomingOccurrences = upcoming.take(3),
            incomes = plan.incomes,
            recurringExpenses = plan.recurringExpenses,
            remainingToSpendByDay = remainingToSpendByDay,
            remainingToSpendStateByDay = remainingToSpendStateByDay,
            transactions = transactionItems
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibertyUiState())
}
