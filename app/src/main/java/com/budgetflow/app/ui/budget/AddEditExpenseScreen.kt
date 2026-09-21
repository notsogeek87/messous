package com.budgetflow.app.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.budgetflow.app.ui.components.AmountField
import com.budgetflow.app.ui.components.CategoryDropdown
import com.budgetflow.app.ui.components.FrequencySelector
import com.budgetflow.app.ui.components.ScheduleDetailsFields
import com.budgetflow.app.ui.components.ServiceLogo
import com.budgetflow.app.ui.components.ServiceSuggestionsPanel
import com.budgetflow.app.ui.components.dailyEquivalent
import com.budgetflow.app.ui.components.formatMoney
import com.budgetflow.app.ui.components.monthlyEquivalent
import com.budgetflow.app.ui.components.toAmountOrNull
import com.budgetflow.app.ui.theme.NegativeRed
import com.budgetflow.engine.model.Frequency

/** "Ajouter une dépense fixe" - same full-page treatment as [AddEditIncomeScreen], with a category. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(expenseId: Long?, onDone: () -> Unit) {
    val viewModel: AddEditExpenseViewModel = viewModel(
        factory = simpleViewModelFactory {
            AddEditExpenseViewModel(
                expenseId,
                ServiceLocator.recurringExpenseRepository,
                ServiceLocator.categoryRepository,
                ServiceLocator.serviceCatalog,
                ServiceLocator.serviceCategoryMatcher
            )
        }
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSaved) { if (state.isSaved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (expenseId == null) R.string.budget_add_expense else R.string.action_edit)) },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val recognizedService = state.recognizedService
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = state.label,
                    onValueChange = viewModel::updateLabel,
                    label = { Text(stringResource(R.string.budget_label_name)) },
                    placeholder = { Text(stringResource(R.string.budget_expense_label_hint)) },
                    leadingIcon = if (recognizedService != null) {
                        { ServiceLogo(service = recognizedService, size = 28.dp) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                ServiceSuggestionsPanel(matches = state.suggestions, onSelect = viewModel::selectSuggestion)
            }

            AmountField(
                value = state.amount,
                onValueChange = viewModel::updateAmount,
                modifier = Modifier.fillMaxWidth()
            )

            CategoryDropdown(
                categories = state.categories,
                selectedCategoryId = state.categoryId,
                onCategorySelected = viewModel::updateCategory,
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.budget_frequency_section), style = MaterialTheme.typography.titleSmall)
                FrequencySelector(frequency = state.frequency, onFrequencyChange = viewModel::updateFrequency)
                ScheduleDetailsFields(
                    frequency = state.frequency,
                    dayOfMonth = state.dayOfMonth, onDayOfMonthChange = viewModel::updateDayOfMonth,
                    dayOfWeek = state.dayOfWeek, onDayOfWeekChange = viewModel::updateDayOfWeek,
                    monthOfYear = state.monthOfYear, onMonthOfYearChange = viewModel::updateMonthOfYear
                )
            }

            ActiveSwitchRow(state.isActive, viewModel::updateIsActive)

            state.amount.toAmountOrNull()?.takeIf { it > 0.0 }?.let { amount ->
                ExpenseImpactCard(amount, state.frequency)
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun ExpenseImpactCard(amount: Double, frequency: Frequency) {
    Card(colors = CardDefaults.cardColors(containerColor = NegativeRed.copy(alpha = 0.10f)), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (frequency == Frequency.ONE_TIME) {
                stringResource(R.string.budget_impact_one_time)
            } else {
                stringResource(
                    R.string.budget_impact_expense,
                    formatMoney(dailyEquivalent(amount, frequency)),
                    formatMoney(monthlyEquivalent(amount, frequency))
                )
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(16.dp)
        )
    }
}
