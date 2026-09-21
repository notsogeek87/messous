package com.budgetflow.app.domain.repository

import com.budgetflow.app.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfiles(): Flow<List<Profile>>

    /** The effective current profile id: the user's saved choice, or the first profile if it was deleted or none was ever chosen. */
    fun observeCurrentProfileId(): Flow<Long>

    suspend fun setCurrentProfile(id: Long)

    /** Creates a new, empty (but pre-seeded with the starter categories) profile and returns its id. */
    suspend fun addProfile(name: String): Long

    suspend fun rename(profile: Profile, newName: String)

    /** Deletes [profileId] and every account/category/transaction/... walled off under it. Returns false, changing nothing, if it's the last remaining profile. */
    suspend fun delete(profileId: Long): Boolean
}
