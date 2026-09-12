package com.codigitech.ft.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codigitech.ft.domain.model.Money
import com.codigitech.ft.ui.theme.Motion
import com.codigitech.ft.ui.theme.expenseColor
import com.codigitech.ft.ui.theme.incomeColor
import com.codigitech.ft.ui.theme.tabular

data class BarSegment(val label: String, val value: Long, val color: Color)

/**
 * A single stacked horizontal bar: each segment's width is its share of the total, with a
 * 2dp gap of surface between segments. Segments animate in from zero width.
 */
@Composable
fun SegmentedBar(segments: List<BarSegment>, modifier: Modifier = Modifier, height: Int = 12) {
    val total = segments.sumOf { it.value }.coerceAtLeast(1)
    val progress by animateFloatAsState(if (segments.isEmpty()) 0f else 1f, tween(Motion.LONG, easing = Motion.emphasized), label = "segBar")
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.fillMaxWidth().height(height.dp)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = radius)
        val gap = 2.dp.toPx()
        var x = 0f
        val usable = size.width - gap * (segments.size - 1).coerceAtLeast(0)
        segments.forEach { s ->
            val w = usable * (s.value.toFloat() / total) * progress
            if (w > 0f) {
                drawRoundRect(s.color, topLeft = Offset(x, 0f), size = Size(w, size.height), cornerRadius = radius)
                x += w + gap
            }
        }
    }
}

/** One thin animated progress bar; turns to the error colour once [fraction] passes 1. */
@Composable
fun BudgetBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Int = 8) {
    val target = fraction.coerceIn(0f, 1f)
    val animated by animateFloatAsState(target, tween(Motion.LONG, easing = Motion.emphasized), label = "budget")
    val over = fraction > 1f
    val fill = if (over) MaterialTheme.colorScheme.error else color
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.fillMaxWidth().height(height.dp)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = radius)
        drawRoundRect(fill, size = Size(size.width * animated, size.height), cornerRadius = radius)
    }
}

data class MonthBar(val label: String, val income: Long, val expense: Long, val highlighted: Boolean = false)

/**
 * Paired income/expense columns per month. One shared axis; bar tops rounded; the highlighted
 * month (the one being viewed) is drawn at full opacity and the rest are softened.
 */
@Composable
fun MonthlyBarChart(months: List<MonthBar>, modifier: Modifier = Modifier, onSelect: ((Int) -> Unit)? = null) {
    val income = incomeColor()
    val expense = expenseColor()
    val max = months.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1) ?: 1
    val progress by animateFloatAsState(1f, tween(Motion.LONG, easing = Motion.emphasized), label = "bars")
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = MaterialTheme.colorScheme.outlineVariant

    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(120.dp), verticalAlignment = Alignment.Bottom) {
            months.forEachIndexed { index, m ->
                val alpha = if (m.highlighted || months.none { it.highlighted }) 1f else 0.45f
                Box(
                    Modifier.weight(1f).fillMaxHeight().then(
                        if (onSelect != null) Modifier.clickable { onSelect(index) } else Modifier,
                    ),
                ) {
                    Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
                        val baseline = size.height - 1f
                        drawLine(grid, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = 1f)
                        val barW = (size.width * 0.28f).coerceAtMost(14.dp.toPx())
                        val gap = 2.dp.toPx()
                        val centre = size.width / 2
                        fun bar(value: Long, x: Float, color: Color) {
                            val h = (baseline * (value.toFloat() / max) * progress)
                            if (h <= 0f) return
                            drawRoundRect(color.copy(alpha = alpha), topLeft = Offset(x, baseline - h), size = Size(barW, h), cornerRadius = CornerRadius(barW / 2))
                        }
                        bar(m.income, centre - barW - gap / 2, income)
                        bar(m.expense, centre + gap / 2, expense)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            months.forEach { m ->
                Text(
                    m.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (m.highlighted) MaterialTheme.colorScheme.onSurface else labelColor,
                    fontWeight = if (m.highlighted) FontWeight.SemiBold else null,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot("Income", income)
            LegendDot("Expense", expense)
        }
    }
}

@Composable
fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Row used under a segmented bar: dot, name, share, amount. */
@Composable
fun BreakdownRow(
    color: Color,
    icon: String?,
    name: String,
    amount: Long,
    share: Float,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(10.dp))
        if (icon != null) {
            Text(icon, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(6.dp))
        }
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            "${(share * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium.tabular,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(Money.format(amount), style = MaterialTheme.typography.bodyMedium.tabular, fontWeight = FontWeight.Medium)
        trailing?.invoke()
    }
}

/** Ring gauge for a single ratio, e.g. spent vs budget. */
@Composable
fun RingGauge(fraction: Float, color: Color, modifier: Modifier = Modifier, stroke: Int = 10, content: @Composable () -> Unit = {}) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(Motion.LONG, easing = Motion.emphasized), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val s = stroke.dp.toPx()
            val inset = s / 2
            val arcSize = Size(size.width - s, size.height - s)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(s, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawArc(color, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(s, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        content()
    }
}
