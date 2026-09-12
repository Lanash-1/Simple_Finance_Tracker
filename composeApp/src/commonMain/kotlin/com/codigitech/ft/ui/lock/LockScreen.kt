package com.codigitech.ft.ui.lock

import com.codigitech.ft.Brand
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codigitech.ft.ui.components.VSpace
import com.codigitech.ft.ui.theme.AppIcons
import com.codigitech.ft.ui.theme.Motion
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun LockScreen(viewModel: LockViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The lock engages while the app is in the background, so the prompt has to wait until the
    // screen is actually in front of the user; a biometric request from a stopped activity fails.
    // Once per lock, so a cancelled prompt does not reappear on every resume.
    var prompted by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!prompted && state.biometricOffered) {
            prompted = true
            viewModel.biometric()
        }
    }

    // Horizontal shake of the dot row on a wrong PIN.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state.errorCount) {
        if (state.errorCount == 0) return@LaunchedEffect
        shake.snapTo(0f)
        shake.animateTo(0f, keyframes {
            durationMillis = 420
            -14f at 60; 12f at 130; -8f at 200; 6f at 270; -3f at 340; 0f at 420
        })
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
            }
            VSpace(20)
            Text("Enter your PIN", style = MaterialTheme.typography.titleLarge)
            Text("${Brand.APP_NAME} is locked", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            VSpace(28)
            Row(
                Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) },
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                repeat(6) { i -> PinDot(filled = i < state.pin.length, error = state.error != null) }
            }
            VSpace(12)
            Text(
                state.error ?: " ",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            VSpace(24)
            listOf("123", "456", "789").forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { d -> Key(label = d.toString()) { viewModel.press(d) } }
                }
                VSpace(14)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                if (state.biometricOffered) Key(icon = AppIcons.Face) { viewModel.biometric() } else Key(label = "", enabled = false) {}
                Key(label = "0") { viewModel.press('0') }
                Key(icon = AppIcons.Backspace) { viewModel.backspace() }
            }
            if (state.biometricOffered) {
                VSpace(16)
                TextButton(onClick = viewModel::biometric) { Text("Use biometrics") }
            }
        }
    }
}

@Composable
private fun PinDot(filled: Boolean, error: Boolean) {
    val scale by animateFloatAsState(if (filled) 1.25f else 1f, Motion.gentleSpring(), label = "dot")
    val color by animateColorAsState(
        when {
            error -> MaterialTheme.colorScheme.error
            filled -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        tween(Motion.SHORT),
        label = "dotColor",
    )
    Box(Modifier.size(14.dp).graphicsLayer { scaleX = scale; scaleY = scale }.background(color, CircleShape))
}

@Composable
private fun Key(label: String? = null, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val container = if (enabled && (label?.isNotEmpty() == true || icon != null)) MaterialTheme.colorScheme.surfaceContainerHigh else androidx.compose.ui.graphics.Color.Transparent
    Box(
        Modifier.size(72.dp).background(container, CircleShape).then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when {
            icon != null -> Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            label != null -> Text(label, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
