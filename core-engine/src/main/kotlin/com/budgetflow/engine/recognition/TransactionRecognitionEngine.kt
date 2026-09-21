package com.budgetflow.engine.recognition

import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.max

/**
 * Local, offline, typo-tolerant matching of free text against a [RecognizableService] catalog -
 * the "smart search" behind service recognition while typing an expense/income label. Pure
 * Kotlin/JVM so the exact same engine can later score imported bank transaction labels (e.g.
 * "NETFLIX.COM", "UBER *TRIP") without any change - only the text fed to [suggest] differs.
 *
 * This is a suggestion engine, never a decision maker: [suggest] returns ranked candidates, it
 * never rewrites or replaces the caller's text. Nothing here mutates persisted data.
 */
object TransactionRecognitionEngine {

    private const val MIN_FUZZY_QUERY_LENGTH = 4

    /** Lowercase, accent/punctuation/space-stripped form used for every comparison below, so
     * "Netflix.com", "netflix com" and "NETFLIX" all normalize to "netflixcom"/"netflix". */
    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        val builder = StringBuilder(decomposed.length)
        for (char in decomposed) {
            if (char in 'a'..'z' || char in '0'..'9') builder.append(char)
        }
        return builder.toString()
    }

    /**
     * Ranked suggestions for [query] against [catalog], strongest match first. Returns an empty
     * list for a blank query or when nothing is recognizable (spec section 10): the caller must
     * never invent or force a match from this - it's a suggestion list, not a decision.
     */
    fun suggest(
        query: String,
        catalog: List<RecognizableService>,
        kind: ServiceKind? = null,
        limit: Int = 6
    ): List<ServiceMatch> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()

        val candidates = if (kind != null) catalog.filter { it.kind == kind } else catalog
        val allowFuzzy = normalizedQuery.length >= MIN_FUZZY_QUERY_LENGTH

        return candidates
            .mapNotNull { service -> bestMatch(normalizedQuery, service, allowFuzzy) }
            .sortedWith(compareByDescending<ServiceMatch> { it.score }.thenBy { it.service.name })
            .take(limit)
    }

    private fun bestMatch(normalizedQuery: String, service: RecognizableService, allowFuzzy: Boolean): ServiceMatch? {
        val normalizedName = normalize(service.name)
        if (normalizedName.isEmpty()) return null
        val normalizedAliases = service.aliases.map(::normalize).filter { it.isNotEmpty() }

        if (normalizedName == normalizedQuery) return ServiceMatch(service, 100f, MatchType.EXACT_NAME)
        if (normalizedQuery in normalizedAliases) return ServiceMatch(service, 95f, MatchType.EXACT_ALIAS)

        if (normalizedName.startsWith(normalizedQuery)) {
            val closeness = normalizedQuery.length.toFloat() / normalizedName.length
            return ServiceMatch(service, 80f + 10f * closeness, MatchType.PREFIX_NAME)
        }
        val aliasPrefixMatch = normalizedAliases.firstOrNull { it.startsWith(normalizedQuery) }
        if (aliasPrefixMatch != null) {
            val closeness = normalizedQuery.length.toFloat() / aliasPrefixMatch.length
            return ServiceMatch(service, 75f + 10f * closeness, MatchType.PREFIX_ALIAS)
        }

        if (normalizedName.contains(normalizedQuery)) return ServiceMatch(service, 55f, MatchType.CONTAINS_NAME)
        if (normalizedAliases.any { it.contains(normalizedQuery) }) return ServiceMatch(service, 50f, MatchType.CONTAINS_ALIAS)

        if (allowFuzzy) {
            val fuzzyScore = bestFuzzyScore(normalizedQuery, listOf(normalizedName) + normalizedAliases)
            if (fuzzyScore != null) return ServiceMatch(service, fuzzyScore, MatchType.FUZZY)
        }
        return null
    }

    private fun bestFuzzyScore(normalizedQuery: String, candidates: List<String>): Float? {
        var best: Float? = null
        val maxAllowed = maxAllowedDistance(normalizedQuery.length)
        for (candidate in candidates) {
            if (abs(candidate.length - normalizedQuery.length) > maxAllowed + 1) continue
            val distance = levenshtein(normalizedQuery, candidate)
            if (distance > maxAllowed) continue
            val similarity = 1f - distance.toFloat() / max(normalizedQuery.length, candidate.length)
            val score = 20f + 20f * similarity
            if (best == null || score > best!!) best = score
        }
        return best
    }

    private fun maxAllowedDistance(queryLength: Int): Int = when {
        queryLength < MIN_FUZZY_QUERY_LENGTH -> 0
        queryLength <= 5 -> 1
        queryLength <= 8 -> 2
        else -> 3
    }

    /** Classic iterative Levenshtein edit distance, O(a.length * b.length), fine for short strings. */
    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val previous = IntArray(b.length + 1) { it }
        val current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
            }
            for (j in 0..b.length) previous[j] = current[j]
        }
        return previous[b.length]
    }

    private val amountPattern = Regex("""(?:(?<pre>[€$])\s?)?(?<num>\d{1,6}[.,]\d{1,2})\s?(?<post>[€$])?""")

    /**
     * Pulls a trailing/embedded amount out of free text, e.g. "Netflix 19.99" or "19,99€ Spotify".
     * Only fires on a clearly monetary-looking number (decimal separator, optionally with a
     * currency sign) so it never misfires on a plan name that happens to contain digits (e.g.
     * "OCS Max 2", "Free 5G"). Returns null when no such amount is found.
     */
    fun extractAmount(text: String): AmountExtraction? {
        val match = amountPattern.find(text) ?: return null
        val numberText = match.groups["num"]?.value ?: return null
        val amount = numberText.replace(',', '.').toDoubleOrNull() ?: return null
        val remaining = (text.substring(0, match.range.first) + text.substring(match.range.last + 1))
            .replace(Regex("\\s+"), " ")
            .trim()
        return AmountExtraction(amount, remaining)
    }
}
