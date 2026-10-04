package com.budgetflow.app.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lielu.githubupdater.UpdateInfo
import com.lielu.githubupdater.UpdateManager
import com.lielu.githubupdater.UpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Vérifie les mises à jour à chaque ouverture de l'app et pilote la fenêtre qui les propose.
 * L'état brut est celui de [UpdateManager], partagé avec l'écran Réglages (même instance, portée Activity).
 */
class AppUpdateViewModel(
    private val updateManager: UpdateManager,
    private val updatesEnabled: Boolean,
) : ViewModel() {
    val state = updateManager.state

    /** `false` : applicationId à suffixe, aucune mise à jour possible depuis la release de production. */
    val enabled: Boolean get() = updatesEnabled

    private val _dismissed = MutableStateFlow(false)

    /** `true` une fois « Plus tard » touché : la fenêtre revient à la prochaine ouverture de l'app. */
    val dismissed: StateFlow<Boolean> = _dismissed

    private val _userStarted = MutableStateFlow(false)

    /** `true` dès que l'utilisateur a agi (« Installer », « Rechercher ») : seulement alors on lui montre une erreur. */
    val userStarted: StateFlow<Boolean> = _userStarted

    /** `false` tant qu'Android n'a pas autorisé l'app à installer des applications (étape à expliquer). */
    fun canInstallPackages(): Boolean = updateManager.canInstallPackages()

    /**
     * Appelée à chaque passage de l'app au premier plan. `force = true` : sans cela la bibliothèque réutilise
     * sa réponse précédente (« à jour ») jusqu'à `checkIntervalHours`, et une release publiée entre-temps
     * n'est pas vue. Une requête GitHub par ouverture reste très en dessous du quota (60/h).
     */
    fun checkOnOpen() {
        if (!updatesEnabled) return
        val s = state.value
        if (s is UpdateState.Downloading || s is UpdateState.Installing) return
        // Chaque ouverture réaffiche une mise à jour encore en attente (fermée par « Plus tard »).
        _dismissed.value = false
        _userStarted.value = false
        // Ne pas écraser une fenêtre déjà affichée (mise à jour proposée ou téléchargée), ni un téléchargement en cours.
        if (s !is UpdateState.Idle && s !is UpdateState.UpToDate && s !is UpdateState.Error) return
        // Les erreurs (hors ligne, quota GitHub…) sont publiées dans `state` ; ici on reste silencieux.
        viewModelScope.launch { runCatching { updateManager.checkForUpdate(force = true) } }
    }

    /** Bouton « Rechercher une mise à jour » des Réglages : même vérification, mais les erreurs sont montrées. */
    fun checkNow() {
        if (!updatesEnabled) return
        val s = state.value
        if (s is UpdateState.Checking || s is UpdateState.Downloading || s is UpdateState.Installing) return
        _dismissed.value = false
        _userStarted.value = true
        if (s is UpdateState.UpdateAvailable || s is UpdateState.Downloaded) return // la fenêtre se réaffiche
        viewModelScope.launch { runCatching { updateManager.checkForUpdate(force = true) } }
    }

    fun onDismiss() {
        _dismissed.value = true
    }

    fun onInstall(update: UpdateInfo) {
        _userStarted.value = true
        viewModelScope.launch {
            runCatching {
                val apk = updateManager.downloadUpdate(update)
                install(apk)
            }
        }
    }

    /** Relance l'installation d'un APK déjà téléchargé, typiquement au retour des réglages Android. */
    fun onInstallDownloaded(apk: File) {
        _userStarted.value = true
        runCatching { install(apk) }
    }

    private fun install(apk: File) {
        if (updateManager.canInstallPackages()) {
            updateManager.installUpdate(apk)
        } else {
            updateManager.openInstallPermissionSettings()
        }
    }
}
