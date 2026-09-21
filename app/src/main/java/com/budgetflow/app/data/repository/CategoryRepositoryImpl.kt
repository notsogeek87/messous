package com.budgetflow.app.data.repository

import com.budgetflow.app.data.local.dao.CategoryDao
import com.budgetflow.app.data.profile.CurrentProfileProvider
import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.model.DefaultCategories
import com.budgetflow.app.domain.repository.CategoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val currentProfile: CurrentProfileProvider
) : CategoryRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCategories(): Flow<List<Category>> =
        currentProfile.currentProfileId.flatMapLatest { profileId -> categoryDao.observeAllForProfile(profileId) }
            .map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(category: Category): Long =
        categoryDao.upsert(category.toEntity(currentProfile.currentProfileId.first()))

    override suspend fun delete(category: Category) =
        categoryDao.delete(category.toEntity(currentProfile.currentProfileId.first()))

    override suspend fun seedDefaultsIfEmpty() {
        seedDefaultsForProfile(currentProfile.currentProfileId.first())
    }

    override suspend fun seedDefaultsForProfile(profileId: Long) {
        if (categoryDao.countForProfile(profileId) == 0) {
            categoryDao.insertAll(DefaultCategories.all.map { it.toEntity(profileId) })
        }
    }
}
