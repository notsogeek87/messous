package com.budgetflow.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A recurring fixed expense (rent, credit, subscription, insurance...). */
@Serializable
@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val amount: Double,
    val frequency: String,
    val dayOfMonth: Int? = null,
    val dayOfWeek: Int? = null,
    val monthOfYear: Int? = null,
    val oneTimeDateEpochDay: Long? = null,
    val startDateEpochDay: Long? = null,
    val endDateEpochDay: Long? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    /** True when the amount is identical every period (rent); false when it typically varies (electricity). */
    val isFixedAmount: Boolean = true,
    val isActive: Boolean = true,
    @ColumnInfo(defaultValue = "0") val profileId: Long = 0
)
