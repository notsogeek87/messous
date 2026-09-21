package com.budgetflow.app.data.repository

import androidx.room.withTransaction
import com.budgetflow.app.data.local.BudgetFlowDatabase
import com.budgetflow.app.data.local.entity.ProfileEntity
import com.budgetflow.app.data.prefs.UserPreferences
import com.budgetflow.app.data.profile.CurrentProfileProvider
import com.budgetflow.app.domain.model.Profile
import com.budgetflow.app.domain.repository.CategoryRepository
import com.budgetflow.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(
    private val database: BudgetFlowDatabase,
    private val preferences: UserPreferences,
    private val currentProfile: CurrentProfileProvider,
    private val categoryRepository: CategoryRepository
) : ProfileRepository {
    private val profileDao = database.profileDao()

    override fun observeProfiles(): Flow<List<Profile>> =
        profileDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeCurrentProfileId(): Flow<Long> = currentProfile.currentProfileId

    override suspend fun setCurrentProfile(id: Long) = preferences.setCurrentProfileId(id)

    override suspend fun addProfile(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1
        val nextOrder = (profileDao.getAll().maxOfOrNull { it.sortOrder } ?: -1) + 1
        val id = profileDao.upsert(ProfileEntity(name = trimmed, sortOrder = nextOrder))
        categoryRepository.seedDefaultsForProfile(id)
        return id
    }

    override suspend fun rename(profile: Profile, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        profileDao.rename(profile.id, trimmed)
    }

    override suspend fun delete(profileId: Long): Boolean = database.withTransaction {
        if (profileDao.getAll().size <= 1) return@withTransaction false
        database.accountDao().deleteByProfile(profileId)
        database.categoryDao().deleteByProfile(profileId)
        database.incomeDao().deleteByProfile(profileId)
        database.recurringExpenseDao().deleteByProfile(profileId)
        database.variableBudgetDao().deleteByProfile(profileId)
        database.transactionDao().deleteByProfile(profileId)
        database.savingsGoalDao().deleteByProfile(profileId)
        profileDao.deleteById(profileId)
        true
    }
}

fun ProfileEntity.toDomain() = Profile(id, name, sortOrder)
