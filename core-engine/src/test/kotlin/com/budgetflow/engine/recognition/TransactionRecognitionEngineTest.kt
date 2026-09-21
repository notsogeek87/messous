package com.budgetflow.engine.recognition

import com.budgetflow.engine.model.Frequency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransactionRecognitionEngineTest {

    private val catalog = listOf(
        RecognizableService(
            id = "netflix", name = "Netflix",
            aliases = listOf("nflx", "netflix.com"),
            category = "Streaming", subCategory = "Vidéo",
            kind = ServiceKind.EXPENSE, icon = "netflix", defaultFrequency = Frequency.MONTHLY
        ),
        RecognizableService(
            id = "netflix_games", name = "Netflix Games",
            aliases = emptyList(),
            category = "Jeux", subCategory = null,
            kind = ServiceKind.EXPENSE, icon = "netflix"
        ),
        RecognizableService(
            id = "spotify", name = "Spotify",
            aliases = listOf("spotify premium"),
            category = "Musique", subCategory = "Audio",
            kind = ServiceKind.EXPENSE, icon = "spotify", defaultFrequency = Frequency.MONTHLY
        ),
        RecognizableService(
            id = "orange", name = "Orange",
            aliases = listOf("orange france"),
            category = "Télécom", subCategory = null,
            kind = ServiceKind.EXPENSE, icon = "orange", defaultFrequency = Frequency.MONTHLY
        ),
        RecognizableService(
            id = "edf", name = "EDF",
            aliases = listOf("edf particulier"),
            category = "Énergie", subCategory = null,
            kind = ServiceKind.EXPENSE, icon = "edf", defaultFrequency = Frequency.MONTHLY
        ),
        RecognizableService(
            id = "amazon_prime", name = "Amazon Prime",
            aliases = emptyList(),
            category = "Shopping", subCategory = "Abonnement",
            kind = ServiceKind.EXPENSE, icon = "amazon", defaultFrequency = Frequency.MONTHLY
        ),
        RecognizableService(
            id = "amazon_prime_video", name = "Amazon Prime Video",
            aliases = emptyList(),
            category = "Streaming", subCategory = "Vidéo",
            kind = ServiceKind.EXPENSE, icon = "amazon", defaultFrequency = Frequency.MONTHLY
        ),
        RecognizableService(
            id = "amazon_music", name = "Amazon Music",
            aliases = emptyList(),
            category = "Musique", subCategory = null,
            kind = ServiceKind.EXPENSE, icon = "amazon", defaultFrequency = Frequency.MONTHLY
        ),
        RecognizableService(
            id = "disney_plus", name = "Disney+",
            aliases = listOf("disney plus"),
            category = "Streaming", subCategory = "Vidéo",
            kind = ServiceKind.EXPENSE, icon = "disneyplus", defaultFrequency = Frequency.MONTHLY
        ),
        RecognizableService(
            id = "salaire", name = "Salaire",
            aliases = listOf("paie", "salary"),
            category = "Revenu du travail", subCategory = null,
            kind = ServiceKind.INCOME, icon = "salary", defaultFrequency = Frequency.MONTHLY
        )
    )

    private fun namesOf(matches: List<ServiceMatch>) = matches.map { it.service.name }

    @Test
    fun `prefix queries find Netflix`() {
        listOf("n", "ne", "net", "netf", "netfl", "netflix").forEach { query ->
            val top = TransactionRecognitionEngine.suggest(query, catalog).firstOrNull()
            assertEquals("Netflix", top?.service?.name, "query '$query' should surface Netflix first")
        }
    }

    @Test
    fun `netflix dot com and alias variants all resolve to Netflix`() {
        listOf("netflix.com", "NETFLIX", "netflix com", "Netflix.com").forEach { query ->
            val top = TransactionRecognitionEngine.suggest(query, catalog).first()
            assertEquals("Netflix", top.service.name)
        }
        val nflx = TransactionRecognitionEngine.suggest("nflx", catalog).first()
        assertEquals("Netflix", nflx.service.name)
        assertEquals(MatchType.EXACT_ALIAS, nflx.matchedBy)
    }

    @Test
    fun `spo and spoti resolve to Spotify`() {
        assertEquals("Spotify", TransactionRecognitionEngine.suggest("spo", catalog).first().service.name)
        assertEquals("Spotify", TransactionRecognitionEngine.suggest("spoti", catalog).first().service.name)
    }

    @Test
    fun `short prefixes resolve Orange and EDF`() {
        assertEquals("Orange", TransactionRecognitionEngine.suggest("ora", catalog).first().service.name)
        assertEquals("EDF", TransactionRecognitionEngine.suggest("edf", catalog).first().service.name)
    }

    @Test
    fun `ama surfaces several Amazon services`() {
        val results = TransactionRecognitionEngine.suggest("ama", catalog)
        val names = namesOf(results)
        assertTrue(names.contains("Amazon Prime"))
        assertTrue(names.contains("Amazon Prime Video"))
        assertTrue(names.contains("Amazon Music"))
        assertTrue(results.size >= 3)
    }

    @Test
    fun `typo disnei still finds Disney plus with a reasonable score`() {
        val top = TransactionRecognitionEngine.suggest("disnei", catalog).firstOrNull()
        assertEquals("Disney+", top?.service?.name)
        assertEquals(MatchType.FUZZY, top?.matchedBy)
    }

    @Test
    fun `amount extraction handles dot and comma decimals`() {
        val dot = TransactionRecognitionEngine.extractAmount("Netflix 19.99")
        assertEquals(19.99, dot?.amount)
        assertEquals("Netflix", dot?.remainingText)

        val comma = TransactionRecognitionEngine.extractAmount("spotify 11,12")
        assertEquals(11.12, comma?.amount)
        assertEquals("spotify", comma?.remainingText)
    }

    @Test
    fun `amount extraction handles currency signs before or after`() {
        val prefixed = TransactionRecognitionEngine.extractAmount("€19.99 Netflix")
        assertEquals(19.99, prefixed?.amount)
        assertEquals("Netflix", prefixed?.remainingText)

        val suffixed = TransactionRecognitionEngine.extractAmount("Netflix 19,99€")
        assertEquals(19.99, suffixed?.amount)
        assertEquals("Netflix", suffixed?.remainingText)
    }

    @Test
    fun `netflix with amount is still recognized once the amount is stripped`() {
        val extraction = TransactionRecognitionEngine.extractAmount("Netflix 19.99")
        requireNotNull(extraction)
        val top = TransactionRecognitionEngine.suggest(extraction.remainingText, catalog).first()
        assertEquals("Netflix", top.service.name)
    }

    @Test
    fun `plain integers are not mistaken for an amount`() {
        assertNull(TransactionRecognitionEngine.extractAmount("Free 5G"))
        assertNull(TransactionRecognitionEngine.extractAmount("OCS Max 2"))
    }

    @Test
    fun `unrecognizable text yields no suggestions and no forced match`() {
        assertTrue(TransactionRecognitionEngine.suggest("Mon abonnement bizarre", catalog).isEmpty())
        assertTrue(TransactionRecognitionEngine.suggest("xyzzyqwerty", catalog).isEmpty())
    }

    @Test
    fun `blank very short and special character queries are handled safely`() {
        assertTrue(TransactionRecognitionEngine.suggest("", catalog).isEmpty())
        assertTrue(TransactionRecognitionEngine.suggest("   ", catalog).isEmpty())
        assertTrue(TransactionRecognitionEngine.suggest("!!!", catalog).isEmpty())
        // A single character is still processed, just very broad - must not crash.
        TransactionRecognitionEngine.suggest("n", catalog)
    }

    @Test
    fun `kind filter restricts to income or expense catalogs`() {
        val incomeResults = TransactionRecognitionEngine.suggest("sal", catalog, kind = ServiceKind.INCOME)
        assertEquals(listOf("Salaire"), namesOf(incomeResults))

        val expenseResults = TransactionRecognitionEngine.suggest("sal", catalog, kind = ServiceKind.EXPENSE)
        assertTrue(expenseResults.isEmpty())
    }

    @Test
    fun `accents and case are ignored`() {
        val service = listOf(
            RecognizableService(
                id = "energie_test", name = "Énergie Plus",
                category = "Énergie", kind = ServiceKind.EXPENSE, icon = "bolt"
            )
        )
        val top = TransactionRecognitionEngine.suggest("energie", service).firstOrNull()
        assertEquals("Énergie Plus", top?.service?.name)
        val top2 = TransactionRecognitionEngine.suggest("ÉNERGIE", service).firstOrNull()
        assertEquals("Énergie Plus", top2?.service?.name)
    }

    @Test
    fun `results are ranked exact then alias then prefix then fuzzy`() {
        val exact = TransactionRecognitionEngine.suggest("Netflix", catalog).first()
        assertEquals(MatchType.EXACT_NAME, exact.matchedBy)

        val alias = TransactionRecognitionEngine.suggest("nflx", catalog).first()
        assertEquals(MatchType.EXACT_ALIAS, alias.matchedBy)

        val prefix = TransactionRecognitionEngine.suggest("netf", catalog).first()
        assertEquals(MatchType.PREFIX_NAME, prefix.matchedBy)
    }
}
