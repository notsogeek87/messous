package com.budgetflow.engine.recognition

import com.budgetflow.engine.model.Frequency

/** What a [RecognizableService] is used for; mirrors the app's own income/expense split. */
enum class ServiceKind { EXPENSE, INCOME }

/**
 * One entry of the local, offline service catalog (Netflix, Spotify, EDF, a salary type, ...).
 * Deliberately plain data with no Android/JSON dependency so it can be matched and unit tested
 * from any JVM, and later reused to recognize imported bank transaction labels (e.g.
 * "NETFLIX.COM", "UBER *TRIP") without changing the engine - only the caller feeding it text.
 */
data class RecognizableService(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val category: String,
    val subCategory: String? = null,
    val kind: ServiceKind,
    val icon: String,
    val defaultFrequency: Frequency? = null,
    val country: String? = null,
    val officialUrl: String? = null
)
