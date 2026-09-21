package com.budgetflow.app.ui.future

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetflow.app.R
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.di.simpleViewModelFactory
import com.budgetflow.app.ui.components.AnimatedMoneyText
import com.budgetflow.app.ui.components.EmptyState
import com.budgetflow.app.ui.components.MoneyText
import com.budgetflow.engine.model.DailyProjection
import com.budgetflow.engine.model.FlowDirection
import com.budgetflow.engine.model.MonthlyForecast
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutureScreen() {
    val viewModel: FutureViewModel = viewModel(
        factory = simpleViewModelFactory {
            FutureViewModel(ServiceLocator.dashboardUseCase, ServiceLocator.dailyProjectionUseCase, ServiceLocator.forecastUseCase)
        }
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.future_title)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { padding ->
        if (state.dailyProjections.isEmpty() || state.baseline == null) {
            EmptyState(
                icon = Icons.Filled.Event,
                title = stringResource(R.string.future_empty),
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            return@Scaffold
        }

        FutureContent(state = state, padding = padding)
    }
}

@Composable
private fun FutureContent(state: FutureUiState, padding: PaddingValues) {
    val projections = state.dailyProjections
    val baseline = state.baseline!!
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selected = projections[selectedIndex.coerceIn(0, projections.lastIndex)]
    val (freeMoneyAtDate, freedomPerDayAtDate) = freedomAtProjection(selected, baseline, state.monthEnd)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            TimeTravelCard(
                state = state,
                selectedIndex = selectedIndex,
                selected = selected,
                freeMoneyAtDate = freeMoneyAtDate,
                freedomPerDayAtDate = freedomPerDayAtDate,
                onIndexChange = { selectedIndex = it }
            )
        }

        item { Text(stringResource(R.string.dashboard_upcoming_this_month), style = MaterialTheme.typography.titleMedium) }

        items(projections.filter { it.occurrences.isNotEmpty() || it == projections.first() || it == projections.last() }) { projection ->
            TimelineEntry(
                projection = projection,
                isToday = projection.date == state.today,
                isMonthEnd = projection.date == state.monthEnd,
                onClick = { selectedIndex = projections.indexOf(projection) }
            )
        }

        if (state.nextMonthsForecast.isNotEmpty()) {
            item { Text(stringResource(R.string.future_next_months), style = MaterialTheme.typography.titleMedium) }
            items(state.nextMonthsForecast) { forecast -> NextMonthRow(forecast) }
        }
    }
}

@Composable
private fun TimeTravelCard(
    state: FutureUiState,
    selectedIndex: Int,
    selected: DailyProjection,
    freeMoneyAtDate: Double,
    freedomPerDayAtDate: Double?,
    onIndexChange: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val label = if (selected.date == state.today) {
                stringResource(R.string.future_today)
            } else {
                val weekday = selected.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase() }
                "$weekday ${selected.date.dayOfMonth}"
            }
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            AnimatedMoneyText(
                amount = selected.balance,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
            )

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(stringResource(R.string.future_free_money_at_date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(freeMoneyAtDate, colorBySign = true, style = MaterialTheme.typography.bodyLarge)
                }
                freedomPerDayAtDate?.let { perDay ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.future_daily_freedom_at_date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(perDay, colorBySign = true, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Text(
                stringResource(R.string.future_time_travel_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            if (state.dailyProjections.size > 1) {
                Slider(
                    value = selectedIndex.toFloat(),
                    onValueChange = { onIndexChange(it.toInt()) },
                    valueRange = 0f..state.dailyProjections.lastIndex.toFloat(),
                    steps = (state.dailyProjections.size - 2).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TimelineEntry(projection: DailyProjection, isToday: Boolean, isMonthEnd: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val dateLabel = when {
                    isToday -> stringResource(R.string.future_today)
                    isMonthEnd -> stringResource(R.string.future_month_end_estimate)
                    else -> "${projection.date.dayOfMonth}"
                }
                Text(dateLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                projection.occurrences.forEach { occurrence ->
                    Text(occurrence.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                projection.occurrences.forEach { occurrence ->
                    MoneyText(
                        amount = if (occurrence.direction == FlowDirection.EXPENSE) -occurrence.amount else occurrence.amount,
                        colorBySign = true,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (isMonthEnd || isToday) {
                    MoneyText(projection.balance, style = MaterialTheme.typography.bodyLarge, colorBySign = false)
                }
            }
        }
    }
}

@Composable
private fun NextMonthRow(forecast: MonthlyForecast) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val name = forecast.month.month.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase() }
        Text(name, style = MaterialTheme.typography.bodyLarge)
        MoneyText(forecast.availableBudget, colorBySign = true, style = MaterialTheme.typography.bodyLarge)
    }
}
