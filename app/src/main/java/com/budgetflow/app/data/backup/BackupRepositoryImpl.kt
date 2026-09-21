package com.budgetflow.app.data.backup

import com.budgetflow.app.data.local.BudgetFlowDatabase
import com.budgetflow.app.data.local.entity.ProfileEntity
import com.budgetflow.app.domain.repository.BackupRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * Exports/imports every table as a single local JSON file. Nothing here ever
 * touches the network - the caller decides where the resulting string is
 * written (normally a user-picked file via the Storage Access Framework).
 */
class BackupRepositoryImpl(
    private val database: BudgetFlowDatabase,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
) : BackupRepository {

    override suspend fun exportToJson(): String {
        val payload = BackupPayload(
            exportedAtEpochMillis = System.currentTimeMillis(),
            profiles = database.profileDao().getAll(),
            accounts = database.accountDao().observeAll().first(),
            categories = database.categoryDao().observeAll().first(),
            incomes = database.incomeDao().observeAll().first(),
            recurringExpenses = database.recurringExpenseDao().observeAll().first(),
            variableBudgets = database.variableBudgetDao().observeAll().first(),
            transactions = database.transactionDao().observeAll().first(),
            savingsGoals = database.savingsGoalDao().observeAll().first()
        )
        return json.encodeToString(BackupPayload.serializer(), payload)
    }

    override suspend fun importFromJson(jsonText: String) {
        val payload = json.decodeFromString(BackupPayload.serializer(), jsonText)

        database.clearAllTables()

        // Pre-profile exports (schemaVersion 1) carry no profiles table: everything they contain
        // becomes the single "Perso" profile, and Pro/Commun are (re)created empty alongside it.
        //
        // Each branch ends with an explicit `val identity: (Long) -> Long = { ... }` bound to a
        // name, rather than a bare trailing lambda: a lambda literal placed right after a call
        // statement on the previous line is parsed by Kotlin as a second trailing-lambda argument
        // to that call, not as the block's own return value ("only one lambda expression is
        // allowed outside a parenthesized argument list").
        val remapProfileId: (Long) -> Long = if (payload.profiles.isNotEmpty()) {
            payload.profiles.forEach { database.profileDao().upsert(it) }
            val identity: (Long) -> Long = { id -> id }
            identity
        } else {
            val now = System.currentTimeMillis()
            val legacyProfileId = database.profileDao().upsert(ProfileEntity(name = "Perso", sortOrder = 0, createdAtEpochMillis = now))
            database.profileDao().upsert(ProfileEntity(name = "Pro", sortOrder = 1, createdAtEpochMillis = now))
            database.profileDao().upsert(ProfileEntity(name = "Commun", sortOrder = 2, createdAtEpochMillis = now))
            val toLegacyProfile: (Long) -> Long = { _ -> legacyProfileId }
            toLegacyProfile
        }

        payload.accounts.forEach { database.accountDao().upsert(it.copy(profileId = remapProfileId(it.profileId))) }
        payload.categories.forEach { database.categoryDao().upsert(it.copy(profileId = remapProfileId(it.profileId))) }
        payload.incomes.forEach { database.incomeDao().upsert(it.copy(profileId = remapProfileId(it.profileId))) }
        payload.recurringExpenses.forEach { database.recurringExpenseDao().upsert(it.copy(profileId = remapProfileId(it.profileId))) }
        payload.variableBudgets.forEach { database.variableBudgetDao().upsert(it.copy(profileId = remapProfileId(it.profileId))) }
        payload.savingsGoals.forEach { database.savingsGoalDao().upsert(it.copy(profileId = remapProfileId(it.profileId))) }
        payload.transactions.forEach { database.transactionDao().upsert(it.copy(profileId = remapProfileId(it.profileId))) }
    }
}
