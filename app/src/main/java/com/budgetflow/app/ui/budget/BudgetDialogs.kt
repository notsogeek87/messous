package com.budgetflow.app.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.budgetflow.app.R
import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.model.SavingsGoal
import com.budgetflow.app.domain.model.VariableBudget
import com.budgetflow.app.ui.components.AmountField
import com.budgetflow.app.ui.components.CategoryDropdown
import com.budgetflow.app.ui.components.toAmountOrNull

@Composable
fun EnvelopeDialog(initial: VariableBudget?, categories: List<Category>, onDismiss: () -> Unit, onSave: (VariableBudget) -> Unit) {
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var monthlyLimit by remember { mutableStateOf(initial?.monthlyLimit?.toString() ?: "") }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.budget_add_envelope else R.string.action_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text(stringResource(R.string.category_name)) }, modifier = Modifier.fillMaxWidth())
                AmountField(value = monthlyLimit, onValueChange = { monthlyLimit = it }, label = stringResource(R.string.budget_monthly_limit), modifier = Modifier.fillMaxWidth())
                CategoryDropdown(categories = categories, selectedCategoryId = categoryId, onCategorySelected = { categoryId = it }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = monthlyLimit.toAmountOrNull() ?: return@TextButton
                val category = categoryId ?: return@TextButton
                onSave(VariableBudget(id = initial?.id ?: 0, label = label.ifBlank { "Enveloppe" }, monthlyLimit = value, categoryId = category, isActive = initial?.isActive ?: true))
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun GoalDialog(initial: SavingsGoal?, onDismiss: () -> Unit, onSave: (SavingsGoal) -> Unit) {
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var target by remember { mutableStateOf(initial?.targetAmount?.toString() ?: "") }
    var current by remember { mutableStateOf(initial?.currentAmount?.toString() ?: "0") }
    var monthlyContribution by remember { mutableStateOf(initial?.monthlyContribution?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.budget_add_goal else R.string.action_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text(stringResource(R.string.category_name)) }, modifier = Modifier.fillMaxWidth())
                AmountField(value = target, onValueChange = { target = it }, label = stringResource(R.string.goal_target), modifier = Modifier.fillMaxWidth())
                AmountField(value = current, onValueChange = { current = it }, label = stringResource(R.string.goal_saved), modifier = Modifier.fillMaxWidth())
                AmountField(value = monthlyContribution, onValueChange = { monthlyContribution = it }, label = stringResource(R.string.dashboard_planned_savings), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val targetValue = target.toAmountOrNull() ?: return@TextButton
                onSave(
                    SavingsGoal(
                        id = initial?.id ?: 0,
                        label = label.ifBlank { "Objectif" },
                        targetAmount = targetValue,
                        currentAmount = current.toAmountOrNull() ?: 0.0,
                        monthlyContribution = monthlyContribution.toAmountOrNull() ?: 0.0,
                        isActive = initial?.isActive ?: true
                    )
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun ActiveSwitchRow(isActive: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.budget_active))
        Switch(checked = isActive, onCheckedChange = onChange)
    }
}
