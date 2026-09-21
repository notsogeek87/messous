package com.budgetflow.app.ui.whatif

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetflow.app.domain.model.SavingsGoal
import com.budgetflow.app.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives "Que faire de cette somme ?" (spec §13, audit §9/Lot 4 P19): unlike the throwaway
 * simulation in "Et si...?", every card here acts on the user's real savings goals - [contribute]
 * is a genuine write, not a preview, so a scenario the user picks is never a dead end.
 */
class AllocateViewModel(private val savingsGoalRepository: SavingsGoalRepository) : ViewModel() {

    val goals: StateFlow<List<SavingsGoal>> = savingsGoalRepository.observeGoals()
        .map { goals -> goals.filter { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun contribute(goal: SavingsGoal, amount: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            savingsGoalRepository.upsert(goal.copy(currentAmount = goal.currentAmount + amount))
            onDone()
        }
    }
}
