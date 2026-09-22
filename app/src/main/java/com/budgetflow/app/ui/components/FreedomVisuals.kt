package com.budgetflow.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.budgetflow.app.R
import com.budgetflow.app.ui.theme.negativeRed
import com.budgetflow.app.ui.theme.neutralAmber
import com.budgetflow.app.ui.theme.positiveGreen
import com.budgetflow.engine.model.FreedomState

/** Color for a [FreedomState] - never the only signal (spec section 24: always paired with icon + text). */
@Composable
fun FreedomState.color(): Color = when (this) {
    FreedomState.COMFORT -> positiveGreen
    FreedomState.CAUTION -> neutralAmber
    FreedomState.ALERT -> negativeRed
}

fun FreedomState.icon() = when (this) {
    FreedomState.COMFORT -> Icons.Filled.CheckCircle
    FreedomState.CAUTION -> Icons.Filled.Bolt
    FreedomState.ALERT -> Icons.Filled.WarningAmber
}

@Composable
fun FreedomState.label(): String = stringResource(
    when (this) {
        FreedomState.COMFORT -> R.string.freedom_state_comfort
        FreedomState.CAUTION -> R.string.freedom_state_caution
        FreedomState.ALERT -> R.string.freedom_state_alert
    }
)

/** Small pill combining icon + text for a [FreedomState] - the state is never conveyed by color alone. */
@Composable
fun FreedomStateBadge(state: FreedomState, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(state.icon(), contentDescription = null, tint = state.color())
        Text(state.label(), color = state.color(), style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * The "seuil de sécurité" bar (spec section 5): the plan's reste à vivre as a filled bar, with a
 * marker at the safety threshold and the margin called out. Never the sole way the margin is
 * conveyed - the numeric values are always shown alongside it, and the whole gauge carries one
 * merged [contentDescription] for TalkBack since the [Canvas] drawing itself is otherwise
 * invisible to it.
 */
@Composable
fun SafetyThresholdGauge(amount: Double, safetyThreshold: Double, state: FreedomState, modifier: Modifier = Modifier) {
    val scale = maxOf(amount, safetyThreshold, 1.0)
    val filledFraction by animateFloatAsState(
        targetValue = (amount / scale).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "gaugeFill"
    )
    val markerFraction = (safetyThreshold / scale).toFloat().coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val fillColor = state.color()
    val markerColor = MaterialTheme.colorScheme.onSurface
    val margin = amount - safetyThreshold
    val gaugeDescription = if (safetyThreshold > 0.0) {
        stringResource(
            if (margin >= 0) R.string.liberty_safety_gauge_description_positive else R.string.liberty_safety_gauge_description_negative,
            formatMoney(amount),
            formatMoney(safetyThreshold),
            formatMoney(kotlin.math.abs(margin))
        )
    } else {
        stringResource(R.string.liberty_safety_gauge_description_no_threshold, formatMoney(amount))
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clearAndSetSemantics { contentDescription = gaugeDescription }
        ) {
            val barHeight = size.height
            drawRoundRect(color = trackColor, cornerRadius = CornerRadius(barHeight / 2, barHeight / 2))
            if (filledFraction > 0f) {
                drawRoundRect(
                    color = fillColor,
                    size = size.copy(width = size.width * filledFraction),
                    cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
                )
            }
            if (safetyThreshold > 0.0) {
                val markerX = size.width * markerFraction
                drawLine(
                    color = markerColor,
                    start = androidx.compose.ui.geometry.Offset(markerX, 0f),
                    end = androidx.compose.ui.geometry.Offset(markerX, barHeight),
                    strokeWidth = 4f
                )
            }
        }
        if (safetyThreshold > 0.0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.liberty_safety_threshold_label, formatMoney(safetyThreshold)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(
                        if (margin >= 0) R.string.liberty_safety_margin_positive else R.string.liberty_safety_margin_negative,
                        formatMoney(kotlin.math.abs(margin))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (margin >= 0) MaterialTheme.colorScheme.onSurfaceVariant else negativeRed
                )
            }
        }
    }
}
