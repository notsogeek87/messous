package com.budgetflow.app.ui.liberty

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.budgetflow.app.domain.model.TransactionType
import com.budgetflow.app.ui.components.AnimatedMoneyText
import com.budgetflow.app.ui.components.EmptyState
import com.budgetflow.app.ui.components.FreedomStateBadge
import com.budgetflow.app.ui.components.MoneyText
import com.budgetflow.app.ui.components.ProfileSelector
import com.budgetflow.app.ui.components.SafetyThresholdGauge
import com.budgetflow.app.ui.components.color
import com.budgetflow.app.ui.components.formatMoney
import com.budgetflow.app.ui.components.frequencyLabel
import com.budgetflow.app.ui.transactions.TransactionListItem
import com.budgetflow.engine.model.CalendarOccurrence
import com.budgetflow.engine.model.FlowDirection
import com.budgetflow.engine.model.FreedomState
import com.budgetflow.engine.model.MonthSummary
import com.budgetflow.engine.model.ScheduledFlow
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val transactionDateFormatter = DateTimeFormatter.ofPattern("dd/MM")

/**
 * "Ma liberté" (spec section 3): the app's home screen and its single most important surface.
 * Everything here answers one question - "combien puis-je dépenser sans mettre mon mois en
 * danger ?" - in the first five seconds, before any secondary detail.
 */
@Composable
fun LibertyScreen(
    onOpenBudget: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenFuture: () -> Unit,
    onOpenWhatIf: () -> Unit,
    onAddTransaction: () -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit
) {
    val viewModel: LibertyViewModel = viewModel(
        factory = simpleViewModelFactory {
            LibertyViewModel(
                ServiceLocator.dashboardUseCase,
                ServiceLocator.calendarUseCase,
                ServiceLocator.transactionRepository,
                ServiceLocator.categoryRepository,
                ServiceLocator.accountRepository
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    val summary = state.summary

    var addMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                ProfileSelector()
            }
        },
        floatingActionButton = {
            if (state.hasAnyData) {
                Box {
                    ExtendedFloatingActionButton(onClick = { addMenuExpanded = true }, icon = {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }, text = { Text(stringResource(R.string.liberty_add_transaction_cta)) })
                    DropdownMenu(expanded = addMenuExpanded, onDismissRequest = { addMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.transactions_add)) },
                            leadingIcon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                            onClick = { addMenuExpanded = false; onAddTransaction() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.budget_add_income)) },
                            leadingIcon = { Icon(Icons.Filled.TrendingUp, contentDescription = null) },
                            onClick = { addMenuExpanded = false; onAddIncome() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.budget_add_expense)) },
                            leadingIcon = { Icon(Icons.Filled.TrendingDown, contentDescription = null) },
                            onClick = { addMenuExpanded = false; onAddExpense() }
                        )
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading || summary == null -> Box(modifier = Modifier.fillMaxSize().padding(padding))

            !state.hasAnyData -> Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    icon = Icons.Filled.Savings,
                    title = stringResource(R.string.liberty_no_data_title),
                    body = stringResource(R.string.liberty_no_data_body),
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = onOpenBudget, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.budget_add_income))
                }
            }

            else -> LibertyContent(
                state = state,
                summary = summary,
                padding = padding,
                onOpenFuture = onOpenFuture,
                onOpenWhatIf = onOpenWhatIf,
                onOpenAccounts = onOpenAccounts
            )
        }
    }
}

@Composable
private fun LibertyContent(
    state: LibertyUiState,
    summary: MonthSummary,
    padding: PaddingValues,
    onOpenFuture: () -> Unit,
    onOpenWhatIf: () -> Unit,
    onOpenAccounts: () -> Unit
) {
    val freeMoney = summary.freeMoney
    val freedomState = summary.freedomState

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                FreedomStateBadge(freedomState)
                Text(
                    text = stringResource(R.string.liberty_remaining_to_spend_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                AnimatedMoneyText(
                    amount = summary.remainingToSpend,
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = freedomState.color()
                )
                Text(
                    text = stringResource(R.string.liberty_available_budget_caption, formatMoney(summary.availableBudget)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (freeMoney != null) {
                    Text(
                        text = stringResource(R.string.liberty_real_balance_label, formatMoney(freeMoney)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    summary.currentBankBalance?.let { balance ->
                        Text(
                            text = "${stringResource(R.string.liberty_balance_label)} : ${formatMoney(balance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    TextButton(onClick = onOpenAccounts, modifier = Modifier.padding(top = 4.dp)) {
                        Text(stringResource(R.string.liberty_add_account_cta))
                    }
                }
            }
        }

        item {
            val maxDay = maxOf(state.remainingToSpendByDay.size, 1)
            var selectedDay by remember(state.today, maxDay) {
                mutableStateOf(state.today.dayOfMonth.coerceIn(1, maxDay))
            }
            val remainingForSelectedDay = state.remainingToSpendByDay.getOrElse(selectedDay - 1) { summary.remainingToSpendToday }
            val stateForSelectedDay = state.remainingToSpendStateByDay.getOrElse(selectedDay - 1) { summary.remainingToSpendTodayState }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.liberty_remaining_today_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedMoneyText(
                        amount = remainingForSelectedDay,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = stateForSelectedDay.color(),
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                    Slider(
                        value = selectedDay.toFloat(),
                        onValueChange = { selectedDay = it.roundToInt().coerceIn(1, maxDay) },
                        valueRange = 1f..maxDay.toFloat(),
                        steps = maxOf(maxDay - 2, 0),
                        colors = SliderDefaults.colors(
                            thumbColor = stateForSelectedDay.color(),
                            activeTrackColor = stateForSelectedDay.color()
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.liberty_day_gauge_day_marker, selectedDay, maxDay),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.liberty_day_gauge_month_target, formatMoney(summary.remainingToSpend)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = freedomState.color().copy(alpha = 0.10f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.liberty_daily_freedom_intro),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedMoneyText(
                        amount = summary.dailyRecommendedBudget,
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = freedomState.color()
                    )
                    Text(
                        stringResource(R.string.liberty_daily_freedom_until_end, summary.remainingDaysInMonth),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (freeMoney != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        SafetyThresholdGauge(
                            freeMoney = freeMoney,
                            safetyThreshold = summary.safetyThreshold,
                            state = freedomState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = libertyMessage(state, summary),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )
        }

        item {
            TextButton(onClick = onOpenWhatIf, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.liberty_simulate_cta))
                Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
            }
        }

        if (state.upcomingOccurrences.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenFuture)
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.liberty_upcoming_title), style = MaterialTheme.typography.titleSmall)
                    state.upcomingOccurrences.forEach { occurrence -> UpcomingRow(occurrence) }
                    Text(
                        stringResource(R.string.liberty_see_future),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        if (state.incomes.isNotEmpty() || state.recurringExpenses.isNotEmpty()) {
            item {
                Text(stringResource(R.string.liberty_recurring_title), style = MaterialTheme.typography.titleMedium)
            }
            items(state.incomes, key = { "income-${it.id}" }) { income -> RecurringFlowRow(income, isExpense = false) }
            items(state.recurringExpenses, key = { "expense-${it.id}" }) { expense -> RecurringFlowRow(expense, isExpense = true) }
        }

        if (state.transactions.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.liberty_transactions_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(state.transactions, key = { "tx-${it.transaction.id}" }) { item -> LibertyTransactionRow(item) }
        }
    }
}

@Composable
private fun RecurringFlowRow(flow: ScheduledFlow, isExpense: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(flow.label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    frequencyLabel(flow.frequency, flow.dayOfMonth, flow.dayOfWeek, flow.monthOfYear, flow.oneTimeDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MoneyText(
                amount = if (isExpense) -flow.amount else flow.amount,
                colorBySign = true,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LibertyTransactionRow(item: TransactionListItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    item.transaction.description.ifBlank { item.category?.name ?: "" },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${item.transaction.date.format(transactionDateFormatter)} · ${item.account?.name ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val signedAmount = if (item.transaction.type == TransactionType.EXPENSE) -item.transaction.amount else item.transaction.amount
            MoneyText(amount = signedAmount, colorBySign = true, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun UpcomingRow(occurrence: CalendarOccurrence) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("${occurrence.date.dayOfMonth} · ${occurrence.label}", style = MaterialTheme.typography.bodyMedium)
        MoneyText(
            amount = if (occurrence.direction == FlowDirection.EXPENSE) -occurrence.amount else occurrence.amount,
            colorBySign = true,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Builds the one contextual sentence for the screen (spec section 7) from purely objective data. */
@Composable
private fun libertyMessage(state: LibertyUiState, summary: MonthSummary): String {
    val notable = state.notableUpcomingExpense
    val freedomPerDay = summary.freedomPerDay
    return when {
        notable != null -> {
            val weekday = notable.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH)
            stringResource(R.string.liberty_message_big_expense, weekday)
        }
        summary.freedomState == FreedomState.ALERT -> stringResource(R.string.liberty_message_alert)
        summary.freedomState == FreedomState.CAUTION -> stringResource(R.string.liberty_message_caution)
        freedomPerDay != null && summary.safetyThreshold > 0 && freedomPerDay > summary.safetyThreshold / 10.0 ->
            stringResource(R.string.liberty_message_comfortable)
        else -> stringResource(R.string.liberty_message_normal, formatMoney(freedomPerDay ?: summary.dailyRecommendedBudget))
    }
}
