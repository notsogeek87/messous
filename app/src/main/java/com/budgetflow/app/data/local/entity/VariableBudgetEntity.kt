package com.budgetflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A recurring monthly spending envelope (e.g. "Courses: 500 EUR / mois").
 * The amount already spent for a given month is derived on the fly from
 * [TransactionEntity] rows tagged with [categoryId], not stored here - this
 * is what lets the same envelope roll over cleanly from one month to the next.
 */
@Serializable
@Entity(tableName = "variable_budgets")
data class VariableBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val monthlyLimit: Double,
    val categoryId: Long,
    val isActive: Boolean = true
)
