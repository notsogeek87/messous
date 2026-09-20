package com.budgetflow.app.data.backup

import com.budgetflow.app.data.local.entity.AccountEntity
import com.budgetflow.app.data.local.entity.CategoryEntity
import com.budgetflow.app.data.local.entity.IncomeEntity
import com.budgetflow.app.data.local.entity.RecurringExpenseEntity
import com.budgetflow.app.data.local.entity.SavingsGoalEntity
import com.budgetflow.app.data.local.entity.TransactionEntity
import com.budgetflow.app.data.local.entity.VariableBudgetEntity
import kotlinx.serialization.Serializable

/**
 * The full content of a BudgetFlow export. This is a plain, documented JSON
 * document (spec section 16: "l'export doit être lisible et documenté") -
 * every field name matches the underlying table so the file can be read or
 * edited by hand if needed.
 */
@Serializable
data class BackupPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtEpochMillis: Long,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val incomes: List<IncomeEntity> = emptyList(),
    val recurringExpenses: List<RecurringExpenseEntity> = emptyList(),
    val variableBudgets: List<VariableBudgetEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
