package com.example.ui.designsystem

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

object MutantMotion {
    const val Feedback = 120
    const val State = 180
    const val Navigation = 240
    const val Container = 320
    const val Focal = 420

    val FeedbackSpec = tween<Float>(durationMillis = Feedback, easing = FastOutSlowInEasing)
    val StateSpec = tween<Float>(durationMillis = State, easing = FastOutSlowInEasing)
    val NavigationSpec = tween<Float>(durationMillis = Navigation, easing = FastOutSlowInEasing)
    val ContainerSpec = tween<Float>(durationMillis = Container, easing = FastOutSlowInEasing)
    val FocalSpec = tween<Float>(durationMillis = Focal, easing = FastOutSlowInEasing)

    val SetEnterTransition = fadeIn(animationSpec = tween(140, easing = LinearOutSlowInEasing)) +
            expandVertically(animationSpec = tween(State, easing = FastOutSlowInEasing))

    val SetExitTransition = fadeOut(animationSpec = tween(100, easing = FastOutLinearInEasing)) +
            shrinkVertically(animationSpec = tween(140, easing = FastOutLinearInEasing))

    val CollapsibleEnterTransition = expandVertically(animationSpec = tween(State, easing = FastOutSlowInEasing)) +
            fadeIn(animationSpec = tween(140, easing = LinearOutSlowInEasing))

    val CollapsibleExitTransition = shrinkVertically(animationSpec = tween(140, easing = FastOutLinearInEasing)) +
            fadeOut(animationSpec = tween(100, easing = FastOutLinearInEasing))

    fun timerEnterTransition(density: Density) = with(density) {
        slideInVertically(
            initialOffsetY = { 16.dp.roundToPx() },
            animationSpec = tween(State, easing = LinearOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(State, easing = LinearOutSlowInEasing))
    }

    fun timerExitTransition(density: Density) = with(density) {
        slideOutVertically(
            targetOffsetY = { 16.dp.roundToPx() },
            animationSpec = tween(120, easing = FastOutLinearInEasing)
        ) + fadeOut(animationSpec = tween(120, easing = FastOutLinearInEasing))
    }

    fun exerciseTransitionSpec(density: Density, isForward: Boolean): ContentTransform = with(density) {
        val distance = 24.dp.roundToPx()
        val enterSlide = slideInHorizontally(
            initialOffsetX = { if (isForward) distance else -distance },
            animationSpec = tween(Navigation, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(Navigation, easing = LinearOutSlowInEasing))

        val exitSlide = slideOutHorizontally(
            targetOffsetX = { if (isForward) -distance else distance },
            animationSpec = tween(Navigation, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(120, easing = FastOutLinearInEasing))

        enterSlide togetherWith exitSlide
    }

    val ScreenFadeThroughSpec = fadeIn(animationSpec = tween(Navigation, easing = LinearOutSlowInEasing)) togetherWith
            fadeOut(animationSpec = tween(120, easing = FastOutLinearInEasing))
}
