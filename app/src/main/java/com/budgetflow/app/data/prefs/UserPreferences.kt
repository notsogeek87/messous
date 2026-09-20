package com.budgetflow.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "budgetflow_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Small local-only settings: nothing here is a financial figure, and none of
 * it ever leaves the device.
 */
class UserPreferences(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
    }

    val isOnboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    val dynamicColorEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    val biometricLockEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.BIOMETRIC_LOCK] ?: false }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
    }
}
