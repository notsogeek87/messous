package com.budgetflow.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector
import com.budgetflow.app.R

object Routes {
    const val ONBOARDING = "onboarding"

    // The four primary spaces (spec section 2).
    const val LIBERTY = "liberty"
    const val FUTURE = "future"
    const val WHATIF = "whatif"
    const val ME = "me"

    // Secondary screens, reached from Moi or from within a primary space.
    const val TRANSACTIONS = "transactions"
    const val ADD_TRANSACTION = "add_transaction"
    const val ADD_TRANSACTION_WITH_ID = "add_transaction?transactionId={transactionId}"
    const val BUDGET = "budget"
    const val STATISTICS = "statistics"
    const val ACCOUNTS = "accounts"
    const val CATEGORIES = "categories"
    const val ADD_INCOME = "add_income"
    const val ADD_INCOME_WITH_ID = "add_income?incomeId={incomeId}"
    const val ADD_EXPENSE = "add_expense"
    const val ADD_EXPENSE_WITH_ID = "add_expense?expenseId={expenseId}"

    fun editTransaction(id: Long) = "add_transaction?transactionId=$id"
    fun editIncome(id: Long) = "add_income?incomeId=$id"
    fun editExpense(id: Long) = "add_expense?expenseId=$id"
}

/** The four primary destinations shown in the bottom navigation bar (spec section 2). */
data class BottomNavItem(val route: String, val labelRes: Int, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Routes.LIBERTY, R.string.nav_liberty, Icons.Filled.AccountBalanceWallet),
    BottomNavItem(Routes.FUTURE, R.string.nav_future, Icons.Filled.Timeline),
    BottomNavItem(Routes.WHATIF, R.string.nav_whatif, Icons.Filled.Lightbulb),
    BottomNavItem(Routes.ME, R.string.nav_me, Icons.Filled.Person)
)
