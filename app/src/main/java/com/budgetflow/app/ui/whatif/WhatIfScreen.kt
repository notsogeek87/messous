package com.budgetflow.app.ui.whatif

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.budgetflow.app.ui.components.AmountField
import com.budgetflow.app.ui.components.AnimatedMoneyText
import com.budgetflow.app.ui.components.FreedomStateBadge
import com.budgetflow.app.ui.components.MoneyText
import com.budgetflow.app.ui.components.color
import com.budgetflow.app.ui.components.formatMoney
import com.budgetflow.app.ui.components.toAmountOrNull
import com.budgetflow.engine.model.ExpenseSimulation
import kotlinx.coroutines.launch

private enum class WhatIfMode { EXPENSE, ALLOCATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatIfScreen() {
    var mode by remember { mutableStateOf(WhatIfMode.EXPENSE) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.whatif_title)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SegmentedButton(
                    selected = mode == WhatIfMode.EXPENSE,
                    onClick = { mode = WhatIfMode.EXPENSE },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text(stringResource(R.string.whatif_mode_expense)) }
                SegmentedButton(
                    selected = mode == WhatIfMode.ALLOCATE,
                    onClick = { mode = WhatIfMode.ALLOCATE },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text(stringResource(R.string.whatif_mode_allocate)) }
            }

            when (mode) {
                WhatIfMode.EXPENSE -> ExpenseSimulatorContent(
                    modifier = Modifier.weight(1f),
                    onExpenseAdded = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
                )
                WhatIfMode.ALLOCATE -> AllocateContent(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ExpenseSimulatorContent(modifier: Modifier = Modifier, onExpenseAdded: (String) -> Unit) {
    val viewModel: WhatIfViewModel = viewModel(
        factory = simpleViewModelFactory {
            WhatIfViewModel(
                ServiceLocator.simulateExpenseUseCase,
                ServiceLocator.savingsGoalRepository,
                ServiceLocator.transactionRepository,
                ServiceLocator.accountRepository
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    val addedMessage = stringResource(R.string.whatif_added_confirmation)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AmountField(
                value = state.amountInput,
                onValueChange = viewModel::setAmountInput,
                label = stringResource(R.string.whatif_amount_label),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = state.labelInput,
                onValueChange = viewModel::setLabelInput,
                label = { Text(stringResource(R.string.whatif_label_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        val now = state.scenarios.now
        if (state.amount == null || now == null) {
            item {
                Text(
                    stringResource(R.string.whatif_empty_prompt),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        } else {
            item { ImpactCard(now) }
            item { ThresholdMessageCard(now) }
            if (state.goalImpacts.isNotEmpty()) {
                item { Text(stringResource(R.string.goal_target), style = MaterialTheme.typography.titleSmall) }
            }
            items(state.goalImpacts) { impact ->
                Text(
                    stringResource(R.string.whatif_goal_impact, impact.goal.label, impact.delayDays),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item { Text(stringResource(R.string.future_time_travel_label), style = MaterialTheme.typography.titleSmall) }
            item { ScenarioRow(stringResource(R.string.whatif_scenario_now), now) }
            state.scenarios.in15Days?.let { item { ScenarioRow(stringResource(R.string.whatif_scenario_15_days), it) } }
            state.scenarios.nextMonth?.let { item { ScenarioRow(stringResource(R.string.whatif_scenario_next_month), it) } }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = viewModel::reset, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.whatif_cancel))
                    }
                    Button(
                        onClick = { viewModel.confirmRealExpense { onExpenseAdded(addedMessage) } },
                        modifier = Modifier.weight(1f),
                        enabled = state.accounts.isNotEmpty()
                    ) { Text(stringResource(R.string.whatif_add_real_expense)) }
                }
            }
        }
    }
}

@Composable
private fun ImpactCard(simulation: ExpenseSimulation) {
    Card(
        colors = CardDefaults.cardColors(containerColor = simulation.after.freedomState.color().copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            FreedomStateBadge(simulation.after.freedomState)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.whatif_before), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(simulation.before.freeMoney ?: 0.0, style = MaterialTheme.typography.titleLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.whatif_after), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimatedMoneyText(
                        simulation.after.freeMoney ?: 0.0,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = simulation.after.freedomState.color()
                    )
                }
            }
            simulation.freedomPerDayDelta?.let { delta ->
                Text(
                    stringResource(R.string.whatif_cost_in_freedom, formatMoney(delta)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ThresholdMessageCard(simulation: ExpenseSimulation) {
    val message = simulation.amountUnderThreshold?.let { stringResource(R.string.whatif_threshold_breach, formatMoney(it)) }
        ?: stringResource(R.string.whatif_threshold_ok)
    Text(
        message,
        style = MaterialTheme.typography.bodyLarge,
        color = if (simulation.wouldBreachSafetyThreshold) simulation.after.freedomState.color() else MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun ScenarioRow(label: String, simulation: ExpenseSimulation) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        MoneyText(simulation.after.freeMoney ?: 0.0, colorBySign = true)
    }
}

// --- "Que faire de X €?" (spec section 13) ---------------------------------------------------

@Composable
private fun AllocateContent(modifier: Modifier = Modifier) {
    var amountInput by remember { mutableStateOf("") }
    val amount = amountInput.toAmountOrNull()?.takeIf { it > 0.0 }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AmountField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = stringResource(R.string.whatif_allocate_amount_label),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (amount == null) {
            item {
                Text(
                    stringResource(R.string.whatif_empty_prompt),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        } else {
            item {
                ScenarioCard(stringResource(R.string.whatif_allocate_vacation)) {
                    SplitRow(stringResource(R.string.whatif_allocate_fun), amount * 0.6)
                    SplitRow(stringResource(R.string.whatif_allocate_margin), amount * 0.4)
                }
            }
            item {
                ScenarioCard(stringResource(R.string.whatif_allocate_project)) {
                    SplitRow(stringResource(R.string.whatif_allocate_goal), amount)
                }
            }
            item {
                ScenarioCard(stringResource(R.string.whatif_allocate_security)) {
                    SplitRow(stringResource(R.string.whatif_allocate_kept), amount)
                }
            }
            item { BalancedScenarioCard(amount) }
        }
    }
}

@Composable
private fun ScenarioCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun BalancedScenarioCard(amount: Double) {
    var savingsFraction by remember(amount) { mutableFloatStateOf(0.5f) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.whatif_allocate_balanced), style = MaterialTheme.typography.titleMedium)
            SplitRow(stringResource(R.string.whatif_allocate_savings), amount * savingsFraction)
            SplitRow(stringResource(R.string.whatif_allocate_fun), amount * (1f - savingsFraction))
            Slider(
                value = savingsFraction,
                onValueChange = { savingsFraction = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SplitRow(label: String, amount: Double) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        MoneyText(amount, style = MaterialTheme.typography.bodyLarge)
    }
}
