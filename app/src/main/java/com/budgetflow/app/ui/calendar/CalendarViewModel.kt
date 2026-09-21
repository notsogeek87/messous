package com.budgetflow.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.usecase.GetCalendarOccurrencesUseCase
import com.budgetflow.engine.model.CalendarOccurrence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val occurrencesByDate: Map<LocalDate, List<CalendarOccurrence>> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val calendarUseCase: GetCalendarOccurrencesUseCase) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<CalendarUiState> = month.flatMapLatest { currentMonth ->
        calendarUseCase.observe(currentMonth.atDay(1), currentMonth.atEndOfMonth()).map { occurrences ->
            CalendarUiState(
                month = currentMonth,
                occurrencesByDate = occurrences.groupBy { it.date }.toSortedMap()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun nextMonth() = month.update { it.plusMonths(1) }
    fun previousMonth() = month.update { it.minusMonths(1) }
}
