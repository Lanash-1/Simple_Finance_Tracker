package com.codigitech.ft.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codigitech.ft.domain.model.Money
import com.codigitech.ft.ui.theme.tabular
import kotlinx.datetime.LocalDate

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

@Composable
fun VSpace(height: Int = 8) = Spacer(Modifier.height(height.dp))

/** A filled, low-elevation card used for every grouped surface in the app. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, colors = colors, shape = MaterialTheme.shapes.large, content = content)
    } else {
        Card(modifier = modifier, colors = colors, shape = MaterialTheme.shapes.large, content = content)
    }
}

@Composable
fun StatColumn(label: String, minor: Long, color: Color, modifier: Modifier = Modifier, labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column(modifier, horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = labelColor)
        Text(Money.format(minor), style = MaterialTheme.typography.titleMedium.tabular, color = color, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** ‹ September 2026 › with a directional slide when the month changes. */
@Composable
fun MonthSwitcher(
    month: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    canGoNext: Boolean = true,
    onReset: (() -> Unit)? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = tint)
        }
        AnimatedContent(
            targetState = month,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally { if (forward) it / 2 else -it / 2 } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it / 2 else it / 2 } + fadeOut())
            },
            label = "month",
        ) { m ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(m.monthTitle(), style = MaterialTheme.typography.titleMedium, color = tint)
            }
        }
        if (onReset != null && canGoNext) {
            TextButton(onClick = onReset, enabled = true) { Text("Now", color = tint) }
        }
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = if (canGoNext) tint else tint.copy(alpha = 0.35f))
        }
    }
}
