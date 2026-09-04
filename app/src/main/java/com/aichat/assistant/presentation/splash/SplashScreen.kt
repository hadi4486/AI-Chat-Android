package com.aichat.assistant.presentation.splash

import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aichat.assistant.R
import kotlinx.coroutines.delay

private const val SPLASH_HOLD_MS = 1100L

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var animateIn by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.7f,
        animationSpec = tween(650, easing = EaseOutBack),
        label = "splashScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(500),
        label = "splashAlpha"
    )

    LaunchedEffect(Unit) {
        animateIn = true
        delay(SPLASH_HOLD_MS)
        onFinished()
    }

    val background = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(colors = listOf(primary.copy(alpha = 0.12f), background))
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_splash_logo),
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .scale(scale)
                .alpha(alpha)
        )
    }
}
