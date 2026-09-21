package com.budgetflow.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budgetflow.app.R
import com.budgetflow.engine.model.Frequency
import java.time.DayOfWeek
import kotlinx.coroutines.launch

@Composable
fun FrequencySelector(
    frequency: Frequency,
    onFrequencyChange: (Frequency) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        Frequency.MONTHLY to R.string.budget_frequency_monthly,
        Frequency.WEEKLY to R.string.budget_frequency_weekly,
        Frequency.YEARLY to R.string.budget_frequency_yearly,
        Frequency.ONE_TIME to R.string.budget_frequency_one_time
    )
    LazyRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { (value, labelRes) ->
            FilterChip(
                selected = frequency == value,
                onClick = { onFrequencyChange(value) },
                label = { Text(stringResource(labelRes)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduleDetailsFields(
    frequency: Frequency,
    dayOfMonth: Int,
    onDayOfMonthChange: (Int) -> Unit,
    dayOfWeek: DayOfWeek,
    onDayOfWeekChange: (DayOfWeek) -> Unit,
    monthOfYear: Int,
    onMonthOfYearChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayOfMonthBringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val dayOfMonthFieldModifier = Modifier
        .fillMaxWidth()
        .bringIntoViewRequester(dayOfMonthBringIntoViewRequester)
        .onFocusEvent { focusState ->
            if (focusState.isFocused) {
                coroutineScope.launch { dayOfMonthBringIntoViewRequester.bringIntoView() }
            }
        }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (frequency) {
            Frequency.MONTHLY -> {
                OutlinedTextField(
                    value = dayOfMonth.toString(),
                    onValueChange = { text -> text.toIntOrNull()?.let { if (it in 1..31) onDayOfMonthChange(it) } },
                    label = { Text(stringResource(R.string.budget_day_of_month)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = dayOfMonthFieldModifier
                )
            }
            Frequency.YEARLY -> {
                OutlinedTextField(
                    value = dayOfMonth.toString(),
                    onValueChange = { text -> text.toIntOrNull()?.let { if (it in 1..31) onDayOfMonthChange(it) } },
                    label = { Text(stringResource(R.string.budget_day_of_month)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = dayOfMonthFieldModifier
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..12).toList()) { month ->
                        FilterChip(
                            selected = monthOfYear == month,
                            onClick = { onMonthOfYearChange(month) },
                            label = { Text(frenchMonthAbbreviation(month)) }
                        )
                    }
                }
            }
            Frequency.WEEKLY -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DayOfWeek.entries.toList()) { day ->
                        FilterChip(
                            selected = dayOfWeek == day,
                            onClick = { onDayOfWeekChange(day) },
                            label = { Text(frenchWeekdayAbbreviation(day)) }
                        )
                    }
                }
            }
            Frequency.ONE_TIME -> Unit
        }
    }
}

private fun frenchWeekdayAbbreviation(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Lun"
    DayOfWeek.TUESDAY -> "Mar"
    DayOfWeek.WEDNESDAY -> "Mer"
    DayOfWeek.THURSDAY -> "Jeu"
    DayOfWeek.FRIDAY -> "Ven"
    DayOfWeek.SATURDAY -> "Sam"
    DayOfWeek.SUNDAY -> "Dim"
}

private fun frenchMonthAbbreviation(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Fév"; 3 -> "Mar"; 4 -> "Avr"; 5 -> "Mai"; 6 -> "Juin"
    7 -> "Juil"; 8 -> "Août"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Déc"
}
