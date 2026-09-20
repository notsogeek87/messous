package com.budgetflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A recurring or one-off income. Scheduling fields mirror
 * [com.budgetflow.engine.model.ScheduledFlow] so mapping to the engine is a
 * straight field-by-field copy - see Mappers.kt.
 */
@Serializable
@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val amount: Double,
    /** WEEKLY, MONTHLY, YEARLY or ONE_TIME. */
    val frequency: String,
    val dayOfMonth: Int? = null,
    /** 1 (Monday) .. 7 (Sunday), matching [java.time.DayOfWeek.getValue]. */
    val dayOfWeek: Int? = null,
    val monthOfYear: Int? = null,
    val oneTimeDateEpochDay: Long? = null,
    val startDateEpochDay: Long? = null,
    val endDateEpochDay: Long? = null,
    val accountId: Long? = null,
    val isActive: Boolean = true
)
