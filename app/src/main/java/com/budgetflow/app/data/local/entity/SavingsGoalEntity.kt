package com.budgetflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    /** Counted in the monthly "épargne prévue" total when active. */
    val monthlyContribution: Double = 0.0,
    val isActive: Boolean = true
)
