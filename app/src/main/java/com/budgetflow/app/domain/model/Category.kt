package com.budgetflow.app.domain.model

/** The fixed set of category groups offered when creating a category (section 8 of the spec). */
object CategoryGroups {
    const val HOUSING = "Logement"
    const val DAILY_LIFE = "Vie quotidienne"
    const val LEISURE = "Loisirs"
    const val FAMILY = "Famille"
    const val FINANCE = "Finance"
    const val OTHER = "Autre"

    val all = listOf(HOUSING, DAILY_LIFE, LEISURE, FAMILY, FINANCE, OTHER)
}

data class Category(
    val id: Long = 0,
    val name: String,
    val group: String,
    val icon: String,
    val isDefault: Boolean = false
)
