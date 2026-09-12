package com.codigitech.ft.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Swipe either way to delete. The action fires once the row settles off-screen; the caller is
 * expected to remove the item from the list (and offer an undo) in response.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDelete(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val latestOnDelete by rememberUpdatedState(onDelete)
    val state = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.45f },
    )
    LaunchedEffect(state.currentValue) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) {
            latestOnDelete()
            state.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            val target = state.targetValue != SwipeToDismissBoxValue.Settled
            val color by animateColorAsState(
                if (target) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
                label = "swipeBg",
            )
            val scale by animateFloatAsState(if (target) 1.15f else 0.9f, label = "swipeIcon")
            val alignment = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            Box(Modifier.fillMaxSize().background(color).padding(horizontal = 24.dp), contentAlignment = alignment) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = if (target) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp).graphicsLayer { scaleX = scale; scaleY = scale },
                )
            }
        },
        content = { content() },
    )
}
