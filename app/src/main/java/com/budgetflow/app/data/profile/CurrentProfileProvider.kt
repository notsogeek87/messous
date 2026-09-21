package com.budgetflow.app.data.profile

import com.budgetflow.app.data.local.dao.CategoryDao
import com.budgetflow.app.data.local.dao.ProfileDao
import com.budgetflow.app.data.local.entity.ProfileEntity
import com.budgetflow.app.data.prefs.UserPreferences
import com.budgetflow.app.data.repository.toEntity
import com.budgetflow.app.domain.model.DefaultCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Resolves the app-wide "current profile" every profile-scoped repository reads and writes
 * through. Every screen and repository shares this single flow instead of reading
 * [UserPreferences.currentProfileId] directly, so that:
 *  - a fresh install lazily gets its three default profiles (Perso/Pro/Commun) the first time
 *    ANY repository is collected, whichever happens first - there is no separate app-startup
 *    step to race against;
 *  - switching or deleting a profile is reflected everywhere at once ("s'applique sur toute
 *    l'appli");
 *  - if the saved choice no longer exists (its profile was deleted), everything falls back to
 *    the lowest-sortOrder profile - Perso by default - instead of showing an empty app.
 */
class CurrentProfileProvider(
    private val preferences: UserPreferences,
    private val profileDao: ProfileDao,
    private val categoryDao: CategoryDao
) {
    private val bootstrapMutex = Mutex()

    private suspend fun ensureDefaultProfiles(): List<ProfileEntity> {
        val existing = profileDao.getAll()
        if (existing.isNotEmpty()) return existing
        return bootstrapMutex.withLock {
            val recheck = profileDao.getAll()
            if (recheck.isNotEmpty()) return@withLock recheck
            val now = System.currentTimeMillis()
            val defaults = listOf(
                ProfileEntity(name = "Perso", sortOrder = 0, createdAtEpochMillis = now),
                ProfileEntity(name = "Pro", sortOrder = 1, createdAtEpochMillis = now),
                ProfileEntity(name = "Commun", sortOrder = 2, createdAtEpochMillis = now)
            )
            defaults.forEach { profile ->
                val id = profileDao.upsert(profile)
                categoryDao.insertAll(DefaultCategories.all.map { it.toEntity(id) })
            }
            profileDao.getAll()
        }
    }

    val currentProfileId: Flow<Long> =
        combine(preferences.currentProfileId, profileDao.observeAll()) { savedId, profiles -> savedId to profiles }
            .map { (savedId, profiles) ->
                val active = if (profiles.isEmpty()) ensureDefaultProfiles() else profiles
                active.firstOrNull { it.id == savedId }?.id
                    ?: active.minByOrNull { it.sortOrder }?.id
                    ?: 0L
            }
            .distinctUntilChanged()
}
