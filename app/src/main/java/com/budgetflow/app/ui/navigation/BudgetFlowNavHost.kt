package com.budgetflow.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.budgetflow.app.ui.accounts.AccountsScreen
import com.budgetflow.app.ui.budget.AddEditExpenseScreen
import com.budgetflow.app.ui.budget.AddEditIncomeScreen
import com.budgetflow.app.ui.budget.BudgetScreen
import com.budgetflow.app.ui.categories.CategoriesScreen
import com.budgetflow.app.ui.future.FutureScreen
import com.budgetflow.app.ui.hub.MonBudgetScreen
import com.budgetflow.app.ui.liberty.LibertyScreen
import com.budgetflow.app.ui.profiles.ProfilesScreen
import com.budgetflow.app.ui.settings.SettingsScreen
import com.budgetflow.app.ui.statistics.StatisticsScreen
import com.budgetflow.app.ui.transactions.AddEditTransactionScreen
import com.budgetflow.app.ui.transactions.TransactionsScreen
import com.budgetflow.app.ui.whatif.WhatIfScreen

/** The main app shell, shown once onboarding is complete: bottom nav + all secondary screens pushed on top. */
@Composable
fun BudgetFlowNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val showBottomBar = currentRoute?.hierarchy?.any { dest -> bottomNavItems.any { it.route == dest.route } } == true

    Scaffold(
        bottomBar = { if (showBottomBar) BudgetFlowBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBERTY,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.LIBERTY) {
                LibertyScreen(
                    onOpenBudget = {
                        navController.navigate(Routes.budget()) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenFuture = {
                        navController.navigate(Routes.FUTURE) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenWhatIf = {
                        navController.navigate(Routes.WHATIF) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddTransaction = { navController.navigate(Routes.ADD_TRANSACTION) },
                    onAddIncome = { navController.navigate(Routes.ADD_INCOME) },
                    onAddExpense = { navController.navigate(Routes.ADD_EXPENSE) }
                )
            }
            composable(Routes.FUTURE) { FutureScreen() }
            composable(Routes.WHATIF) { WhatIfScreen() }
            composable(Routes.ME) {
                MonBudgetScreen(
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenIncomes = { navController.navigate(Routes.budget(Routes.BUDGET_TAB_INCOMES)) },
                    onOpenExpenses = { navController.navigate(Routes.budget(Routes.BUDGET_TAB_EXPENSES)) },
                    onOpenEnvelopes = { navController.navigate(Routes.budget(Routes.BUDGET_TAB_ENVELOPES)) },
                    onOpenGoals = { navController.navigate(Routes.budget(Routes.BUDGET_TAB_GOALS)) },
                    onOpenTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                    onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                    onOpenStatistics = { navController.navigate(Routes.STATISTICS) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenProfiles = { navController.navigate(Routes.PROFILES) }
                )
            }

            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(
                    onBack = { navController.popBackStack() },
                    onAddTransaction = { navController.navigate(Routes.ADD_TRANSACTION) },
                    onEditTransaction = { id -> navController.navigate(Routes.editTransaction(id)) }
                )
            }
            composable(Routes.ADD_TRANSACTION) {
                AddEditTransactionScreen(transactionId = null, onDone = { navController.popBackStack() })
            }
            composable(
                Routes.ADD_TRANSACTION_WITH_ID,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("transactionId")
                AddEditTransactionScreen(transactionId = id, onDone = { navController.popBackStack() })
            }
            composable(
                Routes.BUDGET,
                arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = Routes.BUDGET_TAB_INCOMES })
            ) { entry ->
                val tab = entry.arguments?.getInt("tab") ?: Routes.BUDGET_TAB_INCOMES
                BudgetScreen(
                    onBack = { navController.popBackStack() },
                    initialTab = tab,
                    onAddIncome = { navController.navigate(Routes.ADD_INCOME) },
                    onEditIncome = { id -> navController.navigate(Routes.editIncome(id)) },
                    onAddExpense = { navController.navigate(Routes.ADD_EXPENSE) },
                    onEditExpense = { id -> navController.navigate(Routes.editExpense(id)) }
                )
            }
            composable(Routes.ADD_INCOME) {
                AddEditIncomeScreen(incomeId = null, onDone = { navController.popBackStack() })
            }
            composable(
                Routes.ADD_INCOME_WITH_ID,
                arguments = listOf(navArgument("incomeId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("incomeId")
                AddEditIncomeScreen(incomeId = id, onDone = { navController.popBackStack() })
            }
            composable(Routes.ADD_EXPENSE) {
                AddEditExpenseScreen(expenseId = null, onDone = { navController.popBackStack() })
            }
            composable(
                Routes.ADD_EXPENSE_WITH_ID,
                arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("expenseId")
                AddEditExpenseScreen(expenseId = id, onDone = { navController.popBackStack() })
            }
            composable(Routes.STATISTICS) {
                StatisticsScreen(
                    onBack = { navController.popBackStack() },
                    onAddTransaction = { navController.navigate(Routes.ADD_TRANSACTION) }
                )
            }
            composable(Routes.ACCOUNTS) { AccountsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.CATEGORIES) { CategoriesScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.PROFILES) { ProfilesScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
