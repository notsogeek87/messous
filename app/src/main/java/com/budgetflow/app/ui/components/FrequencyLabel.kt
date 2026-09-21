package com.budgetflow.app.ui.components

import com.budgetflow.engine.model.Frequency
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.LocalDate

private val oneTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun frenchWeekday(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "lundi"
    DayOfWeek.TUESDAY -> "mardi"
    DayOfWeek.WEDNESDAY -> "mercredi"
    DayOfWeek.THURSDAY -> "jeudi"
    DayOfWeek.FRIDAY -> "vendredi"
    DayOfWeek.SATURDAY -> "samedi"
    DayOfWeek.SUNDAY -> "dimanche"
}

private fun frenchMonth(month: Int): String = when (month) {
    1 -> "janvier"; 2 -> "février"; 3 -> "mars"; 4 -> "avril"; 5 -> "mai"; 6 -> "juin"
    7 -> "juillet"; 8 -> "août"; 9 -> "septembre"; 10 -> "octobre"; 11 -> "novembre"; else -> "décembre"
}

/** A short human-readable description of a recurring schedule, e.g. "Mensuel · le 5" or "Hebdomadaire · lundi". */
fun frequencyLabel(
    frequency: Frequency,
    dayOfMonth: Int?,
    dayOfWeek: DayOfWeek?,
    monthOfYear: Int?,
    oneTimeDate: LocalDate?
): String = when (frequency) {
    Frequency.MONTHLY -> "Mensuel · le ${dayOfMonth ?: "?"}"
    Frequency.WEEKLY -> "Hebdomadaire · ${dayOfWeek?.let { frenchWeekday(it) } ?: "?"}"
    Frequency.YEARLY -> "Annuel · ${dayOfMonth ?: "?"} ${monthOfYear?.let { frenchMonth(it) } ?: ""}"
    Frequency.ONE_TIME -> "Ponctuel · ${oneTimeDate?.format(oneTimeFormatter) ?: "?"}"
}
