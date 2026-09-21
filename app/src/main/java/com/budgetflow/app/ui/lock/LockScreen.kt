package com.budgetflow.app.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.budgetflow.app.R
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.ui.theme.negativeRed
import kotlinx.coroutines.launch

/**
 * Error codes meaning "there is now structurally no way to satisfy this prompt" - removed PIN,
 * uninstalled fingerprint, hardware gone. Never the result of a wrong fingerprint or a deliberate
 * lockout after too many tries (that stays a real refusal, on purpose) - only these mean retrying
 * can never succeed, which is the one case where offering to turn the lock back off is safe.
 */
private val NO_AUTH_METHOD_AVAILABLE = setOf(
    BiometricPrompt.ERROR_NO_BIOMETRICS,
    BiometricPrompt.ERROR_HW_NOT_PRESENT,
    BiometricPrompt.ERROR_HW_UNAVAILABLE,
    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
    BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED
)

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val activity = LocalContext.current as FragmentActivity
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var offerRescue by remember { mutableStateOf(false) }

    fun attempt() {
        errorMessage = null
        promptBiometricUnlock(
            activity = activity,
            onSuccess = onUnlocked,
            onError = { code, message ->
                // The user dismissing the sheet or tapping "cancel" isn't a failure worth
                // narrating back at them.
                val isUserDismiss = code == BiometricPrompt.ERROR_USER_CANCELED ||
                    code == BiometricPrompt.ERROR_CANCELED ||
                    code == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                if (!isUserDismiss) {
                    errorMessage = message.toString()
                    if (code in NO_AUTH_METHOD_AVAILABLE) offerRescue = true
                }
            }
        )
    }

    // Ask right away instead of waiting for a tap on every single app open (spec: one less step).
    LaunchedEffect(Unit) { attempt() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.height(56.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.unlock_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.unlock_prompt), color = MaterialTheme.colorScheme.onSurfaceVariant)
        errorMessage?.let {
            Text(it, color = negativeRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
        Button(onClick = ::attempt, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.unlock_button))
        }
        // The one deliberate way out, and only once the OS itself says no attempt can ever
        // succeed any more (PIN removed, fingerprint uninstalled...): never leave the user's own
        // data permanently unreachable because of a lock they turned on themselves. A merely
        // wrong fingerprint, or a temporary OS lockout after too many tries, never offers this.
        if (offerRescue) {
            TextButton(
                onClick = {
                    scope.launch {
                        ServiceLocator.preferences.setBiometricLockEnabled(false)
                        onUnlocked()
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.unlock_disable_lock))
            }
        }
    }
}

/** True when the device has no way at all to satisfy [BiometricPrompt] - no fingerprint/face
 * enrolled AND no PIN/pattern/password set. Enabling the lock in that state would strand the
 * user outside their own data with no way back in, so the Settings toggle checks this first. */
fun canUseBiometricLock(context: android.content.Context): Boolean =
    BiometricManager.from(context).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    ) == BiometricManager.BIOMETRIC_SUCCESS

private fun promptBiometricUnlock(activity: FragmentActivity, onSuccess: () -> Unit, onError: (Int, CharSequence) -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errorCode, errString)
            }
        }
    )
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(activity.getString(R.string.unlock_title))
        .setSubtitle(activity.getString(R.string.unlock_prompt))
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()
    prompt.authenticate(promptInfo)
}
