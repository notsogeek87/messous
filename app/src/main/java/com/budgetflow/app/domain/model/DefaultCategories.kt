package com.budgetflow.app.domain.model

/** The starter categories created on first launch (spec section 8). Icon keys are resolved by ui/components/CategoryIcons.kt. */
object DefaultCategories {
    val all: List<Category> = listOf(
        Category(name = "Crédit", group = CategoryGroups.HOUSING, icon = "home", isDefault = true),
        Category(name = "Loyer", group = CategoryGroups.HOUSING, icon = "home", isDefault = true),
        Category(name = "Charges", group = CategoryGroups.HOUSING, icon = "receipt", isDefault = true),
        Category(name = "Énergie", group = CategoryGroups.HOUSING, icon = "bolt", isDefault = true),

        Category(name = "Courses", group = CategoryGroups.DAILY_LIFE, icon = "cart", isDefault = true),
        Category(name = "Transport", group = CategoryGroups.DAILY_LIFE, icon = "train", isDefault = true),
        Category(name = "Essence", group = CategoryGroups.DAILY_LIFE, icon = "fuel", isDefault = true),
        Category(name = "Santé", group = CategoryGroups.DAILY_LIFE, icon = "health", isDefault = true),

        Category(name = "Restaurants", group = CategoryGroups.LEISURE, icon = "restaurant", isDefault = true),
        Category(name = "Sorties", group = CategoryGroups.LEISURE, icon = "celebration", isDefault = true),
        Category(name = "Jeux", group = CategoryGroups.LEISURE, icon = "games", isDefault = true),
        Category(name = "Streaming", group = CategoryGroups.LEISURE, icon = "movie", isDefault = true),

        Category(name = "Enfants", group = CategoryGroups.FAMILY, icon = "child", isDefault = true),
        Category(name = "École", group = CategoryGroups.FAMILY, icon = "school", isDefault = true),
        Category(name = "Activités", group = CategoryGroups.FAMILY, icon = "activity", isDefault = true),

        Category(name = "Épargne", group = CategoryGroups.FINANCE, icon = "savings", isDefault = true),
        Category(name = "Investissement", group = CategoryGroups.FINANCE, icon = "trending_up", isDefault = true),
        Category(name = "Impôts", group = CategoryGroups.FINANCE, icon = "bank", isDefault = true)
    )
}
