package com.budgetflow.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(private val categoryRepository: CategoryRepository) : ViewModel() {

    val categories: StateFlow<List<Category>> =
        categoryRepository.observeCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(category: Category) = viewModelScope.launch { categoryRepository.upsert(category) }
    fun delete(category: Category) = viewModelScope.launch { categoryRepository.delete(category) }
}
