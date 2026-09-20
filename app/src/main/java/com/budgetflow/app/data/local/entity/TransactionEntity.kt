package com.budgetflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A single, manually or automatically recorded money movement. [amount] is always a positive magnitude. */
@Serializable
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    /** "INCOME" or "EXPENSE". */
    val type: String,
    val categoryId: Long? = null,
    val dateEpochDay: Long,
    val description: String = "",
    val accountId: Long,
    val createdAtEpochMillis: Long
)
