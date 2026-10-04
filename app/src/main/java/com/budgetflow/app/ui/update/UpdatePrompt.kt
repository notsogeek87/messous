package com.budgetflow.app.ui.update

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetflow.app.R
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.di.simpleViewModelFactory
import com.lielu.githubupdater.UpdateError
import com.lielu.githubupdater.UpdateState

/**
 * Le [AppUpdateViewModel] de l'Activity : le même pour la fenêtre de lancement et l'écran Réglages,
 * afin qu'ils partagent l'état « fermé » et « lancé par l'utilisateur ».
 */
@Composable
fun rememberAppUpdateViewModel(): AppUpdateViewModel {
    val owner = LocalContext.current as ComponentActivity
    return viewModel(
        viewModelStoreOwner = owner,
        factory = simpleViewModelFactory { AppUpdateViewModel(ServiceLocator.updateManager, ServiceLocator.updatesEnabled) },
    )
}

/** Fenêtre proposant une mise à jour trouvée à l'ouverture, avec les étapes à suivre expliquées. */
@Composable
fun UpdatePrompt(viewModel: AppUpdateViewModel = rememberAppUpdateViewModel()) {
    val state by viewModel.state.collectAsState()
    val dismissed by viewModel.dismissed.collectAsState()
    val userStarted by viewModel.userStarted.collectAsState()
    var canInstall by remember { mutableStateOf(viewModel.canInstallPackages()) }

    // ON_START : à l'ouverture ET quand l'app, restée en mémoire, repasse au premier plan (par ex. au
    // retour des réglages Android, où l'autorisation d'installer a pu être accordée).
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        canInstall = viewModel.canInstallPackages()
        viewModel.checkOnOpen()
    }

    if (dismissed) return

    when (val s = state) {
        is UpdateState.UpdateAvailable ->
            AlertDialog(
                onDismissRequest = viewModel::onDismiss,
                title = { Text(stringResource(R.string.update_available_title)) },
                text = {
                    UpdateSteps(
                        intro = stringResource(R.string.update_available_intro, s.update.versionName),
                        steps =
                            listOf(
                                stringResource(R.string.update_step_download),
                                stringResource(R.string.update_step_allow_source),
                                stringResource(R.string.update_step_confirm),
                            ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.onInstall(s.update) }) { Text(stringResource(R.string.update_action_install)) }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onDismiss) { Text(stringResource(R.string.update_action_later)) }
                },
            )
        is UpdateState.Downloading ->
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.update_downloading_title)) },
                text = {
                    LinearProgressIndicator(
                        progress = { (s.progress.percentage ?: 0) / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {},
            )
        is UpdateState.Downloaded ->
            AlertDialog(
                onDismissRequest = viewModel::onDismiss,
                title = { Text(stringResource(R.string.update_downloaded_title)) },
                text = {
                    if (canInstall) {
                        UpdateSteps(
                            intro = stringResource(R.string.update_downloaded_intro),
                            steps = listOf(stringResource(R.string.update_downloaded_step_install)),
                        )
                    } else {
                        UpdateSteps(
                            intro = stringResource(R.string.update_permission_intro),
                            steps =
                                listOf(
                                    stringResource(R.string.update_permission_step_open),
                                    stringResource(R.string.update_permission_step_enable),
                                    stringResource(R.string.update_permission_step_retry),
                                ),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.onInstallDownloaded(s.file) }) { Text(stringResource(R.string.update_action_install)) }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onDismiss) { Text(stringResource(R.string.update_action_later)) }
                },
            )
        is UpdateState.Error ->
            if (userStarted) {
                AlertDialog(
                    onDismissRequest = viewModel::onDismiss,
                    title = { Text(stringResource(R.string.update_error_title)) },
                    text = { Text(updateErrorMessage(s.error)) },
                    confirmButton = {
                        TextButton(onClick = viewModel::onDismiss) { Text(stringResource(R.string.update_action_ok)) }
                    },
                )
            }
        else -> Unit
    }
}

@Composable
private fun UpdateSteps(
    intro: String,
    steps: List<String>,
) {
    Column {
        Text(intro, style = MaterialTheme.typography.bodyMedium)
        steps.forEachIndexed { index, step ->
            Text(
                "${index + 1}. $step",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            stringResource(R.string.update_data_kept),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
internal fun updateErrorMessage(error: UpdateError): String =
    when (error) {
        is UpdateError.NetworkError -> stringResource(R.string.update_error_network)
        is UpdateError.RateLimit -> stringResource(R.string.update_error_rate_limit)
        UpdateError.ReleaseNotFound -> stringResource(R.string.update_error_no_release)
        is UpdateError.ApkNotFound -> stringResource(R.string.update_error_no_apk)
        UpdateError.InstallationNotAllowed -> stringResource(R.string.update_error_not_allowed)
        else -> stringResource(R.string.update_error_generic, error.message ?: "")
    }
