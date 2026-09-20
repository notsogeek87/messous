package com.budgetflow.app.data.backup

import com.budgetflow.app.data.local.BudgetFlowDatabase
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

        payload.accounts.forEach { database.accountDao().upsert(it) }
        payload.categories.forEach { database.categoryDao().upsert(it) }
        payload.incomes.forEach { database.incomeDao().upsert(it) }
        payload.recurringExpenses.forEach { database.recurringExpenseDao().upsert(it) }
        payload.variableBudgets.forEach { database.variableBudgetDao().upsert(it) }
        payload.savingsGoals.forEach { database.savingsGoalDao().upsert(it) }
        payload.transactions.forEach { database.transactionDao().upsert(it) }
    }
}
