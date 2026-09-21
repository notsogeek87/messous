package com.budgetflow.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import com.budgetflow.app.R

object Routes {
    const val ONBOARDING = "onboarding"

    // The four primary spaces (spec section 2).
    const val LIBERTY = "liberty"
    const val FUTURE = "future"
    const val WHATIF = "whatif"
    const val ME = "me"

    // Secondary screens, reached from "Mon budget" or from within a primary space.
    const val TRANSACTIONS = "transactions"
    const val ADD_TRANSACTION = "add_transaction"
    const val ADD_TRANSACTION_WITH_ID = "add_transaction?transactionId={transactionId}"
    const val BUDGET = "budget?tab={tab}"
    const val STATISTICS = "statistics"
    const val ACCOUNTS = "accounts"
    const val CATEGORIES = "categories"
    const val PROFILES = "profiles"
    const val SETTINGS = "settings"
    const val ADD_INCOME = "add_income"
    const val ADD_INCOME_WITH_ID = "add_income?incomeId={incomeId}"
    const val ADD_EXPENSE = "add_expense"
    const val ADD_EXPENSE_WITH_ID = "add_expense?expenseId={expenseId}"

    // Budget's four tabs, for deep-linking straight to one from "Mon budget" (spec §10.2 - "liens
    // directs vers chaque onglet de Budget" instead of always landing on "Revenus").
    const val BUDGET_TAB_INCOMES = 0
    const val BUDGET_TAB_EXPENSES = 1
    const val BUDGET_TAB_ENVELOPES = 2
    const val BUDGET_TAB_GOALS = 3

    fun editTransaction(id: Long) = "add_transaction?transactionId=$id"
    fun editIncome(id: Long) = "add_income?incomeId=$id"
    fun editExpense(id: Long) = "add_expense?expenseId=$id"
    fun budget(tab: Int = BUDGET_TAB_INCOMES) = "budget?tab=$tab"
}

/** The four primary destinations shown in the bottom navigation bar (spec section 2). */
data class BottomNavItem(val route: String, val labelRes: Int, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Routes.LIBERTY, R.string.nav_liberty, Icons.Filled.Today),
    BottomNavItem(Routes.FUTURE, R.string.nav_future, Icons.Filled.Timeline),
    BottomNavItem(Routes.WHATIF, R.string.nav_whatif, Icons.Filled.Lightbulb),
    BottomNavItem(Routes.ME, R.string.nav_me, Icons.Filled.AccountBalanceWallet)
)
