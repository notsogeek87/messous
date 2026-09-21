package com.budgetflow.app.ui.components

import com.budgetflow.engine.model.Frequency

/**
 * Rough daily/monthly weight of a recurring amount, used to preview the impact of an
 * income/expense *while filling the form* - before it ever reaches the engine's real
 * month-by-month calculation. A one-time flow has no ongoing weight.
 */
fun dailyEquivalent(amount: Double, frequency: Frequency): Double = when (frequency) {
    Frequency.MONTHLY -> amount / 30.0
    Frequency.WEEKLY -> amount / 7.0
    Frequency.YEARLY -> amount / 365.0
    Frequency.ONE_TIME -> 0.0
}

fun monthlyEquivalent(amount: Double, frequency: Frequency): Double = when (frequency) {
    Frequency.MONTHLY -> amount
    Frequency.WEEKLY -> amount * 52.0 / 12.0
    Frequency.YEARLY -> amount / 12.0
    Frequency.ONE_TIME -> amount
}
