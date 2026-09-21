package com.budgetflow.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.budgetflow.app.R
import com.budgetflow.app.di.ServiceLocator
import kotlinx.coroutines.launch

/**
 * The one profile switcher for the whole app (spec: "un sélecteur de profil qui s'applique sur
 * toute l'appli"). Lives on the home screen only, but switching here changes what every other
 * screen shows since all data repositories read the same [com.budgetflow.app.data.profile.CurrentProfileProvider].
 */
@Composable
fun ProfileSelector(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val profiles by ServiceLocator.profileRepository.observeProfiles().collectAsState(initial = emptyList())
    val currentId by ServiceLocator.profileRepository.observeCurrentProfileId().collectAsState(initial = -1L)
    var expanded by remember { mutableStateOf(false) }
    val currentName = profiles.firstOrNull { it.id == currentId }?.name ?: stringResource(R.string.profile_selector_label)

    Box(modifier = modifier) {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(currentName) },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(profile.name) },
                    leadingIcon = if (profile.id == currentId) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else null,
                    onClick = {
                        expanded = false
                        scope.launch { ServiceLocator.profileRepository.setCurrentProfile(profile.id) }
                    }
                )
            }
        }
    }
}
