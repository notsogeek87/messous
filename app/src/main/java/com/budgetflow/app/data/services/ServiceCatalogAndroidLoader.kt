package com.budgetflow.app.data.services

import android.content.Context
import android.util.Log
import com.budgetflow.engine.recognition.RecognizableService

private const val ASSET_NAME = "services.json"
private const val LOG_TAG = "ServiceCatalog"

/**
 * Reads the offline service catalog bundled in the APK (spec section 1: no network, no server,
 * works with airplane mode / GrapheneOS). A malformed catalog must never crash the app for a real
 * user - [ServiceCatalogLoader.loadFrom] already fails loudly in unit tests for that case - so
 * here it degrades to an empty catalog (recognition simply stays quiet) and logs the reason.
 */
fun loadServiceCatalogFromAssets(context: Context): List<RecognizableService> = try {
    val text = context.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
    ServiceCatalogLoader.loadFrom(text)
} catch (error: Exception) {
    Log.w(LOG_TAG, "Could not load $ASSET_NAME, service recognition will be disabled", error)
    emptyList()
}
