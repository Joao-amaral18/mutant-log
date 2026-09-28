package com.example.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

object MotionDuration {
    const val Instant = 100
    const val Feedback = 120
    const val State = 180
    const val Navigation = 240
    const val Container = 320
    const val Focal = 420
}

object MotionDistance {
    val Small = 8.dp
    val Medium = 16.dp
    val Navigation = 32.dp
}

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

    // Set logged entrance: fade + vertical expand
    val SetEnterTransition = fadeIn(animationSpec = tween(160, easing = LinearOutSlowInEasing)) +
            expandVertically(animationSpec = tween(MotionDuration.State, easing = FastOutSlowInEasing))

    val SetExitTransition = fadeOut(animationSpec = tween(100, easing = FastOutLinearInEasing)) +
            shrinkVertically(animationSpec = tween(160, easing = FastOutLinearInEasing))

    // Technique & collapsible fields transition
    val CollapsibleEnterTransition = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
            fadeIn(animationSpec = tween(160, easing = LinearOutSlowInEasing))

    val CollapsibleExitTransition = shrinkVertically(animationSpec = tween(160, easing = FastOutLinearInEasing)) +
            fadeOut(animationSpec = tween(100, easing = FastOutLinearInEasing))

    // Rest timer entrance from bottom (+16dp rise)
    fun timerEnterTransition(density: Density) = with(density) {
        slideInVertically(
            initialOffsetY = { MotionDistance.Medium.roundToPx() },
            animationSpec = tween(MotionDuration.State, easing = LinearOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(MotionDuration.State, easing = LinearOutSlowInEasing))
    }

    fun timerExitTransition(density: Density) = with(density) {
        slideOutVertically(
            targetOffsetY = { MotionDistance.Medium.roundToPx() },
            animationSpec = tween(140, easing = FastOutLinearInEasing)
        ) + fadeOut(animationSpec = tween(140, easing = FastOutLinearInEasing))
    }

    // Shared-axis horizontal exercise navigation transition (32dp small travel)
    fun exerciseTransitionSpec(density: Density, isForward: Boolean): ContentTransform = with(density) {
        val distance = MotionDistance.Navigation.roundToPx()
        val enterSlide = slideInHorizontally(
            initialOffsetX = { if (isForward) distance else -distance },
            animationSpec = tween(MotionDuration.Navigation, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(MotionDuration.Navigation, easing = LinearOutSlowInEasing))

        val exitSlide = slideOutHorizontally(
            targetOffsetX = { if (isForward) -distance else distance },
            animationSpec = tween(MotionDuration.Navigation, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(140, easing = FastOutLinearInEasing))

        enterSlide togetherWith exitSlide
    }

    // Fade-through transition for independent top-level screen destinations
    val ScreenFadeThroughSpec = fadeIn(animationSpec = tween(MotionDuration.Navigation, easing = LinearOutSlowInEasing)) togetherWith
            fadeOut(animationSpec = tween(140, easing = FastOutLinearInEasing))
}
