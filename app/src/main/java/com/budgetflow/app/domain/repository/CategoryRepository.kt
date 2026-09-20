package com.budgetflow.app.domain.repository

import com.budgetflow.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    suspend fun upsert(category: Category): Long
    suspend fun delete(category: Category)
    suspend fun seedDefaultsIfEmpty()
}
