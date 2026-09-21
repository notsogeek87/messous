package com.budgetflow.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.model.Transaction
import com.budgetflow.app.domain.model.TransactionType
import com.budgetflow.app.domain.repository.CategoryRepository
import com.budgetflow.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class CategorySpendingSlice(val category: Category?, val amount: Double, val fraction: Float)
data class MonthTotals(val month: YearMonth, val income: Double, val expense: Double)

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val categorySlices: List<CategorySpendingSlice> = emptyList(),
    val currentIncome: Double = 0.0,
    val currentExpense: Double = 0.0,
    val previousIncome: Double = 0.0,
    val previousExpense: Double = 0.0,
    val monthlyHistory: List<MonthTotals> = emptyList()
) {
    val savedThisMonth: Double get() = currentIncome - currentExpense
    val expenseDeltaVsLastMonth: Double get() = currentExpense - previousExpense
}

class StatisticsViewModel(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeCategories()
    ) { transactions, categories ->
        buildState(transactions, categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsUiState())

    private fun buildState(transactions: List<Transaction>, categories: List<Category>): StatisticsUiState {
        val currentMonth = YearMonth.now()
        val previousMonth = currentMonth.minusMonths(1)

        fun inMonth(t: Transaction, month: YearMonth) = YearMonth.from(t.date) == month
        fun total(month: YearMonth, type: TransactionType) =
            transactions.filter { inMonth(it, month) && it.type == type }.sumOf { it.amount }

        val currentIncome = total(currentMonth, TransactionType.INCOME)
        val currentExpense = total(currentMonth, TransactionType.EXPENSE)
        val previousIncome = total(previousMonth, TransactionType.INCOME)
        val previousExpense = total(previousMonth, TransactionType.EXPENSE)

        val expensesByCategory = transactions
            .filter { inMonth(it, currentMonth) && it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val totalExpense = expensesByCategory.values.sum()
        val slices = expensesByCategory.entries
            .sortedByDescending { it.value }
            .map { (categoryId, amount) ->
                CategorySpendingSlice(
                    category = categories.firstOrNull { it.id == categoryId },
                    amount = amount,
                    fraction = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                )
            }

        val monthlyHistory = (5 downTo 0).map { offset ->
            val month = currentMonth.minusMonths(offset.toLong())
            MonthTotals(month, total(month, TransactionType.INCOME), total(month, TransactionType.EXPENSE))
        }

        return StatisticsUiState(
            isLoading = false,
            categorySlices = slices,
            currentIncome = currentIncome,
            currentExpense = currentExpense,
            previousIncome = previousIncome,
            previousExpense = previousExpense,
            monthlyHistory = monthlyHistory
        )
    }
}
