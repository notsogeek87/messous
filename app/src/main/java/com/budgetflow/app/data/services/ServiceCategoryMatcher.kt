package com.budgetflow.app.data.services

import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.model.CategoryGroups
import com.budgetflow.app.domain.repository.CategoryRepository
import com.budgetflow.engine.recognition.RecognizableService

/**
 * Bridges the service catalog's category labels ("Streaming", "Télécom", ...) to the app's own
 * [Category] table instead of building a second, parallel category system (spec section 21).
 *
 * It first tries to reuse a category the user already has (matching the service's sub-category,
 * then its top-level category, case-insensitively). Only when neither exists yet does it create
 * one - through the same [CategoryRepository] the "Catégories" screen writes to, so the result is
 * an ordinary category that shows up in every dropdown and stays fully user-editable/removable.
 * Nothing here is called except when the user explicitly taps a suggestion (spec section 19).
 */
class ServiceCategoryMatcher(private val categoryRepository: CategoryRepository) {

    suspend fun categoryFor(service: RecognizableService, existingCategories: List<Category>): Category? {
        val preferredName = service.subCategory?.takeIf { it.isNotBlank() } ?: service.category
        existingCategories.firstOrNull { it.name.equals(preferredName, ignoreCase = true) }?.let { return it }
        existingCategories.firstOrNull { it.name.equals(service.category, ignoreCase = true) }?.let { return it }

        val group = groupFor(service.category)
        val icon = iconFor(service.category)
        val id = categoryRepository.upsert(Category(name = preferredName, group = group, icon = icon, isDefault = false))
        return Category(id = id, name = preferredName, group = group, icon = icon, isDefault = false)
    }

    private fun groupFor(categoryLabel: String): String = CATEGORY_GROUP_BY_LABEL[categoryLabel] ?: CategoryGroups.OTHER
    private fun iconFor(categoryLabel: String): String = CATEGORY_ICON_BY_LABEL[categoryLabel] ?: "receipt"

    companion object {
        /** service.category (top level, as written in services.json) -> existing [CategoryGroups] bucket. */
        private val CATEGORY_GROUP_BY_LABEL: Map<String, String> = mapOf(
            "Streaming" to CategoryGroups.LEISURE,
            "Musique" to CategoryGroups.LEISURE,
            "IA" to CategoryGroups.OTHER,
            "Cloud" to CategoryGroups.OTHER,
            "Logiciels" to CategoryGroups.OTHER,
            "Jeux" to CategoryGroups.LEISURE,
            "Télécom" to CategoryGroups.HOUSING,
            "Internet" to CategoryGroups.HOUSING,
            "Énergie" to CategoryGroups.HOUSING,
            "Eau" to CategoryGroups.HOUSING,
            "Assurances" to CategoryGroups.FINANCE,
            "Banques" to CategoryGroups.FINANCE,
            "Automobile" to CategoryGroups.DAILY_LIFE,
            "Transport" to CategoryGroups.DAILY_LIFE,
            "Sport" to CategoryGroups.LEISURE,
            "Shopping" to CategoryGroups.DAILY_LIFE,
            "Presse" to CategoryGroups.LEISURE,
            "Éducation" to CategoryGroups.FAMILY,
            "Famille" to CategoryGroups.FAMILY
        )

        /** service.category -> key resolved by [com.budgetflow.app.ui.components.CategoryIcons]. */
        private val CATEGORY_ICON_BY_LABEL: Map<String, String> = mapOf(
            "Streaming" to "movie",
            "Musique" to "music",
            "IA" to "ai",
            "Cloud" to "cloud",
            "Logiciels" to "software",
            "Jeux" to "games",
            "Télécom" to "phone",
            "Internet" to "wifi",
            "Énergie" to "bolt",
            "Eau" to "water",
            "Assurances" to "shield",
            "Banques" to "bank",
            "Automobile" to "car",
            "Transport" to "train",
            "Sport" to "fitness",
            "Shopping" to "cart",
            "Presse" to "newspaper",
            "Éducation" to "school",
            "Famille" to "child"
        )
    }
}
