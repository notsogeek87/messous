package com.budgetflow.app.ui.future

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.usecase.GetDailyProjectionUseCase
import com.budgetflow.app.domain.usecase.GetDashboardForMonthUseCase
import com.budgetflow.app.domain.usecase.GetMonthlyForecastsUseCase
import com.budgetflow.engine.model.DailyProjection
import com.budgetflow.engine.model.MonthSummary
import com.budgetflow.engine.model.MonthlyForecast
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class FutureUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val monthEnd: LocalDate = YearMonth.now().atEndOfMonth(),
    val dailyProjections: List<DailyProjection> = emptyList(),
    val baseline: MonthSummary? = null,
    val nextMonthsForecast: List<MonthlyForecast> = emptyList()
)

/** Drives "Mon futur" (spec sections 8 & 9): the day-by-day timeline and the "voyage dans le temps" slider. */
class FutureViewModel(
    dashboardUseCase: GetDashboardForMonthUseCase,
    dailyProjectionUseCase: GetDailyProjectionUseCase,
    forecastUseCase: GetMonthlyForecastsUseCase
) : ViewModel() {

    private val today = LocalDate.now()
    private val month = YearMonth.from(today)

    val uiState: StateFlow<FutureUiState> = combine(
        dashboardUseCase.observe(month, today),
        dailyProjectionUseCase.observe(today),
        forecastUseCase.observe(month.plusMonths(1), 3)
    ) { summary, projections, forecasts ->
        FutureUiState(
            isLoading = false,
            today = today,
            monthEnd = month.atEndOfMonth(),
            dailyProjections = projections,
            baseline = summary,
            nextMonthsForecast = forecasts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FutureUiState())
}

/**
 * What "argent libre" / "liberté quotidienne" would look like on [projection]'s date, reusing
 * the same envelopes-and-savings reservation already computed for today in [baseline] (those
 * don't change day to day) combined with that day's real projected balance.
 */
fun freedomAtProjection(projection: DailyProjection, baseline: MonthSummary, monthEnd: LocalDate): Pair<Double, Double?> {
    val freeMoneyAtDate = projection.balance - baseline.remainingVariableBudget - baseline.remainingPlannedSavings
    val daysRemaining = (monthEnd.toEpochDay() - projection.date.toEpochDay() + 1).toInt()
    val freedomPerDayAtDate = if (daysRemaining > 0) freeMoneyAtDate / daysRemaining else null
    return freeMoneyAtDate to freedomPerDayAtDate
}
