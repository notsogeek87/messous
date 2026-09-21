package com.budgetflow.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.budgetflow.app.R
import com.budgetflow.app.ui.theme.NegativeRed
import com.budgetflow.app.ui.theme.NeutralAmber
import com.budgetflow.app.ui.theme.PositiveGreen
import com.budgetflow.engine.model.FreedomState

/** Color for a [FreedomState] - never the only signal (spec section 24: always paired with icon + text). */
@Composable
fun FreedomState.color(): Color = when (this) {
    FreedomState.COMFORT -> PositiveGreen
    FreedomState.CAUTION -> NeutralAmber
    FreedomState.ALERT -> NegativeRed
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
 * The "seuil de sécurité" bar (spec section 5): free money as a filled bar, with a marker at
 * the safety threshold and the margin called out. Never the sole way the margin is conveyed -
 * the numeric values are always shown alongside it.
 */
@Composable
fun SafetyThresholdGauge(freeMoney: Double, safetyThreshold: Double, state: FreedomState, modifier: Modifier = Modifier) {
    val scale = maxOf(freeMoney, safetyThreshold, 1.0)
    val filledFraction by animateFloatAsState(
        targetValue = (freeMoney / scale).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "gaugeFill"
    )
    val markerFraction = (safetyThreshold / scale).toFloat().coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val fillColor = state.color()
    val markerColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
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
                val margin = freeMoney - safetyThreshold
                Text(
                    stringResource(
                        if (margin >= 0) R.string.liberty_safety_margin_positive else R.string.liberty_safety_margin_negative,
                        formatMoney(kotlin.math.abs(margin))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (margin >= 0) MaterialTheme.colorScheme.onSurfaceVariant else NegativeRed
                )
            }
        }
    }
}

/**
 * "Reste à vivre" spread across the month as a gauge (follow-up to spec section 3): the fill is
 * how much of [remainingForMonth] is actually backed by income/fixed expenses whose date has
 * already passed ([remainingToday]), while the marker is simply how far [dayOfMonth] is through
 * [totalDaysInMonth]. When the marker sits ahead of the fill, real money is running behind the
 * calendar - a bill landed before the paycheck that covers it, for instance - never the reverse.
 */
@Composable
fun DailyRemainingGauge(
    remainingToday: Double,
    remainingForMonth: Double,
    dayOfMonth: Int,
    totalDaysInMonth: Int,
    state: FreedomState,
    modifier: Modifier = Modifier
) {
    val scale = maxOf(remainingForMonth, remainingToday, 1.0)
    val filledFraction by animateFloatAsState(
        targetValue = (remainingToday / scale).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "dailyGaugeFill"
    )
    val dayFraction = if (totalDaysInMonth > 0) {
        (dayOfMonth.toFloat() / totalDaysInMonth).coerceIn(0f, 1f)
    } else {
        0f
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val fillColor = state.color()
    val markerColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
            val barHeight = size.height
            drawRoundRect(color = trackColor, cornerRadius = CornerRadius(barHeight / 2, barHeight / 2))
            if (filledFraction > 0f) {
                drawRoundRect(
                    color = fillColor,
                    size = size.copy(width = size.width * filledFraction),
                    cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
                )
            }
            val markerX = size.width * dayFraction
            drawLine(
                color = markerColor,
                start = androidx.compose.ui.geometry.Offset(markerX, 0f),
                end = androidx.compose.ui.geometry.Offset(markerX, barHeight),
                strokeWidth = 4f
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.liberty_day_gauge_day_marker, dayOfMonth, totalDaysInMonth),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.liberty_day_gauge_month_target, formatMoney(remainingForMonth)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
