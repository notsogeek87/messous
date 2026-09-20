package com.budgetflow.app.domain.repository

interface BackupRepository {
    /** Serializes every table into a single human-readable JSON document. */
    suspend fun exportToJson(): String

    /** Replaces all current data with the content of [json]. Destructive - callers must confirm with the user first. */
    suspend fun importFromJson(json: String)
}
