package com.budgetflow.app.ui.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.budgetflow.app.ui.budget.BudgetScreen
import com.budgetflow.app.ui.calendar.CalendarScreen
import com.budgetflow.app.ui.categories.CategoriesScreen
import com.budgetflow.app.ui.dashboard.DashboardScreen
import com.budgetflow.app.ui.settings.SettingsScreen
import com.budgetflow.app.ui.statistics.StatisticsScreen
import com.budgetflow.app.ui.transactions.AddEditTransactionScreen
import com.budgetflow.app.ui.transactions.TransactionsScreen

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
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                    onOpenBudget = {
                        navController.navigate(Routes.BUDGET) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(
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
            composable(Routes.BUDGET) { BudgetScreen() }
            composable(Routes.STATISTICS) { StatisticsScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) }
                )
            }
            composable(Routes.CALENDAR) { CalendarScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ACCOUNTS) { AccountsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.CATEGORIES) { CategoriesScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
