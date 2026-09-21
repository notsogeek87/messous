package com.budgetflow.app.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetflow.app.R
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.di.simpleViewModelFactory
import com.budgetflow.app.ui.components.EmptyState
import com.budgetflow.app.ui.components.MoneyText
import com.budgetflow.engine.model.CalendarOccurrence
import com.budgetflow.engine.model.FlowDirection
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit) {
    val viewModel: CalendarViewModel = viewModel(
        factory = simpleViewModelFactory { CalendarViewModel(ServiceLocator.calendarUseCase) }
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResourceMonth(state.month)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = viewModel::previousMonth) { Icon(Icons.Filled.ChevronLeft, contentDescription = null) }
                    IconButton(onClick = viewModel::nextMonth) { Icon(Icons.Filled.ChevronRight, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        if (state.occurrencesByDate.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Event,
                title = androidx.compose.ui.res.stringResource(R.string.calendar_empty),
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            state.occurrencesByDate.forEach { (date, occurrences) ->
                item(key = date.toEpochDay()) { DateHeader(date) }
                items(occurrences, key = { it.flowId to it.direction to it.date }) { occurrence ->
                    OccurrenceRow(occurrence)
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun stringResourceMonth(month: java.time.YearMonth): String {
    val name = month.month.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase() }
    return "$name ${month.year}"
}

@Composable
private fun DateHeader(date: LocalDate) {
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase() }
    Text(
        text = "$weekday ${date.dayOfMonth}",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun OccurrenceRow(occurrence: CalendarOccurrence) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(occurrence.label, style = MaterialTheme.typography.bodyLarge)
        MoneyText(
            amount = if (occurrence.direction == FlowDirection.EXPENSE) -occurrence.amount else occurrence.amount,
            colorBySign = true
        )
    }
}
