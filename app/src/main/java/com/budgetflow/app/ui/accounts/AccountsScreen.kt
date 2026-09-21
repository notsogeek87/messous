package com.budgetflow.app.ui.accounts

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetflow.app.R
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.di.simpleViewModelFactory
import com.budgetflow.app.domain.model.Account
import com.budgetflow.app.ui.components.AmountField
import com.budgetflow.app.ui.components.EmptyState
import com.budgetflow.app.ui.components.MoneyText
import com.budgetflow.app.ui.components.toAmountOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(onBack: () -> Unit) {
    val viewModel: AccountsViewModel = viewModel(
        factory = simpleViewModelFactory { AccountsViewModel(ServiceLocator.accountRepository, ServiceLocator.transactionRepository) }
    )
    val accounts by viewModel.accounts.collectAsState()
    var editing by remember { mutableStateOf<Account?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<AccountItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.accounts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.accounts_add))
            }
        }
    ) { padding ->
        if (accounts.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.CreditCard,
                title = stringResource(R.string.accounts_empty_title),
                body = stringResource(R.string.accounts_empty_body),
                actionLabel = stringResource(R.string.accounts_add),
                onAction = { editing = null; showDialog = true },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts, key = { it.account.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { editing = item.account; showDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.account.name, style = MaterialTheme.typography.bodyLarge)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MoneyText(item.currentBalance, colorBySign = true)
                                IconButton(onClick = { pendingDelete = item }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.action_delete_item, item.account.name)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AccountDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { viewModel.save(it); showDialog = false }
        )
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.account_delete_confirm_title, item.account.name)) },
            text = {
                Text(
                    if (item.transactionCount > 0) {
                        stringResource(R.string.account_delete_confirm_body, item.transactionCount)
                    } else {
                        stringResource(R.string.account_delete_confirm_body_empty)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(item.account); pendingDelete = null }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

// Messous is single-currency (EUR): no per-account currency picker, just the name and the
// starting balance (spec: never show a control whose value the rest of the app cannot act on).
@Composable
private fun AccountDialog(initial: Account?, onDismiss: () -> Unit, onSave: (Account) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var balance by remember { mutableStateOf(initial?.initialBalance?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.accounts_add else R.string.action_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.account_name)) }, modifier = Modifier.fillMaxWidth())
                AmountField(value = balance, onValueChange = { balance = it }, label = stringResource(R.string.account_initial_balance), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) return@TextButton
                onSave(
                    Account(
                        id = initial?.id ?: 0,
                        name = name,
                        initialBalance = balance.toAmountOrNull() ?: 0.0,
                        currency = "EUR",
                        isArchived = initial?.isArchived ?: false
                    )
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
