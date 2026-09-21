package com.budgetflow.app.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Deletes [item] right away, then offers "Annuler" for a few seconds through [snackbarHostState]
 * instead of asking before the fact - a single tap stays a single tap, but is never final in
 * silence. Every repository's `upsert` REPLACEs by id (see the Room DAOs), so re-upserting the
 * exact object just deleted genuinely restores it: same id, same timestamps.
 */
fun <T> CoroutineScope.deleteWithUndo(
    item: T,
    message: String,
    undoLabel: String,
    snackbarHostState: SnackbarHostState,
    delete: suspend (T) -> Unit,
    restore: suspend (T) -> Unit
) {
    launch {
        delete(item)
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            withDismissAction = true
        )
        if (result == SnackbarResult.ActionPerformed) {
            restore(item)
        }
    }
}
