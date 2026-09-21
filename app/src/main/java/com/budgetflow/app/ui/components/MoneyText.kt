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
import com.budgetflow.app.ui.theme.negativeRed
import com.budgetflow.app.ui.theme.positiveGreen
import java.text.NumberFormat
import java.util.Locale

// Messous is single-currency (EUR) by design: no account/transaction carries a currency of its
// own to format against (see Account.currency, kept only as a free-text label). One formatter,
// one symbol, everywhere.
private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE)
private val currencyFormatRounded: NumberFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE).apply {
    maximumFractionDigits = 0
    minimumFractionDigits = 0
}

/** [roundToEuro] drops the cents - for headline figures meant to be read in one glance, never for lists or forms. */
fun formatMoney(amount: Double, roundToEuro: Boolean = false): String =
    (if (roundToEuro) currencyFormatRounded else currencyFormat).format(amount)

/** Renders an amount, optionally coloring it green/red based on its sign. */
@Composable
fun MoneyText(
    amount: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    colorBySign: Boolean = false,
    color: Color = Color.Unspecified,
    roundToEuro: Boolean = false
) {
    val resolvedColor = when {
        color != Color.Unspecified -> color
        !colorBySign -> MaterialTheme.colorScheme.onSurface
        amount > 0 -> positiveGreen
        amount < 0 -> negativeRed
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(text = formatMoney(amount, roundToEuro), modifier = modifier, style = style, color = resolvedColor)
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
    roundToEuro: Boolean = false,
    durationMillis: Int = 500
) {
    val animated by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = tween(durationMillis = durationMillis),
        label = "animatedMoney"
    )
    MoneyText(
        amount = animated.toDouble(),
        modifier = modifier,
        style = style,
        colorBySign = colorBySign,
        color = color,
        roundToEuro = roundToEuro
    )
}
