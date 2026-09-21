package com.budgetflow.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetflow.app.BuildConfig
import com.budgetflow.app.R
import com.budgetflow.app.data.prefs.ThemeMode
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.di.simpleViewModelFactory
import com.budgetflow.app.ui.components.AmountField
import com.budgetflow.app.ui.components.formatMoney
import com.budgetflow.app.ui.components.toAmountOrNull
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenAccounts: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenStatistics: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = simpleViewModelFactory { SettingsViewModel(ServiceLocator.preferences, ServiceLocator.backupRepository) }
    )
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var showThresholdDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = viewModel.exportJson()
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            snackbarHostState.showSnackbar(context.getString(R.string.settings_export))
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
            if (text != null) pendingImportJson = text
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item { SectionHeader(stringResource(R.string.settings_section_situation)) }
            item {
                SettingsRow(
                    icon = Icons.Filled.Receipt,
                    title = stringResource(R.string.settings_transactions),
                    onClick = onOpenTransactions
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = stringResource(R.string.settings_budgets),
                    onClick = onOpenBudget
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Filled.BarChart,
                    title = stringResource(R.string.settings_statistics),
                    onClick = onOpenStatistics
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Filled.AccountBalance,
                    title = stringResource(R.string.settings_accounts),
                    onClick = onOpenAccounts
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Filled.Category,
                    title = stringResource(R.string.settings_categories),
                    onClick = onOpenCategories
                )
            }

            item { Divider() }
            item { SectionHeader(stringResource(R.string.settings_section_freedom)) }
            item {
                SettingsRow(
                    icon = Icons.Filled.Security,
                    title = stringResource(R.string.settings_safety_threshold),
                    subtitle = stringResource(R.string.settings_safety_threshold_body, formatMoney(state.safetyThreshold)),
                    onClick = { showThresholdDialog = true }
                )
            }
            item {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_biometric_lock),
                    subtitle = stringResource(R.string.settings_biometric_lock_body),
                    checked = state.biometricLockEnabled,
                    onCheckedChange = viewModel::setBiometricLockEnabled
                )
            }

            item { Divider() }
            item { SectionHeader(stringResource(R.string.settings_section_data)) }
            item {
                SettingsRow(
                    icon = Icons.Filled.Download,
                    title = stringResource(R.string.settings_export),
                    subtitle = stringResource(R.string.settings_export_body),
                    onClick = { exportLauncher.launch("messous-export.json") }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Filled.Upload,
                    title = stringResource(R.string.settings_import),
                    subtitle = stringResource(R.string.settings_import_body),
                    onClick = { importLauncher.launch(arrayOf("application/json")) }
                )
            }

            item { Divider() }
            item { SectionHeader(stringResource(R.string.settings_section_appearance)) }
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeOption(ThemeMode.SYSTEM, state.themeMode, stringResource(R.string.settings_theme_system), viewModel::setThemeMode)
                        ThemeOption(ThemeMode.LIGHT, state.themeMode, stringResource(R.string.settings_theme_light), viewModel::setThemeMode)
                        ThemeOption(ThemeMode.DARK, state.themeMode, stringResource(R.string.settings_theme_dark), viewModel::setThemeMode)
                    }
                }
            }
            item {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_dynamic_color),
                    checked = state.dynamicColorEnabled,
                    onCheckedChange = viewModel::setDynamicColorEnabled
                )
            }

            item { Divider() }
            item { SectionHeader(stringResource(R.string.settings_section_about)) }
            item {
                Text(
                    stringResource(R.string.settings_privacy_statement),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    pendingImportJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingImportJson = null },
            title = { Text(stringResource(R.string.settings_import_confirm_title)) },
            text = { Text(stringResource(R.string.settings_import_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val result = viewModel.importJson(json)
                        pendingImportJson = null
                        snackbarHostState.showSnackbar(
                            if (result.isSuccess) context.getString(R.string.settings_import) else result.exceptionOrNull()?.message.orEmpty()
                        )
                    }
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = { TextButton(onClick = { pendingImportJson = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
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
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeOption(mode: ThemeMode, current: ThemeMode, label: String, onSelect: (ThemeMode) -> Unit) {
    FilterChip(selected = current == mode, onClick = { onSelect(mode) }, label = { Text(label) })
}
