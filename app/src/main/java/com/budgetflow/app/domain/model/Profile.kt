package com.budgetflow.app.domain.model

/** A walled-off space (Perso, Pro, Commun, or any custom one the user adds) - see [com.budgetflow.app.domain.repository.ProfileRepository]. */
data class Profile(
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0
)
