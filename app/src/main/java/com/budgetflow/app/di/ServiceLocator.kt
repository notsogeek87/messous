package com.budgetflow.app.di

import android.content.Context
import com.budgetflow.app.data.backup.BackupRepositoryImpl
import com.budgetflow.app.data.local.BudgetFlowDatabase
import com.budgetflow.app.data.prefs.UserPreferences
import com.budgetflow.app.data.profile.CurrentProfileProvider
import com.budgetflow.app.data.repository.AccountRepositoryImpl
import com.budgetflow.app.data.repository.CategoryRepositoryImpl
import com.budgetflow.app.data.repository.IncomeRepositoryImpl
import com.budgetflow.app.data.repository.ProfileRepositoryImpl
import com.budgetflow.app.data.repository.RecurringExpenseRepositoryImpl
import com.budgetflow.app.data.repository.SavingsGoalRepositoryImpl
import com.budgetflow.app.data.repository.TransactionRepositoryImpl
import com.budgetflow.app.data.repository.VariableBudgetRepositoryImpl
import com.budgetflow.app.data.services.ServiceCategoryMatcher
import com.budgetflow.app.data.services.loadServiceCatalogFromAssets
import com.budgetflow.app.domain.repository.AccountRepository
import com.budgetflow.app.domain.repository.BackupRepository
import com.budgetflow.app.domain.repository.CategoryRepository
import com.budgetflow.app.domain.repository.IncomeRepository
import com.budgetflow.app.domain.repository.ProfileRepository
import com.budgetflow.app.domain.repository.RecurringExpenseRepository
import com.budgetflow.app.domain.repository.SavingsGoalRepository
import com.budgetflow.app.domain.repository.TransactionRepository
import com.budgetflow.app.domain.repository.VariableBudgetRepository
import com.budgetflow.app.domain.usecase.GetCalendarOccurrencesUseCase
import com.budgetflow.app.domain.usecase.GetDailyProjectionUseCase
import com.budgetflow.app.domain.usecase.GetDashboardForMonthUseCase
import com.budgetflow.app.domain.usecase.GetMonthlyForecastsUseCase
import com.budgetflow.app.domain.usecase.SimulateExpenseUseCase

/**
 * Hand-rolled dependency container. BudgetFlow deliberately avoids an
 * annotation-processing DI framework: the dependency graph is small and
 * static, so a plain lazily-initialized singleton object is easier to read,
 * easier to test against, and keeps the build simpler.
 */
object ServiceLocator {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val database: BudgetFlowDatabase by lazy { BudgetFlowDatabase.getInstance(appContext) }
    val preferences: UserPreferences by lazy { UserPreferences(appContext) }

    /** The single source every profile-scoped repository reads the active profile from - see its own doc. */
    val currentProfileProvider: CurrentProfileProvider by lazy {
        CurrentProfileProvider(preferences, database.profileDao(), database.categoryDao())
    }

    val accountRepository: AccountRepository by lazy {
        AccountRepositoryImpl(database.accountDao(), database.transactionDao(), currentProfileProvider)
    }
    val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database.categoryDao(), currentProfileProvider)
    }
    val incomeRepository: IncomeRepository by lazy { IncomeRepositoryImpl(database.incomeDao(), currentProfileProvider) }
    val recurringExpenseRepository: RecurringExpenseRepository by lazy {
        RecurringExpenseRepositoryImpl(database.recurringExpenseDao(), currentProfileProvider)
    }
    val variableBudgetRepository: VariableBudgetRepository by lazy {
        VariableBudgetRepositoryImpl(database.variableBudgetDao(), database.transactionDao(), currentProfileProvider)
    }
    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database.transactionDao(), currentProfileProvider)
    }
    val savingsGoalRepository: SavingsGoalRepository by lazy {
        SavingsGoalRepositoryImpl(database.savingsGoalDao(), currentProfileProvider)
    }
    val profileRepository: ProfileRepository by lazy {
        ProfileRepositoryImpl(database, preferences, currentProfileProvider, categoryRepository)
    }
    val backupRepository: BackupRepository by lazy { BackupRepositoryImpl(database) }

    /** The local, offline service catalog (Netflix, Spotify, EDF, ...) bundled in assets/services.json. */
    val serviceCatalog by lazy { loadServiceCatalogFromAssets(appContext) }
    val serviceCategoryMatcher: ServiceCategoryMatcher by lazy { ServiceCategoryMatcher(categoryRepository) }

    val dashboardUseCase: GetDashboardForMonthUseCase by lazy {
        GetDashboardForMonthUseCase(
            incomeRepository, recurringExpenseRepository, variableBudgetRepository,
            savingsGoalRepository, accountRepository, transactionRepository, preferences
        )
    }
    val calendarUseCase: GetCalendarOccurrencesUseCase by lazy {
        GetCalendarOccurrencesUseCase(incomeRepository, recurringExpenseRepository)
    }
    val forecastUseCase: GetMonthlyForecastsUseCase by lazy {
        GetMonthlyForecastsUseCase(incomeRepository, recurringExpenseRepository, variableBudgetRepository, savingsGoalRepository)
    }
    val simulateExpenseUseCase: SimulateExpenseUseCase by lazy { SimulateExpenseUseCase(dashboardUseCase) }
    val dailyProjectionUseCase: GetDailyProjectionUseCase by lazy { GetDailyProjectionUseCase(dashboardUseCase) }
}
