package com.budgetflow.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.usecase.GetCalendarOccurrencesUseCase
import com.budgetflow.app.domain.usecase.GetDashboardForMonthUseCase
import com.budgetflow.engine.model.CalendarOccurrence
import com.budgetflow.engine.model.MonthSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class DashboardUiState(
    val isLoading: Boolean = true,
    val summary: MonthSummary? = null,
    val upcomingOccurrences: List<CalendarOccurrence> = emptyList(),
    val monthYear: YearMonth = YearMonth.now()
) {
    val hasAnyData: Boolean
        get() = summary != null && (summary.totalIncome != 0.0 || summary.totalFixedExpenses != 0.0 || summary.totalVariableBudgetAllocated != 0.0)
}

class DashboardViewModel(
    dashboardUseCase: GetDashboardForMonthUseCase,
    calendarUseCase: GetCalendarOccurrencesUseCase
) : ViewModel() {

    private val today = LocalDate.now()
    private val month = YearMonth.from(today)

    val uiState: StateFlow<DashboardUiState> = combine(
        dashboardUseCase.observe(month, today),
        calendarUseCase.observe(today, month.atEndOfMonth())
    ) { summary, occurrences ->
        DashboardUiState(
            isLoading = false,
            summary = summary,
            upcomingOccurrences = occurrences.take(5),
            monthYear = month
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
