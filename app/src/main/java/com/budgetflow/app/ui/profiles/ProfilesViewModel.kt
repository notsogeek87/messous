package com.budgetflow.app.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.model.Profile
import com.budgetflow.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfilesUiState(
    val profiles: List<Profile> = emptyList(),
    val currentProfileId: Long = -1L
) {
    val canDelete: Boolean get() = profiles.size > 1
}

class ProfilesViewModel(private val profileRepository: ProfileRepository) : ViewModel() {

    val uiState: StateFlow<ProfilesUiState> = combine(
        profileRepository.observeProfiles(),
        profileRepository.observeCurrentProfileId()
    ) { profiles, currentId ->
        ProfilesUiState(profiles = profiles, currentProfileId = currentId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfilesUiState())

    fun addProfile(name: String) = viewModelScope.launch { profileRepository.addProfile(name) }

    fun rename(profile: Profile, newName: String) = viewModelScope.launch { profileRepository.rename(profile, newName) }

    fun select(id: Long) = viewModelScope.launch { profileRepository.setCurrentProfile(id) }

    fun delete(profile: Profile) = viewModelScope.launch { profileRepository.delete(profile.id) }
}
