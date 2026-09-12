package com.codigitech.ft.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

/** One place for durations and curves so every screen moves the same way. */
object Motion {
    const val SHORT = 180
    const val MEDIUM = 300
    const val LONG = 450

    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val standard = FastOutSlowInEasing

    fun <T> gentleSpring() = spring<T>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
    fun <T> snappySpring() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    // Navigation -----------------------------------------------------------------------------

    /** Top-level tab to tab: a quiet fade-through, no direction. */
    val tabEnter: EnterTransition = fadeIn(tween(MEDIUM, easing = standard)) + scaleIn(tween(MEDIUM, easing = standard), initialScale = 0.98f)
    val tabExit: ExitTransition = fadeOut(tween(SHORT))

    /** Pushing a detail screen: slide in from the right while the parent recedes. */
    fun <S> AnimatedContentTransitionScope<S>.pushEnter(): EnterTransition =
        slideInHorizontally(tween(LONG, easing = emphasized)) { it / 3 } + fadeIn(tween(MEDIUM))

    fun <S> AnimatedContentTransitionScope<S>.pushExit(): ExitTransition =
        slideOutHorizontally(tween(LONG, easing = emphasized)) { -it / 5 } + fadeOut(tween(MEDIUM))

    fun <S> AnimatedContentTransitionScope<S>.popEnter(): EnterTransition =
        slideInHorizontally(tween(LONG, easing = emphasized)) { -it / 5 } + fadeIn(tween(MEDIUM))

    fun <S> AnimatedContentTransitionScope<S>.popExit(): ExitTransition =
        slideOutHorizontally(tween(LONG, easing = emphasized)) { it / 3 } + fadeOut(tween(MEDIUM))

    /** The editor rises like a sheet. */
    val sheetEnter: EnterTransition = slideInVertically(tween(LONG, easing = emphasized)) { it / 2 } + fadeIn(tween(MEDIUM))
    val sheetExit: ExitTransition = slideOutVertically(tween(MEDIUM, easing = emphasized)) { it / 2 } + fadeOut(tween(SHORT))

    // In-screen -----------------------------------------------------------------------------

    val barEnter: EnterTransition = slideInVertically(tween(MEDIUM, easing = emphasized)) { it } + fadeIn(tween(MEDIUM))
    val barExit: ExitTransition = slideOutVertically(tween(MEDIUM, easing = emphasized)) { it } + fadeOut(tween(SHORT))
}
