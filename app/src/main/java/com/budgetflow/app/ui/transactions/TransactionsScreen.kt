package com.budgetflow.app.ui.transactions

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetflow.app.R
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.di.simpleViewModelFactory
import com.budgetflow.app.domain.model.TransactionType
import com.budgetflow.app.ui.components.CategoryIcons
import com.budgetflow.app.ui.components.EmptyState
import com.budgetflow.app.ui.components.MoneyText
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(onAddTransaction: () -> Unit, onEditTransaction: (Long) -> Unit) {
    val viewModel: TransactionsViewModel = viewModel(
        factory = simpleViewModelFactory {
            TransactionsViewModel(ServiceLocator.transactionRepository, ServiceLocator.categoryRepository, ServiceLocator.accountRepository)
        }
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.transactions_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.transactions_add))
            }
        }
    ) { padding ->
        if (!state.isLoading && state.items.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Receipt,
                title = stringResource(R.string.transactions_empty),
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(state.items, key = { it.transaction.id }) { item ->
                TransactionRow(
                    item = item,
                    onClick = { onEditTransaction(item.transaction.id) },
                    onDelete = { viewModel.delete(item.transaction) }
                )
                Divider()
            }
        }
    }
}

@Composable
private fun TransactionRow(item: TransactionListItem, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(CategoryIcons.of(item.category?.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f).clickable(onClick = onClick)) {
            Text(item.transaction.description.ifBlank { item.category?.name ?: "" }, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${item.transaction.date.format(dateFormatter)} · ${item.account?.name ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val signedAmount = if (item.transaction.type == TransactionType.EXPENSE) -item.transaction.amount else item.transaction.amount
        MoneyText(amount = signedAmount, colorBySign = true)
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.transaction_delete))
        }
    }
}
