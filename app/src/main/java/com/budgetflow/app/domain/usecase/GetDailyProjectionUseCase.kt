package com.budgetflow.app.domain.usecase

import com.budgetflow.engine.BudgetEngine
import com.budgetflow.engine.model.DailyProjection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

/** Powers "Mon futur" (spec sections 8 & 9): the day-by-day running balance for the current month. */
class GetDailyProjectionUseCase(private val monthPlanSource: GetDashboardForMonthUseCase) {

    fun observe(today: LocalDate = LocalDate.now()): Flow<List<DailyProjection>> =
        monthPlanSource.observePlan(YearMonth.from(today), today).map(BudgetEngine::projectDailyBalances)
}
