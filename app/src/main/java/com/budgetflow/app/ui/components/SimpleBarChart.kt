package com.budgetflow.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A simple grouped bar chart drawn with Canvas - no external charting dependency. */
@Composable
fun GroupedBarChart(
    labels: List<String>,
    seriesA: List<Float>,
    seriesB: List<Float>,
    colorA: Color,
    colorB: Color,
    modifier: Modifier = Modifier
) {
    val maxValue = (seriesA + seriesB).maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Canvas(modifier = modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp)) {
        val groupCount = labels.size.coerceAtLeast(1)
        val groupWidth = size.width / groupCount
        val barWidth = groupWidth / 3f
        val chartHeight = size.height

        labels.indices.forEach { index ->
            val a = seriesA.getOrElse(index) { 0f }
            val b = seriesB.getOrElse(index) { 0f }
            val groupStart = index * groupWidth + groupWidth / 6f

            val heightA = (a / maxValue) * chartHeight
            drawRect(
                color = colorA,
                topLeft = androidx.compose.ui.geometry.Offset(groupStart, chartHeight - heightA),
                size = Size(barWidth, heightA)
            )

            val heightB = (b / maxValue) * chartHeight
            drawRect(
                color = colorB,
                topLeft = androidx.compose.ui.geometry.Offset(groupStart + barWidth + 4f, chartHeight - heightB),
                size = Size(barWidth, heightB)
            )
        }
    }
}

/** A horizontal bar per entry, proportional to [fraction] (0f..1f) - used for the category breakdown. */
@Composable
fun HorizontalBarRow(label: String, amountLabel: String, fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(amountLabel, style = MaterialTheme.typography.bodyMedium)
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp)) {
            drawRoundRect(
                color = color.copy(alpha = 0.15f),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = color,
                size = Size(size.width * fraction.coerceIn(0f, 1f), size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }
    }
}
