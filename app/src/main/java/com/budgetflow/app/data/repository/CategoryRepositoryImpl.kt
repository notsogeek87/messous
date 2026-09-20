package com.budgetflow.app.data.repository

import com.budgetflow.app.data.local.dao.CategoryDao
import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.model.DefaultCategories
import com.budgetflow.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(private val categoryDao: CategoryDao) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(category: Category): Long = categoryDao.upsert(category.toEntity())

    override suspend fun delete(category: Category) = categoryDao.delete(category.toEntity())

    override suspend fun seedDefaultsIfEmpty() {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(DefaultCategories.all.map { it.toEntity() })
        }
    }
}
