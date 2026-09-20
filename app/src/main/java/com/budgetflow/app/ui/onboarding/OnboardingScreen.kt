package com.budgetflow.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetflow.app.R
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.di.simpleViewModelFactory
import com.budgetflow.app.domain.model.RecurringExpense
import com.budgetflow.app.domain.model.VariableBudget
import com.budgetflow.app.ui.components.AmountField
import com.budgetflow.app.ui.components.CategoryDropdown
import com.budgetflow.app.ui.components.LabeledRow
import com.budgetflow.app.ui.components.MoneyText
import com.budgetflow.app.ui.components.SectionCard

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val viewModel: OnboardingViewModel = viewModel(
        factory = simpleViewModelFactory {
            OnboardingViewModel(
                ServiceLocator.categoryRepository,
                ServiceLocator.incomeRepository,
                ServiceLocator.recurringExpenseRepository,
                ServiceLocator.variableBudgetRepository,
                ServiceLocator.accountRepository,
                ServiceLocator.preferences
            )
        }
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onFinished()
    }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            if (state.step != ONBOARDING_STEP_DONE) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = viewModel::skip) { Text(stringResource(R.string.onboarding_skip)) }
                }
            }

            StepDots(step = state.step, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (state.step) {
                    ONBOARDING_STEP_WELCOME -> WelcomeStep()
                    ONBOARDING_STEP_PRIVACY -> PrivacyStep()
                    ONBOARDING_STEP_INCOME -> IncomeStep(state, viewModel)
                    ONBOARDING_STEP_EXPENSES -> ExpensesStep(state, viewModel)
                    ONBOARDING_STEP_VARIABLE -> VariableStep(state, viewModel)
                    ONBOARDING_STEP_DONE -> DoneStep(state)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (state.step > ONBOARDING_STEP_WELCOME) {
                    OutlinedButton(onClick = viewModel::previousStep) { Text(stringResource(R.string.action_cancel)) }
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }
                Button(onClick = { if (state.step == ONBOARDING_STEP_DONE) viewModel.finish() else viewModel.nextStep() }) {
                    Text(
                        stringResource(
                            if (state.step == ONBOARDING_STEP_DONE) R.string.onboarding_finish else R.string.onboarding_next
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StepDots(step: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(ONBOARDING_STEP_DONE + 1) { index ->
            val color = if (index == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun WelcomeStep() {
    CenteredMessage(icon = Icons.Filled.WavingHand, title = stringResource(R.string.onboarding_welcome_title), body = stringResource(R.string.onboarding_welcome_body))
}

@Composable
private fun PrivacyStep() {
    CenteredMessage(icon = Icons.Filled.Lock, title = stringResource(R.string.onboarding_privacy_title), body = stringResource(R.string.onboarding_privacy_body))
}

@Composable
private fun CenteredMessage(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IncomeStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.onboarding_income_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onboarding_income_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = state.incomeLabel,
            onValueChange = viewModel::updateIncomeLabel,
            label = { Text(stringResource(R.string.transaction_description)) },
            modifier = Modifier.fillMaxWidth()
        )
        AmountField(value = state.incomeAmount, onValueChange = viewModel::updateIncomeAmount, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ExpensesStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.onboarding_expenses_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onboarding_expenses_body), color = MaterialTheme.colorScheme.onSurfaceVariant)

        LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.pendingExpenses) { expense ->
                PendingItemRow(label = expense.label, amount = expense.amount, onRemove = { viewModel.removeExpense(expense) })
            }
        }

        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text(stringResource(R.string.category_name)) }, modifier = Modifier.fillMaxWidth())
        AmountField(value = amount, onValueChange = { amount = it }, modifier = Modifier.fillMaxWidth())
        CategoryDropdown(categories = state.categories, selectedCategoryId = categoryId, onCategorySelected = { categoryId = it }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(
            onClick = {
                viewModel.addExpense(label, amount, categoryId, dayOfMonth = 5)
                label = ""; amount = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.onboarding_add_expense)) }
    }
}

@Composable
private fun VariableStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    var label by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.onboarding_variable_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onboarding_variable_body), color = MaterialTheme.colorScheme.onSurfaceVariant)

        LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.pendingEnvelopes) { envelope ->
                PendingItemRow(label = envelope.label, amount = envelope.monthlyLimit, onRemove = { viewModel.removeEnvelope(envelope) })
            }
        }

        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text(stringResource(R.string.category_name)) }, modifier = Modifier.fillMaxWidth())
        AmountField(value = limit, onValueChange = { limit = it }, label = stringResource(R.string.budget_monthly_limit), modifier = Modifier.fillMaxWidth())
        CategoryDropdown(categories = state.categories, selectedCategoryId = categoryId, onCategorySelected = { categoryId = it }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(
            onClick = {
                viewModel.addEnvelope(label, categoryId, limit)
                label = ""; limit = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.onboarding_add_envelope)) }
    }
}

@Composable
private fun PendingItemRow(label: String, amount: Double, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label)
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoneyText(amount = amount)
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, contentDescription = null) }
            }
        }
    }
}

@Composable
private fun DoneStep(state: OnboardingUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Filled.Savings, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.onboarding_done_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)

        state.previewSummary?.let { summary ->
            SectionCard(title = stringResource(R.string.dashboard_this_month)) {
                LabeledRow(stringResource(R.string.dashboard_available_budget)) { MoneyText(summary.availableBudget, colorBySign = true) }
                LabeledRow(stringResource(R.string.dashboard_remaining_to_spend)) { MoneyText(summary.remainingToSpend, colorBySign = true) }
            }
        }
    }
}
