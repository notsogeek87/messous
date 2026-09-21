package com.budgetflow.app.ui.liberty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.usecase.GetCalendarOccurrencesUseCase
import com.budgetflow.app.domain.usecase.GetDashboardForMonthUseCase
import com.budgetflow.engine.model.CalendarOccurrence
import com.budgetflow.engine.model.FlowDirection
import com.budgetflow.engine.model.MonthSummary
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
    val upcomingOccurrences: List<CalendarOccurrence> = emptyList()
) {
    val hasAnyData: Boolean
        get() = summary != null && (summary.totalIncome != 0.0 || summary.totalFixedExpenses != 0.0 || summary.totalVariableBudgetAllocated != 0.0)
}

/** Drives "Ma liberté" (spec section 3): the whole app boils down to this one screen's numbers. */
class LibertyViewModel(
    dashboardUseCase: GetDashboardForMonthUseCase,
    calendarUseCase: GetCalendarOccurrencesUseCase
) : ViewModel() {

    private val today = LocalDate.now()
    private val month = YearMonth.from(today)

    val uiState: StateFlow<LibertyUiState> = combine(
        dashboardUseCase.observe(month, today),
        calendarUseCase.observe(today, month.atEndOfMonth())
    ) { summary, occurrences ->
        val upcoming = occurrences.filter { it.date.isAfter(today) }
        val freedomPerDay = summary.freedomPerDay
        // "Grosse dépense" = the single largest expense landing within the next 5 days,
        // as long as it would meaningfully dent a day's worth of free money.
        val notable = upcoming
            .filter { it.direction == FlowDirection.EXPENSE && !it.date.isAfter(today.plusDays(5)) }
            .filter { freedomPerDay == null || it.amount > freedomPerDay * 2 }
            .maxByOrNull { it.amount }

        LibertyUiState(
            isLoading = false,
            summary = summary,
            today = today,
            notableUpcomingExpense = notable,
            upcomingOccurrences = upcoming.take(3)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibertyUiState())
}
