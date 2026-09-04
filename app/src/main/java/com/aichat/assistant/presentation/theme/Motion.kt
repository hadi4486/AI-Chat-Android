package com.aichat.assistant.presentation.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * One vocabulary of timings for the whole app. Section 5 of the spec asks for a lot of individual
 * animations — the thing that keeps that many moments from feeling scattered is reusing the same
 * few durations/curves everywhere instead of tuning each one separately.
 */
object Motion {
    const val QUICK_MS = 120
    const val STANDARD_MS = 240
    const val EMPHASIZED_MS = 420

    val standardEasing = FastOutSlowInEasing

    fun <T> standardTween() = tween<T>(durationMillis = STANDARD_MS, easing = standardEasing)
    fun <T> quickTween() = tween<T>(durationMillis = QUICK_MS, easing = standardEasing)
    fun <T> emphasizedTween() = tween<T>(durationMillis = EMPHASIZED_MS, easing = standardEasing)

    fun <T> gentleSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> snappySpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
