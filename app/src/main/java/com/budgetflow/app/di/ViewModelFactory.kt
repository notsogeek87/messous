package com.budgetflow.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** A one-off [ViewModelProvider.Factory] that just calls [create]. Used with `viewModel(factory = ...)` in each screen. */
fun <VM : ViewModel> simpleViewModelFactory(create: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }
