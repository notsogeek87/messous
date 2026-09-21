package com.budgetflow.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.budgetflow.app.ui.BudgetFlowApp

/**
 * A [FragmentActivity] (rather than a plain ComponentActivity) because
 * androidx.biometric's [androidx.biometric.BiometricPrompt] requires one to
 * host the system biometric/device-credential prompt used by the optional
 * app lock (spec section 17).
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetFlowApp()
        }
    }
}
