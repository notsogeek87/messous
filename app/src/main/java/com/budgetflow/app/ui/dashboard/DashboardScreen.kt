package com.budgetflow.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetflow.app.R
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.di.simpleViewModelFactory
import com.budgetflow.app.ui.components.EmptyState
import com.budgetflow.app.ui.components.LabeledRow
import com.budgetflow.app.ui.components.MoneyText
import com.budgetflow.app.ui.components.SectionCard
import com.budgetflow.app.ui.components.formatMoney
import com.budgetflow.engine.model.CalendarOccurrence
import com.budgetflow.engine.model.FlowDirection
import com.budgetflow.engine.model.MonthSummary
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onOpenCalendar: () -> Unit, onOpenBudget: () -> Unit) {
    val viewModel: DashboardViewModel = viewModel(
        factory = simpleViewModelFactory {
            DashboardViewModel(ServiceLocator.dashboardUseCase, ServiceLocator.calendarUseCase)
        }
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val monthName = state.monthYear.month.getDisplayName(TextStyle.FULL, Locale.FRENCH)
                        .replaceFirstChar { it.uppercase() }
                    Text("$monthName ${state.monthYear.year}")
                },
                actions = {
                    IconButton(onClick = onOpenCalendar) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.calendar_title))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { padding ->
        val summary = state.summary
        if (!state.isLoading && summary != null && !state.hasAnyData) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    icon = Icons.Filled.Savings,
                    title = stringResource(R.string.dashboard_empty_title),
                    body = stringResource(R.string.dashboard_empty_body),
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.material3.Button(
                    onClick = onOpenBudget,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) { Text(stringResource(R.string.budget_add_income)) }
            }
            return@Scaffold
        }

        if (summary == null) return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { RemainingToSpendCard(summary) }
            item { MonthBreakdownCard(summary) }
            summary.reallyAvailableNow?.let { reallyAvailable ->
                item { ReallyAvailableCard(bankBalance = summary.currentBankBalance ?: 0.0, reallyAvailable = reallyAvailable) }
            }
            if (state.upcomingOccurrences.isNotEmpty()) {
                item { UpcomingSection(state.upcomingOccurrences, onOpenCalendar) }
            }
        }
    }
}

@Composable
private fun RemainingToSpendCard(summary: MonthSummary) {
    SectionCard(title = stringResource(R.string.dashboard_remaining_to_spend)) {
        MoneyText(
            amount = summary.remainingToSpend,
            style = MaterialTheme.typography.headlineLarge,
            colorBySign = true
        )
        if (summary.remainingDaysInMonth > 0) {
            Text(
                text = stringResource(
                    R.string.dashboard_daily_budget_unit,
                    formatMoney(summary.dailyRecommendedBudget)
                ) + " · " + stringResource(R.string.dashboard_daily_budget),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthBreakdownCard(summary: MonthSummary) {
    SectionCard(title = stringResource(R.string.dashboard_this_month)) {
        LabeledRow(stringResource(R.string.dashboard_income)) { MoneyText(summary.totalIncome) }
        LabeledRow(stringResource(R.string.dashboard_fixed_expenses)) { MoneyText(summary.totalFixedExpenses) }
        LabeledRow(stringResource(R.string.dashboard_variable_budgets)) { MoneyText(summary.totalVariableBudgetAllocated) }
        LabeledRow(stringResource(R.string.dashboard_planned_savings)) { MoneyText(summary.plannedSavings) }
        Divider(modifier = Modifier.padding(vertical = 4.dp))
        LabeledRow(stringResource(R.string.dashboard_available_budget)) {
            Text(formatMoney(summary.availableBudget), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReallyAvailableCard(bankBalance: Double, reallyAvailable: Double) {
    SectionCard(title = stringResource(R.string.dashboard_bank_balance)) {
        LabeledRow(stringResource(R.string.dashboard_bank_balance)) { MoneyText(bankBalance) }
        Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MoneyText(amount = reallyAvailable, style = MaterialTheme.typography.headlineMedium, colorBySign = true)
            Text(stringResource(R.string.dashboard_really_available), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UpcomingSection(occurrences: List<CalendarOccurrence>, onSeeAll: () -> Unit) {
    SectionCard(title = stringResource(R.string.dashboard_upcoming_this_month)) {
        occurrences.forEach { occurrence ->
            LabeledRow("${occurrence.date.dayOfMonth} · ${occurrence.label}") {
                MoneyText(
                    amount = if (occurrence.direction == FlowDirection.EXPENSE) -occurrence.amount else occurrence.amount,
                    colorBySign = true
                )
            }
        }
        Text(
            text = stringResource(R.string.calendar_title),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clickable(onClick = onSeeAll),
            style = MaterialTheme.typography.labelLarge
        )
    }
}
