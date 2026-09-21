package com.budgetflow.app.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetflow.app.R
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.di.simpleViewModelFactory
import com.budgetflow.app.domain.model.TransactionType
import com.budgetflow.app.ui.components.AccountDropdown
import com.budgetflow.app.ui.components.AmountField
import com.budgetflow.app.ui.components.CategoryDropdown
import com.budgetflow.app.ui.components.ServiceLogo
import com.budgetflow.app.ui.components.ServiceSuggestionsPanel
import com.budgetflow.app.ui.components.isInvalidAmount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(transactionId: Long?, onDone: () -> Unit) {
    val viewModel: AddEditTransactionViewModel = viewModel(
        factory = simpleViewModelFactory {
            AddEditTransactionViewModel(
                transactionId,
                ServiceLocator.transactionRepository,
                ServiceLocator.categoryRepository,
                ServiceLocator.accountRepository,
                ServiceLocator.serviceCatalog,
                ServiceLocator.serviceCategoryMatcher
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isEditing = transactionId != null

    LaunchedEffect(state.isSaved, state.isDeleted) {
        if (state.isSaved || state.isDeleted) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isEditing) R.string.action_edit else R.string.transactions_add)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.transaction_delete))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.transaction_type))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.type == TransactionType.EXPENSE,
                        onClick = { viewModel.updateType(TransactionType.EXPENSE) },
                        label = { Text(stringResource(R.string.transaction_type_expense)) }
                    )
                    FilterChip(
                        selected = state.type == TransactionType.INCOME,
                        onClick = { viewModel.updateType(TransactionType.INCOME) },
                        label = { Text(stringResource(R.string.transaction_type_income)) }
                    )
                }
            }

            val recognizedService = state.recognizedService
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text(stringResource(R.string.transaction_service_name)) },
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
                isError = state.amount.isInvalidAmount(),
                supportingText = if (state.amount.isInvalidAmount()) stringResource(R.string.form_error_amount) else null,
                modifier = Modifier.fillMaxWidth()
            )

            CategoryDropdown(
                categories = state.categories,
                selectedCategoryId = state.categoryId,
                onCategorySelected = viewModel::updateCategory,
                modifier = Modifier.fillMaxWidth()
            )

            AccountDropdown(
                accounts = state.accounts,
                selectedAccountId = state.accountId,
                onAccountSelected = viewModel::updateAccount,
                isError = state.accounts.isEmpty(),
                supportingText = if (state.accounts.isEmpty()) stringResource(R.string.form_error_no_account) else null,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("${stringResource(R.string.transaction_date)}: ${state.date.format(dateFormatter)}")
            }

            Button(onClick = viewModel::save, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.transaction_save))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.updateDate(date)
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.transaction_delete_confirm_title)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.delete() }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
