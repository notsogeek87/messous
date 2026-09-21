package com.budgetflow.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** [group] is a free-form label such as "Logement", "Vie quotidienne", "Loisirs", "Famille", "Finance". */
@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val group: String,
    /** Key into [com.budgetflow.app.ui.components.CategoryIcons], e.g. "home", "cart", "movie". */
    val icon: String,
    val isDefault: Boolean = false,
    @ColumnInfo(defaultValue = "0") val profileId: Long = 0
)
