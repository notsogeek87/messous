package com.budgetflow.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.budgetflow.app.data.local.dao.AccountDao
import com.budgetflow.app.data.local.dao.CategoryDao
import com.budgetflow.app.data.local.dao.IncomeDao
import com.budgetflow.app.data.local.dao.RecurringExpenseDao
import com.budgetflow.app.data.local.dao.SavingsGoalDao
import com.budgetflow.app.data.local.dao.TransactionDao
import com.budgetflow.app.data.local.dao.VariableBudgetDao
import com.budgetflow.app.data.local.entity.AccountEntity
import com.budgetflow.app.data.local.entity.CategoryEntity
import com.budgetflow.app.data.local.entity.IncomeEntity
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
        SavingsGoalEntity::class
    ],
    version = 1,
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

    companion object {
        private const val DATABASE_NAME = "budgetflow.db"

        @Volatile
        private var instance: BudgetFlowDatabase? = null

        fun getInstance(context: Context): BudgetFlowDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BudgetFlowDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
    }
}
