package com.budgetflow.app.data.services

import com.budgetflow.engine.model.Frequency
import com.budgetflow.engine.recognition.RecognizableService
import com.budgetflow.engine.recognition.ServiceKind
import kotlinx.serialization.json.Json

/**
 * Parses and validates the local service catalog (spec section 20: catch a duplicate id, a
 * missing name/category, at load time). Deliberately has no Android dependency - [loadFrom] takes
 * plain text - so it can be unit tested on any JVM; [ServiceCatalogAndroidLoader] in the app
 * bridges it to `assets/services.json` on a real device.
 */
object ServiceCatalogLoader {
    private val json = Json { ignoreUnknownKeys = true }

    /** @throws IllegalArgumentException if the catalog has a duplicate id or a blank required field. */
    fun loadFrom(jsonText: String): List<RecognizableService> {
        val file = json.decodeFromString(ServiceCatalogFileDto.serializer(), jsonText)
        val seenIds = mutableSetOf<String>()
        return file.services.map { entry ->
            require(entry.id.isNotBlank()) { "A service catalog entry is missing its id" }
            require(seenIds.add(entry.id)) { "Duplicate service id '${entry.id}' in services.json" }
            require(entry.name.isNotBlank()) { "Service '${entry.id}' has a blank name" }
            require(entry.category.isNotBlank()) { "Service '${entry.id}' has a blank category" }
            entry.toDomain()
        }
    }
}

private fun ServiceCatalogEntryDto.toDomain(): RecognizableService = RecognizableService(
    id = id,
    name = name,
    aliases = aliases,
    category = category,
    subCategory = subCategory,
    kind = when (type.lowercase()) {
        "income" -> ServiceKind.INCOME
        "expense" -> ServiceKind.EXPENSE
        else -> error("Service '$id' has an unknown type '$type' (expected 'expense' or 'income')")
    },
    icon = icon,
    defaultFrequency = defaultFrequency?.let { parseFrequency(id, it) },
    country = country,
    officialUrl = officialUrl
)

private fun parseFrequency(serviceId: String, value: String): Frequency = when (value.lowercase()) {
    "weekly" -> Frequency.WEEKLY
    "monthly" -> Frequency.MONTHLY
    "yearly" -> Frequency.YEARLY
    "one_time", "onetime", "one-time" -> Frequency.ONE_TIME
    else -> error("Service '$serviceId' has an unknown defaultFrequency '$value'")
}
