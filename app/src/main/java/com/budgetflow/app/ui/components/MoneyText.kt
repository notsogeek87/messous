package com.budgetflow.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.budgetflow.app.ui.theme.NegativeRed
import com.budgetflow.app.ui.theme.PositiveGreen
import java.text.NumberFormat
import java.util.Locale

private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE)

fun formatMoney(amount: Double): String = currencyFormat.format(amount)

/** Renders an amount, optionally coloring it green/red based on its sign. */
@Composable
fun MoneyText(
    amount: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    colorBySign: Boolean = false,
    color: Color = Color.Unspecified
) {
    val resolvedColor = when {
        color != Color.Unspecified -> color
        !colorBySign -> MaterialTheme.colorScheme.onSurface
        amount > 0 -> PositiveGreen
        amount < 0 -> NegativeRed
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(text = formatMoney(amount), modifier = modifier, style = style, color = resolvedColor)
}

/**
 * Same as [MoneyText], but counts smoothly from its previous value to [amount] whenever it
 * changes, instead of jumping - the headline figure across "Ma liberté" and "Et si...?" should
 * always visibly move, never silently swap (spec section 17: animations that help understand
 * a financial change, not gadget animations).
 */
@Composable
fun AnimatedMoneyText(
    amount: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    colorBySign: Boolean = false,
    color: Color = Color.Unspecified,
    durationMillis: Int = 500
) {
    val animated by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = tween(durationMillis = durationMillis),
        label = "animatedMoney"
    )
    MoneyText(amount = animated.toDouble(), modifier = modifier, style = style, colorBySign = colorBySign, color = color)
}
