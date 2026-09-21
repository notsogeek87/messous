package com.budgetflow.app.ui.budget

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetflow.app.R
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.di.simpleViewModelFactory
import com.budgetflow.app.domain.model.Income
import com.budgetflow.app.domain.model.RecurringExpense
import com.budgetflow.app.domain.model.SavingsGoal
import com.budgetflow.app.domain.model.VariableBudget
import com.budgetflow.app.ui.components.CategoryIcons
import com.budgetflow.app.ui.components.EmptyState
import com.budgetflow.app.ui.components.MoneyText
import com.budgetflow.app.ui.components.deleteWithUndo
import com.budgetflow.app.ui.components.frequencyLabel
import kotlinx.coroutines.CoroutineScope

private val tabTitles = listOf(
    R.string.budget_tab_incomes,
    R.string.budget_tab_expenses,
    R.string.budget_tab_variable,
    R.string.budget_tab_goals
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    initialTab: Int = 0,
    onAddIncome: () -> Unit,
    onEditIncome: (Long) -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit
) {
    val viewModel: BudgetViewModel = viewModel(
        factory = simpleViewModelFactory {
            BudgetViewModel(
                ServiceLocator.incomeRepository,
                ServiceLocator.recurringExpenseRepository,
                ServiceLocator.variableBudgetRepository,
                ServiceLocator.savingsGoalRepository,
                ServiceLocator.categoryRepository,
                ServiceLocator.accountRepository,
                ServiceLocator.transactionRepository
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    var tab by remember { mutableIntStateOf(initialTab.coerceIn(0, tabTitles.lastIndex)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.action_undo)
    // Resolved once in composition, then plain String.format inside click handlers - stringResource
    // itself can't be called from a non-@Composable lambda like onDelete.
    val itemDeletedFormat = stringResource(R.string.item_deleted_format)
    fun itemDeletedMessage(label: String) = itemDeletedFormat.format(label)

    var editingEnvelope by remember { mutableStateOf<VariableBudget?>(null) }
    var showEnvelopeDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var showGoalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.budget_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                when (tab) {
                    0 -> onAddIncome()
                    1 -> onAddExpense()
                    2 -> { editingEnvelope = null; showEnvelopeDialog = true }
                    3 -> { editingGoal = null; showGoalDialog = true }
                }
            }) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add)) }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                tabTitles.forEachIndexed { index, titleRes ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(stringResource(titleRes)) })
                }
            }

            when (tab) {
                0 -> IncomesTab(
                    incomes = state.incomes,
                    onAdd = onAddIncome,
                    onEdit = { onEditIncome(it.id) },
                    onDelete = { income ->
                        val message = itemDeletedMessage(income.label)
                        scope.deleteIncomeWithUndo(income, message, snackbarHostState, undoLabel, viewModel)
                    }
                )
                1 -> ExpensesTab(
                    expenses = state.expenses,
                    categories = state.categories,
                    onAdd = onAddExpense,
                    onEdit = { onEditExpense(it.id) },
                    onDelete = { expense ->
                        val message = itemDeletedMessage(expense.label)
                        scope.deleteExpenseWithUndo(expense, message, snackbarHostState, undoLabel, viewModel)
                    }
                )
                2 -> EnvelopesTab(
                    envelopes = state.envelopes,
                    onAdd = { editingEnvelope = null; showEnvelopeDialog = true },
                    onEdit = { editingEnvelope = it; showEnvelopeDialog = true },
                    onDelete = { budget ->
                        val message = itemDeletedMessage(budget.label)
                        scope.deleteEnvelopeWithUndo(budget, message, snackbarHostState, undoLabel, viewModel)
                    }
                )
                3 -> GoalsTab(
                    goals = state.goals,
                    onAdd = { editingGoal = null; showGoalDialog = true },
                    onEdit = { editingGoal = it; showGoalDialog = true },
                    onDelete = { goal ->
                        val message = itemDeletedMessage(goal.label)
                        scope.deleteGoalWithUndo(goal, message, snackbarHostState, undoLabel, viewModel)
                    }
                )
            }
        }
    }

    if (showEnvelopeDialog) {
        EnvelopeDialog(
            initial = editingEnvelope,
            categories = state.categories,
            onDismiss = { showEnvelopeDialog = false },
            onSave = { viewModel.saveEnvelope(it); showEnvelopeDialog = false }
        )
    }
    if (showGoalDialog) {
        GoalDialog(
            initial = editingGoal,
            onDismiss = { showGoalDialog = false },
            onSave = { viewModel.saveGoal(it); showGoalDialog = false }
        )
    }
}

private fun CoroutineScope.deleteIncomeWithUndo(income: Income, message: String, snackbarHostState: SnackbarHostState, undoLabel: String, viewModel: BudgetViewModel) =
    deleteWithUndo(income, message, undoLabel, snackbarHostState, viewModel::deleteIncomeSuspending, viewModel::restoreIncomeSuspending)

private fun CoroutineScope.deleteExpenseWithUndo(expense: RecurringExpense, message: String, snackbarHostState: SnackbarHostState, undoLabel: String, viewModel: BudgetViewModel) =
    deleteWithUndo(expense, message, undoLabel, snackbarHostState, viewModel::deleteExpenseSuspending, viewModel::restoreExpenseSuspending)

private fun CoroutineScope.deleteEnvelopeWithUndo(budget: VariableBudget, message: String, snackbarHostState: SnackbarHostState, undoLabel: String, viewModel: BudgetViewModel) =
    deleteWithUndo(budget, message, undoLabel, snackbarHostState, viewModel::deleteEnvelopeSuspending, viewModel::restoreEnvelopeSuspending)

private fun CoroutineScope.deleteGoalWithUndo(goal: SavingsGoal, message: String, snackbarHostState: SnackbarHostState, undoLabel: String, viewModel: BudgetViewModel) =
    deleteWithUndo(goal, message, undoLabel, snackbarHostState, viewModel::deleteGoalSuspending, viewModel::restoreGoalSuspending)

@Composable
private fun IncomesTab(incomes: List<Income>, onAdd: () -> Unit, onEdit: (Income) -> Unit, onDelete: (Income) -> Unit) {
    if (incomes.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.TrendingUp,
            title = stringResource(R.string.budget_incomes_empty_title),
            body = stringResource(R.string.budget_incomes_empty_body),
            actionLabel = stringResource(R.string.budget_add_income),
            onAction = onAdd,
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(incomes, key = { it.id }) { income ->
            Card(modifier = Modifier.fillMaxWidth().clickableCard { onEdit(income) }) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(income.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            frequencyLabel(income.frequency, income.dayOfMonth, income.dayOfWeek, income.monthOfYear, income.oneTimeDate) +
                                if (!income.isActive) " · " + stringResource(R.string.budget_status_inactive) else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MoneyText(income.amount)
                        IconButton(onClick = { onDelete(income) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete_item, income.label))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpensesTab(
    expenses: List<RecurringExpense>,
    categories: List<com.budgetflow.app.domain.model.Category>,
    onAdd: () -> Unit,
    onEdit: (RecurringExpense) -> Unit,
    onDelete: (RecurringExpense) -> Unit
) {
    if (expenses.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.TrendingDown,
            title = stringResource(R.string.budget_expenses_empty_title),
            body = stringResource(R.string.budget_expenses_empty_body),
            actionLabel = stringResource(R.string.budget_add_expense),
            onAction = onAdd,
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(expenses, key = { it.id }) { expense ->
            val category = categories.firstOrNull { it.id == expense.categoryId }
            Card(modifier = Modifier.fillMaxWidth().clickableCard { onEdit(expense) }) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(CategoryIcons.of(category?.icon), contentDescription = category?.name, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(expense.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                frequencyLabel(expense.frequency, expense.dayOfMonth, expense.dayOfWeek, expense.monthOfYear, expense.oneTimeDate) +
                                    if (!expense.isActive) " · " + stringResource(R.string.budget_status_inactive) else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MoneyText(-expense.amount, colorBySign = true)
                        IconButton(onClick = { onDelete(expense) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete_item, expense.label))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnvelopesTab(envelopes: List<EnvelopeItem>, onAdd: () -> Unit, onEdit: (VariableBudget) -> Unit, onDelete: (VariableBudget) -> Unit) {
    if (envelopes.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Savings,
            title = stringResource(R.string.budget_envelopes_empty_title),
            body = stringResource(R.string.budget_envelopes_empty_body),
            actionLabel = stringResource(R.string.budget_add_envelope),
            onAction = onAdd,
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(envelopes, key = { it.budget.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth().clickableCard { onEdit(item.budget) }) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(item.budget.label, style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { onDelete(item.budget) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete_item, item.budget.label))
                        }
                    }
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${stringResource(R.string.budget_spent)}: ${MoneyTextValue(item.spentSoFar)} / ${MoneyTextValue(item.budget.monthlyLimit)}", style = MaterialTheme.typography.bodySmall)
                        MoneyText(item.remaining, colorBySign = true, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsTab(goals: List<SavingsGoal>, onAdd: () -> Unit, onEdit: (SavingsGoal) -> Unit, onDelete: (SavingsGoal) -> Unit) {
    if (goals.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Savings,
            title = stringResource(R.string.budget_goals_empty_title),
            body = stringResource(R.string.budget_goals_empty_body),
            actionLabel = stringResource(R.string.budget_add_goal),
            onAction = onAdd,
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(goals, key = { it.id }) { goal ->
            Card(modifier = Modifier.fillMaxWidth().clickableCard { onEdit(goal) }) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(goal.label, style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { onDelete(goal) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete_item, goal.label))
                        }
                    }
                    LinearProgressIndicator(
                        progress = { goal.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${MoneyTextValue(goal.currentAmount)} / ${MoneyTextValue(goal.targetAmount)}", style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.goal_progress, goal.progressPercent), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoneyTextValue(amount: Double): String = com.budgetflow.app.ui.components.formatMoney(amount)

private fun Modifier.clickableCard(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
