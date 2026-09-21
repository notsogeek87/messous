package com.budgetflow.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.budgetflow.app.data.local.dao.AccountDao
import com.budgetflow.app.data.local.dao.CategoryDao
import com.budgetflow.app.data.local.dao.IncomeDao
import com.budgetflow.app.data.local.dao.ProfileDao
import com.budgetflow.app.data.local.dao.RecurringExpenseDao
import com.budgetflow.app.data.local.dao.SavingsGoalDao
import com.budgetflow.app.data.local.dao.TransactionDao
import com.budgetflow.app.data.local.dao.VariableBudgetDao
import com.budgetflow.app.data.local.entity.AccountEntity
import com.budgetflow.app.data.local.entity.CategoryEntity
import com.budgetflow.app.data.local.entity.IncomeEntity
import com.budgetflow.app.data.local.entity.ProfileEntity
import com.budgetflow.app.data.local.entity.RecurringExpenseEntity
import com.budgetflow.app.data.local.entity.SavingsGoalEntity
import com.budgetflow.app.data.local.entity.TransactionEntity
import com.budgetflow.app.data.local.entity.VariableBudgetEntity

/**
 * The single local Room database backing BudgetFlow. There is no remote or
 * cloud-synced counterpart: this file, at [DATABASE_NAME], is the only place
 * the user's financial data lives unless they explicitly export it.
 */
@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        IncomeEntity::class,
        RecurringExpenseEntity::class,
        VariableBudgetEntity::class,
        TransactionEntity::class,
        SavingsGoalEntity::class,
        ProfileEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class BudgetFlowDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun incomeDao(): IncomeDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun variableBudgetDao(): VariableBudgetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun profileDao(): ProfileDao

    companion object {
        private const val DATABASE_NAME = "budgetflow.db"

        /**
         * Introduces profiles (Perso/Pro/Commun) and walls off every existing table behind a
         * profileId column. Every row that predates this migration becomes "Perso" - the spec's
         * default profile - and Pro/Commun each get a copy of the starter categories so they
         * aren't left empty.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profiles` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL, " +
                        "`createdAtEpochMillis` INTEGER NOT NULL)"
                )
                val now = System.currentTimeMillis()
                db.execSQL("INSERT INTO `profiles` (`id`, `name`, `sortOrder`, `createdAtEpochMillis`) VALUES (1, 'Perso', 0, $now)")
                db.execSQL("INSERT INTO `profiles` (`id`, `name`, `sortOrder`, `createdAtEpochMillis`) VALUES (2, 'Pro', 1, $now)")
                db.execSQL("INSERT INTO `profiles` (`id`, `name`, `sortOrder`, `createdAtEpochMillis`) VALUES (3, 'Commun', 2, $now)")

                listOf("accounts", "categories", "incomes", "recurring_expenses", "variable_budgets", "transactions", "savings_goals")
                    .forEach { table ->
                        db.execSQL("ALTER TABLE `$table` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 0")
                        db.execSQL("UPDATE `$table` SET `profileId` = 1")
                    }

                db.execSQL(
                    "INSERT INTO `categories` (`name`, `group`, `icon`, `isDefault`, `profileId`) " +
                        "SELECT `name`, `group`, `icon`, `isDefault`, 2 FROM `categories` WHERE `profileId` = 1"
                )
                db.execSQL(
                    "INSERT INTO `categories` (`name`, `group`, `icon`, `isDefault`, `profileId`) " +
                        "SELECT `name`, `group`, `icon`, `isDefault`, 3 FROM `categories` WHERE `profileId` = 1"
                )
            }
        }

        @Volatile
        private var instance: BudgetFlowDatabase? = null

        fun getInstance(context: Context): BudgetFlowDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BudgetFlowDatabase::class.java,
                    DATABASE_NAME
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
