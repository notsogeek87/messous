package com.budgetflow.app.domain.model

import com.budgetflow.engine.model.Frequency
import com.budgetflow.engine.model.ScheduledFlow
import java.time.DayOfWeek
import java.time.LocalDate

data class RecurringExpense(
    val id: Long = 0,
    val label: String,
    val amount: Double,
    val frequency: Frequency,
    val dayOfMonth: Int? = null,
    val dayOfWeek: DayOfWeek? = null,
    val monthOfYear: Int? = null,
    val oneTimeDate: LocalDate? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val isFixedAmount: Boolean = true,
    val isActive: Boolean = true
) {
    fun toScheduledFlow(): ScheduledFlow = ScheduledFlow(
        id = id,
        label = label,
        amount = amount,
        frequency = frequency,
        dayOfMonth = dayOfMonth,
        dayOfWeek = dayOfWeek,
        monthOfYear = monthOfYear,
        oneTimeDate = oneTimeDate,
        startDate = startDate,
        endDate = endDate,
        isActive = isActive
    )
}
