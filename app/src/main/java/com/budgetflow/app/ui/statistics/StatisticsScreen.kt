package com.budgetflow.app.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetflow.app.R
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.di.simpleViewModelFactory
import com.budgetflow.app.ui.components.EmptyState
import com.budgetflow.app.ui.components.GroupedBarChart
import com.budgetflow.app.ui.components.HorizontalBarRow
import com.budgetflow.app.ui.components.LabeledRow
import com.budgetflow.app.ui.components.MoneyText
import com.budgetflow.app.ui.components.SectionCard
import com.budgetflow.app.ui.components.formatMoney
import com.budgetflow.app.ui.theme.negativeRed
import com.budgetflow.app.ui.theme.positiveGreen
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(onBack: () -> Unit, onAddTransaction: () -> Unit) {
    val viewModel: StatisticsViewModel = viewModel(
        factory = simpleViewModelFactory { StatisticsViewModel(ServiceLocator.transactionRepository, ServiceLocator.categoryRepository) }
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        if (!state.isLoading && !state.hasAnyTransactions) {
            EmptyState(
                icon = Icons.Filled.BarChart,
                title = stringResource(R.string.statistics_empty_title),
                body = stringResource(R.string.statistics_empty_body),
                actionLabel = stringResource(R.string.transactions_add),
                onAction = onAddTransaction,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SavedThisMonthCard(state) }
            item { IncomeVsExpenseCard(state) }
            if (state.categorySlices.isNotEmpty()) {
                item { CategoryBreakdownCard(state) }
            }
            item { MonthlyEvolutionCard(state) }
        }
    }
}

@Composable
private fun SavedThisMonthCard(state: StatisticsUiState) {
    SectionCard(title = stringResource(R.string.statistics_saved_this_month)) {
        MoneyText(amount = state.savedThisMonth, style = MaterialTheme.typography.headlineMedium, colorBySign = true)
        val delta = state.expenseDeltaVsLastMonth
        val deltaColor = if (delta <= 0) positiveGreen else negativeRed
        val deltaText = if (delta <= 0) "▼ ${formatMoney(-delta)}" else "▲ ${formatMoney(delta)}"
        Text(
            "$deltaText " + stringResource(R.string.statistics_vs_last_month),
            style = MaterialTheme.typography.bodyMedium,
            color = deltaColor
        )
    }
}

@Composable
private fun IncomeVsExpenseCard(state: StatisticsUiState) {
    SectionCard(title = stringResource(R.string.statistics_income_vs_expense)) {
        LabeledRow(stringResource(R.string.dashboard_income)) { MoneyText(state.currentIncome) }
        LabeledRow(stringResource(R.string.transaction_type_expense)) { MoneyText(state.currentExpense) }
    }
}

@Composable
private fun CategoryBreakdownCard(state: StatisticsUiState) {
    val otherLabel = stringResource(R.string.category_other)
    SectionCard(title = stringResource(R.string.statistics_by_category)) {
        state.categorySlices.take(8).forEach { slice ->
            HorizontalBarRow(
                label = slice.category?.name ?: otherLabel,
                amountLabel = formatMoney(slice.amount),
                fraction = slice.fraction,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MonthlyEvolutionCard(state: StatisticsUiState) {
    SectionCard(title = stringResource(R.string.statistics_evolution)) {
        val labels = state.monthlyHistory.map { it.month.month.getDisplayName(TextStyle.SHORT, Locale.FRENCH) }
        GroupedBarChart(
            labels = labels,
            seriesA = state.monthlyHistory.map { it.income.toFloat() },
            seriesB = state.monthlyHistory.map { it.expense.toFloat() },
            colorA = positiveGreen,
            colorB = negativeRed,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendDot(color = positiveGreen, label = stringResource(R.string.dashboard_income))
            LegendDot(color = negativeRed, label = stringResource(R.string.transaction_type_expense))
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
