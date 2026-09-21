package com.budgetflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A walled-off space (Perso, Pro, Commun, or any custom one) - every other table is scoped to one via profileId. */
@Serializable
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
