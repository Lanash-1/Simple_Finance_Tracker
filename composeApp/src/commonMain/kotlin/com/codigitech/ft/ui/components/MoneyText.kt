package com.codigitech.ft.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.codigitech.ft.domain.model.Money
import com.codigitech.ft.ui.theme.Motion
import com.codigitech.ft.ui.theme.expenseColor
import com.codigitech.ft.ui.theme.incomeColor
import com.codigitech.ft.ui.theme.tabular
import kotlin.math.roundToLong

@Composable
fun MoneyText(
    minor: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    colorForSign: Boolean = true,
    withSign: Boolean = false,
    fontWeight: FontWeight? = null,
    color: Color? = null,
) {
    val resolved = color ?: when {
        !colorForSign -> MaterialTheme.colorScheme.onSurface
        minor < 0 -> expenseColor()
        withSign && minor > 0 -> incomeColor()
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(
        Money.format(minor, withSign = withSign),
        modifier = modifier,
        style = style.tabular,
        color = resolved,
        fontWeight = fontWeight,
        maxLines = 1,
    )
}

/**
 * Counts from the previously shown value to [minor]. The animation drives a 0..1 fraction so
 * the final frame is always the exact target, whatever the magnitude.
 */
@Composable
fun AnimatedMoneyText(
    minor: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight? = FontWeight.SemiBold,
    withSign: Boolean = false,
) {
    var shown by remember { mutableStateOf(minor) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(minor) {
        val from = shown
        if (from == minor) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(Motion.LONG + 150, easing = Motion.emphasized)) {
            shown = from + ((minor - from) * value).roundToLong()
        }
        shown = minor
    }
    Text(
        Money.format(shown, withSign = withSign),
        modifier = modifier,
        style = style.tabular,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
    )
}
