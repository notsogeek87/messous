package com.budgetflow.app.data.services

import kotlinx.serialization.Serializable

/**
 * Plain JSON shape of app/src/main/assets/services.json (spec section 1-2: a data file, not a
 * Kotlin "when", so adding a service never touches the recognition engine). One top-level object
 * so the file can later grow extra keys (schema version, source, ...) without breaking parsing.
 */
@Serializable
data class ServiceCatalogFileDto(val services: List<ServiceCatalogEntryDto> = emptyList())

@Serializable
data class ServiceCatalogEntryDto(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val category: String,
    val subCategory: String? = null,
    val type: String,
    val icon: String,
    val defaultFrequency: String? = null,
    val country: String? = null,
    val officialUrl: String? = null
)
