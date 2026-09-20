package com.budgetflow.app

import android.app.Application
import com.budgetflow.app.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BudgetFlowApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        applicationScope.launch {
            ServiceLocator.categoryRepository.seedDefaultsIfEmpty()
        }
    }
}
