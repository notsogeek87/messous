package com.budgetflow.engine.recognition

/**
 * How a [RecognizableService] matched a query, strongest first. Only used to rank/sort
 * suggestions internally - never shown to the user (spec: no visible "score").
 */
enum class MatchType { EXACT_NAME, EXACT_ALIAS, PREFIX_NAME, PREFIX_ALIAS, CONTAINS_NAME, CONTAINS_ALIAS, FUZZY }

data class ServiceMatch(
    val service: RecognizableService,
    val score: Float,
    val matchedBy: MatchType
)

/** An amount found inside free text, and what's left once it's removed (e.g. "Netflix 19.99" -> 19.99 / "Netflix"). */
data class AmountExtraction(val amount: Double, val remainingText: String)
