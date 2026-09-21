package com.budgetflow.app.data.services

import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.model.CategoryGroups
import com.budgetflow.app.domain.repository.CategoryRepository
import com.budgetflow.engine.recognition.ServiceKind
import com.budgetflow.engine.recognition.TransactionRecognitionEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A minimal in-memory [CategoryRepository] so [ServiceCategoryMatcher] can be unit tested on the JVM. */
private class FakeCategoryRepository(seed: List<Category>) : CategoryRepository {
    private val items = seed.toMutableList()
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0) + 1
    val created = mutableListOf<Category>()

    override fun observeCategories(): Flow<List<Category>> = flowOf(items)
    override suspend fun upsert(category: Category): Long {
        val id = if (category.id != 0L) category.id else nextId++
        val saved = category.copy(id = id)
        items.removeAll { it.id == id }
        items.add(saved)
        if (category.id == 0L) created.add(saved)
        return id
    }
    override suspend fun delete(category: Category) { items.removeAll { it.id == category.id } }
    override suspend fun seedDefaultsIfEmpty() {}
}

/**
 * Loads and validates the real app/src/main/assets/services.json (spec section 20: catch a
 * duplicate id, a missing name/category at test time) and exercises the acceptance criteria from
 * the spec (section 23) end-to-end against it - no mock catalog, the exact file shipped in the APK.
 */
class ServiceCatalogValidationTest {

    private val jsonText = javaClass.classLoader!!.getResourceAsStream("services.json")!!
        .bufferedReader(Charsets.UTF_8).use { it.readText() }

    private val catalog = ServiceCatalogLoader.loadFrom(jsonText)

    @Test
    fun `real catalog has several hundred entries covering expenses and income`() {
        assertTrue(catalog.size >= 250, "expected a large catalog, got ${catalog.size}")
        assertTrue(catalog.count { it.kind == ServiceKind.INCOME } >= 15)
        assertTrue(catalog.count { it.kind == ServiceKind.EXPENSE } >= 200)
    }

    @Test
    fun `acceptance criteria queries resolve against the real catalog`() {
        fun top(q: String) = TransactionRecognitionEngine.suggest(q, catalog).firstOrNull()?.service?.name

        assertEquals("Netflix", top("net"))
        assertEquals("Netflix", top("nflx"))
        assertEquals("Spotify", top("spoti"))
        assertEquals("Orange", top("ora"))
        assertEquals("EDF", top("edf"))
        assertEquals("Disney+", top("disnei"))

        val amaNames = TransactionRecognitionEngine.suggest("ama", catalog).map { it.service.name }
        assertTrue(amaNames.contains("Amazon"))
        assertTrue(amaNames.contains("Amazon Prime"))
        assertTrue(amaNames.contains("Amazon Prime Video"))
        assertTrue(amaNames.contains("Amazon Music"))

        assertTrue(TransactionRecognitionEngine.suggest("Mon abonnement bizarre", catalog).isEmpty())
    }

    @Test
    fun `amount extraction plus recognition end to end on the real catalog`() {
        val extraction = TransactionRecognitionEngine.extractAmount("Netflix 19.99")!!
        assertEquals(19.99, extraction.amount)
        val top = TransactionRecognitionEngine.suggest(extraction.remainingText, catalog).first()
        assertEquals("Netflix", top.service.name)
    }

    @Test
    fun `category matcher reuses an existing Streaming category for Netflix`() = runBlocking {
        val existing = listOf(Category(id = 1, name = "Streaming", group = CategoryGroups.LEISURE, icon = "movie", isDefault = true))
        val repo = FakeCategoryRepository(existing)
        val matcher = ServiceCategoryMatcher(repo)
        val netflix = catalog.first { it.id == "netflix" }

        val category = matcher.categoryFor(netflix, existing)
        assertEquals(1L, category?.id)
        assertTrue(repo.created.isEmpty(), "should not create a new category when 'Streaming' already exists")
    }

    @Test
    fun `category matcher creates a missing category through the real repository`() = runBlocking {
        val repo = FakeCategoryRepository(emptyList())
        val matcher = ServiceCategoryMatcher(repo)
        val edf = catalog.first { it.id == "edf" }

        val category = matcher.categoryFor(edf, emptyList())
        assertEquals("Énergie", category?.name)
        assertEquals(1, repo.created.size)
    }

    @Test
    fun `every catalog entry has a non blank icon key and a lowercase ascii id`() {
        catalog.forEach { service ->
            assertTrue(service.icon.isNotBlank(), "service ${service.id} has a blank icon")
            assertTrue(Regex("^[a-z0-9_]+$").matches(service.id), "unexpected id format: ${service.id}")
        }
    }
}
