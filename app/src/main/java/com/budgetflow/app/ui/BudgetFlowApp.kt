package com.budgetflow.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.ui.lock.LockScreen
import com.budgetflow.app.ui.navigation.BudgetFlowNavHost
import com.budgetflow.app.ui.onboarding.OnboardingScreen
import com.budgetflow.app.ui.theme.BudgetFlowTheme

/**
 * The whole app in three possible states: onboarding (first launch), the
 * optional biometric lock gate, or the main navigation shell.
 */
@Composable
fun BudgetFlowApp() {
    val preferences = ServiceLocator.preferences
    val themeMode by preferences.themeMode.collectAsState(initial = com.budgetflow.app.data.prefs.ThemeMode.SYSTEM)
    val dynamicColor by preferences.dynamicColorEnabled.collectAsState(initial = true)
    val onboardingDone by preferences.isOnboardingDone.collectAsState(initial = null)
    val biometricLockEnabled by preferences.biometricLockEnabled.collectAsState(initial = false)

    var isUnlocked by remember { mutableStateOf(false) }
    LaunchedEffect(biometricLockEnabled) {
        if (!biometricLockEnabled) isUnlocked = true
    }

    BudgetFlowTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when {
                onboardingDone == null -> Box(modifier = Modifier.fillMaxSize())
                onboardingDone == false -> OnboardingScreen(onFinished = { /* isOnboardingDone flow updates automatically */ })
                biometricLockEnabled && !isUnlocked -> LockScreen(onUnlocked = { isUnlocked = true })
                else -> BudgetFlowNavHost()
            }
        }
    }
}
