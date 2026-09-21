package com.budgetflow.app.ui.hub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetflow.app.R
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.di.simpleViewModelFactory
import com.budgetflow.app.ui.components.AmountField
import com.budgetflow.app.ui.components.LabeledRow
import com.budgetflow.app.ui.components.MoneyText
import com.budgetflow.app.ui.components.SectionCard
import com.budgetflow.app.ui.components.formatMoney
import com.budgetflow.app.ui.components.toAmountOrNull

/**
 * "Mon budget" (spec §10.2): the hub that replaced "Moi". Two things only - the plan (what's
 * decided: revenus, dépenses fixes, enveloppes, objectifs, seuil de sécurité) and the money
 * (what's actually happening: transactions, comptes, statistiques) - with app configuration moved
 * out behind the gear icon instead of sharing this list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonBudgetScreen(
    onOpenSettings: () -> Unit,
    onOpenIncomes: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenEnvelopes: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenStatistics: () -> Unit
) {
    val viewModel: MonBudgetViewModel = viewModel(
        factory = simpleViewModelFactory {
            MonBudgetViewModel(
                ServiceLocator.dashboardUseCase,
                ServiceLocator.incomeRepository,
                ServiceLocator.recurringExpenseRepository,
                ServiceLocator.variableBudgetRepository,
                ServiceLocator.savingsGoalRepository,
                ServiceLocator.transactionRepository,
                ServiceLocator.preferences
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    var showThresholdDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_me)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val summary = state.summary

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (summary != null) {
                item {
                    SectionCard(title = stringResource(R.string.dashboard_this_month), modifier = Modifier.padding(16.dp)) {
                        LabeledRow(stringResource(R.string.dashboard_income)) { MoneyText(summary.totalIncome, colorBySign = true) }
                        LabeledRow(stringResource(R.string.dashboard_fixed_expenses)) { MoneyText(-summary.totalFixedExpenses, colorBySign = true) }
                        LabeledRow(stringResource(R.string.dashboard_variable_budgets)) { MoneyText(-summary.totalVariableBudgetAllocated, colorBySign = true) }
                        if (summary.plannedSavings > 0.0) {
                            LabeledRow(stringResource(R.string.dashboard_planned_savings)) { MoneyText(-summary.plannedSavings, colorBySign = true) }
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        LabeledRow(stringResource(R.string.dashboard_remaining_to_spend)) {
                            MoneyText(summary.remainingToSpend, colorBySign = true, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.hub_section_plan)) }
            item {
                HubRow(
                    icon = Icons.Filled.TrendingUp,
                    title = stringResource(R.string.budget_tab_incomes),
                    subtitle = stringResource(R.string.hub_incomes_subtitle, formatMoney(summary?.totalIncome ?: 0.0)),
                    onClick = onOpenIncomes
                )
            }
            item {
                HubRow(
                    icon = Icons.Filled.TrendingDown,
                    title = stringResource(R.string.budget_tab_expenses),
                    subtitle = stringResource(R.string.hub_expenses_subtitle, formatMoney(summary?.totalFixedExpenses ?: 0.0)),
                    onClick = onOpenExpenses
                )
            }
            item {
                HubRow(
                    icon = Icons.Filled.Savings,
                    title = stringResource(R.string.budget_tab_variable),
                    subtitle = if (state.envelopeCount > 0) {
                        stringResource(
                            R.string.hub_envelopes_subtitle,
                            formatMoney(summary?.totalVariableSpent ?: 0.0),
                            formatMoney(summary?.totalVariableBudgetAllocated ?: 0.0)
                        )
                    } else {
                        stringResource(R.string.hub_envelopes_empty)
                    },
                    onClick = onOpenEnvelopes
                )
            }
            item {
                HubRow(
                    icon = Icons.Filled.EmojiEvents,
                    title = stringResource(R.string.budget_tab_goals),
                    subtitle = if (state.goalCount > 0) stringResource(R.string.hub_goals_subtitle, state.goalCount) else stringResource(R.string.hub_goals_empty),
                    onClick = onOpenGoals
                )
            }
            item {
                HubRow(
                    icon = Icons.Filled.Security,
                    title = stringResource(R.string.settings_safety_threshold),
                    subtitle = if (state.safetyThreshold > 0.0) {
                        stringResource(R.string.settings_safety_threshold_body, formatMoney(state.safetyThreshold))
                    } else {
                        stringResource(R.string.hub_threshold_not_set)
                    },
                    onClick = { showThresholdDialog = true }
                )
            }

            item { Divider() }
            item { SectionHeader(stringResource(R.string.hub_section_money)) }
            item {
                HubRow(
                    icon = Icons.Filled.Receipt,
                    title = stringResource(R.string.settings_transactions),
                    subtitle = stringResource(R.string.hub_transactions_subtitle, state.transactionsThisMonthCount),
                    onClick = onOpenTransactions
                )
            }
            item {
                HubRow(
                    icon = Icons.Filled.AccountBalance,
                    title = stringResource(R.string.settings_accounts),
                    subtitle = summary?.currentBankBalance?.let { formatMoney(it) } ?: stringResource(R.string.hub_accounts_empty),
                    onClick = onOpenAccounts
                )
            }
            item {
                HubRow(
                    icon = Icons.Filled.BarChart,
                    title = stringResource(R.string.settings_statistics),
                    subtitle = null,
                    onClick = onOpenStatistics
                )
            }
        }
    }

    if (showThresholdDialog) {
        SafetyThresholdDialog(
            initialAmount = state.safetyThreshold,
            onDismiss = { showThresholdDialog = false },
            onSave = { amount -> viewModel.setSafetyThreshold(amount); showThresholdDialog = false }
        )
    }
}

@Composable
private fun SafetyThresholdDialog(initialAmount: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var input by remember { mutableStateOf(if (initialAmount > 0.0) initialAmount.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_safety_threshold_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.settings_safety_threshold_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                AmountField(value = input, onValueChange = { input = it }, label = stringResource(R.string.settings_safety_threshold))
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(input.toAmountOrNull() ?: 0.0) }) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun HubRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
