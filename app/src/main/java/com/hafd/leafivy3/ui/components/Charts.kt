package com.hafd.leafivy3.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.hafd.leafivy3.ml.Prediction
import com.hafd.leafivy3.ui.normalizeLabel

@Composable
fun ConfidenceBarChart(
    predictions: List<Prediction>,
    modifier: Modifier = Modifier
) {
    if (predictions.isEmpty()) return

    val colors = MaterialTheme.colorScheme
    val bars = predictions.take(4)
    val max = (bars.maxOfOrNull { it.confidence } ?: 1f).coerceAtLeast(0.01f)

    Column(
        modifier = modifier.semantics {
            contentDescription = "Confidence chart for predictions"
        }
    ) {
        bars.forEach { item ->
            val label = normalizeLabel(item.label)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
            ) {
                val trackHeight = size.height
                drawRoundRect(
                    color = colors.surfaceVariant,
                    topLeft = Offset.Zero,
                    size = Size(size.width, trackHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )

                val barWidth = size.width * (item.confidence / max).coerceIn(0f, 1f)
                if (barWidth > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                colors.primary,
                                colors.secondary.copy(alpha = 0.9f)
                            )
                        ),
                        topLeft = Offset.Zero,
                        size = Size(barWidth, trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            text = bars.joinToString("  ") { "${(it.confidence * 100).toInt()}%" },
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant
        )
    }
}
