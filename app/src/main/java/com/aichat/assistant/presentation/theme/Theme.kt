package com.aichat.assistant.presentation.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.aichat.assistant.domain.model.ThemeMode

/**
 * Wraps every role of [target] in [animateColorAsState] so switching theme mode (or light/dark
 * following the system) fades smoothly instead of snapping — master spec §5 "Theme Transition".
 * [animateColorAsState] already remembers the previous target across recompositions, so this only
 * needs the new scheme, not the old one.
 */
@Composable
private fun animatedColorScheme(target: ColorScheme): ColorScheme {
    val spec = Motion.standardTween<androidx.compose.ui.graphics.Color>()
    return target.copy(
        primary = animateColorAsState(target.primary, spec, label = "primary").value,
        onPrimary = animateColorAsState(target.onPrimary, spec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(target.primaryContainer, spec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, spec, label = "onPrimaryContainer").value,
        secondary = animateColorAsState(target.secondary, spec, label = "secondary").value,
        onSecondary = animateColorAsState(target.onSecondary, spec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, spec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, spec, label = "onSecondaryContainer").value,
        background = animateColorAsState(target.background, spec, label = "background").value,
        onBackground = animateColorAsState(target.onBackground, spec, label = "onBackground").value,
        surface = animateColorAsState(target.surface, spec, label = "surface").value,
        onSurface = animateColorAsState(target.onSurface, spec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, spec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, spec, label = "onSurfaceVariant").value,
        outline = animateColorAsState(target.outline, spec, label = "outline").value,
        error = animateColorAsState(target.error, spec, label = "error").value,
        onError = animateColorAsState(target.onError, spec, label = "onError").value,
        errorContainer = animateColorAsState(target.errorContainer, spec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(target.onErrorContainer, spec, label = "onErrorContainer").value
    )
}

@Composable
fun AiChatTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColorEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    val context = LocalContext.current
    val targetScheme = when {
        dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDark -> aiChatDarkColorScheme()
        else -> aiChatLightColorScheme()
    }

    val animatedScheme = animatedColorScheme(targetScheme)

    MaterialTheme(
        colorScheme = animatedScheme,
        typography = AiChatTypography,
        content = content
    )
}

/** Convenience accessor for the semantic "connected" dot color, light/dark aware. */
val ColorScheme.connectedIndicator: androidx.compose.ui.graphics.Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) StatusColors.ConnectedDark else StatusColors.Connected
