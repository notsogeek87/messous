package com.budgetflow.app.ui.liberty

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.budgetflow.app.ui.components.LabeledRow
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

private val transactionDateFormatter = DateTimeFormatter.ofPattern("dd/MM")

/**
 * "Ma liberté" (spec section 3): the app's home screen and its single most important surface.
 * Everything here answers one question - "combien puis-je dépenser sans mettre mon mois en
 * danger ?" - in the first five seconds, before any secondary detail: one hero figure, one
 * per-day reading of it, one sentence, one gauge. The math behind it moves to a fold-out block
 * instead of competing with the headline for the first glance (audit §2/§10.2 - Lot 3).
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
            state.isLoading || summary == null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

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
            LibertyHero(summary = summary, onOpenAccounts = onOpenAccounts)
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

/**
 * The headline block: one figure ([MonthSummary.freeMoney], the real-balance one - falling back
 * to the plan-based [MonthSummary.remainingToSpend] only when there is no account to read a real
 * figure from, and *only then* left uncolored) with its per-day reading directly under it, both
 * rounded to the euro for a one-glance read. Never two different "per day" figures on this screen
 * (spec §3 P1/P2): the color and every number here come from the same base quantity.
 */
@Composable
private fun LibertyHero(summary: MonthSummary, onOpenAccounts: () -> Unit) {
    val freeMoney = summary.freeMoney
    val freedomState = summary.freedomState
    val heroAmount = freeMoney ?: summary.remainingToSpend
    val heroColor = if (freeMoney != null) freedomState.color() else MaterialTheme.colorScheme.onSurface
    val perDayAmount = summary.freedomPerDay ?: summary.dailyRecommendedBudget
    var detailsExpanded by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        FreedomStateBadge(freedomState)
        Text(
            text = stringResource(R.string.liberty_remaining_to_spend_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
        AnimatedMoneyText(
            amount = heroAmount,
            roundToEuro = true,
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = heroColor
        )
        Text(
            text = stringResource(R.string.liberty_hero_subtitle, formatMoney(perDayAmount, roundToEuro = true), summary.remainingDaysInMonth),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (freeMoney == null) {
            TextButton(onClick = onOpenAccounts, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.liberty_add_account_cta))
            }
        }

        TextButton(onClick = { detailsExpanded = !detailsExpanded }, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.liberty_details_toggle), style = MaterialTheme.typography.labelLarge)
            Icon(
                if (detailsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        AnimatedVisibility(visible = detailsExpanded) {
            LibertyDetailBreakdown(summary)
        }
    }
}

/** "D'où vient ce chiffre ?" (spec §2): the full plan-vs-solde math, for trust rather than the
 * first glance - every field is already computed on [MonthSummary], never re-derived here. */
@Composable
private fun LibertyDetailBreakdown(summary: MonthSummary) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LabeledRow(stringResource(R.string.dashboard_income)) { MoneyText(summary.totalIncome, colorBySign = true) }
            LabeledRow(stringResource(R.string.dashboard_fixed_expenses)) { MoneyText(-summary.totalFixedExpenses, colorBySign = true) }
            LabeledRow(stringResource(R.string.dashboard_variable_budgets)) { MoneyText(-summary.totalVariableBudgetAllocated, colorBySign = true) }
            if (summary.plannedSavings > 0.0) {
                LabeledRow(stringResource(R.string.dashboard_planned_savings)) { MoneyText(-summary.plannedSavings, colorBySign = true) }
            }
            Divider(modifier = Modifier.padding(vertical = 2.dp))
            LabeledRow(stringResource(R.string.dashboard_remaining_to_spend)) {
                MoneyText(summary.remainingToSpend, colorBySign = true, style = MaterialTheme.typography.bodyLarge)
            }
            summary.currentBankBalance?.let { balance ->
                Divider(modifier = Modifier.padding(vertical = 2.dp))
                LabeledRow(stringResource(R.string.dashboard_bank_balance)) { MoneyText(balance, colorBySign = true) }
                summary.freeMoney?.let { free ->
                    LabeledRow(stringResource(R.string.liberty_free_money_row_label)) {
                        MoneyText(free, colorBySign = true, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
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
