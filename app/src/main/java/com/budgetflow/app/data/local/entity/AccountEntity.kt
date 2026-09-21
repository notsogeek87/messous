package com.budgetflow.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val initialBalance: Double,
    val currency: String = "EUR",
    val isArchived: Boolean = false,
    @ColumnInfo(defaultValue = "0") val profileId: Long = 0
)
