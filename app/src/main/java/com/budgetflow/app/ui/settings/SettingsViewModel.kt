package com.budgetflow.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.data.prefs.ThemeMode
import com.budgetflow.app.data.prefs.UserPreferences
import com.budgetflow.app.domain.repository.BackupRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val biometricLockEnabled: Boolean = false
)

class SettingsViewModel(
    private val preferences: UserPreferences,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.themeMode,
        preferences.dynamicColorEnabled,
        preferences.biometricLockEnabled
    ) { theme, dynamic, biometric ->
        SettingsUiState(themeMode = theme, dynamicColorEnabled = dynamic, biometricLockEnabled = biometric)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { preferences.setThemeMode(mode) }
    fun setDynamicColorEnabled(enabled: Boolean) = viewModelScope.launch { preferences.setDynamicColorEnabled(enabled) }
    fun setBiometricLockEnabled(enabled: Boolean) = viewModelScope.launch { preferences.setBiometricLockEnabled(enabled) }

    suspend fun exportJson(): String = backupRepository.exportToJson()

    suspend fun importJson(json: String): Result<Unit> = runCatching { backupRepository.importFromJson(json) }
}
