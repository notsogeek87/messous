package com.budgetflow.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.budgetflow.app.R

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val ADD_TRANSACTION = "add_transaction"
    const val ADD_TRANSACTION_WITH_ID = "add_transaction?transactionId={transactionId}"
    const val BUDGET = "budget"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"
    const val CALENDAR = "calendar"
    const val ACCOUNTS = "accounts"
    const val CATEGORIES = "categories"

    fun editTransaction(id: Long) = "add_transaction?transactionId=$id"
}

/** The five primary destinations shown in the bottom navigation bar (spec section 14). */
data class BottomNavItem(val route: String, val labelRes: Int, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.Home),
    BottomNavItem(Routes.TRANSACTIONS, R.string.nav_transactions, Icons.Filled.Receipt),
    BottomNavItem(Routes.BUDGET, R.string.nav_budget, Icons.Filled.AccountBalanceWallet),
    BottomNavItem(Routes.STATISTICS, R.string.nav_statistics, Icons.Filled.BarChart),
    BottomNavItem(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings)
)
