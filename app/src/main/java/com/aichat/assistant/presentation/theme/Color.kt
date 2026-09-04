package com.aichat.assistant.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// --- Light scheme -----------------------------------------------------------------------------
private val LightPrimary = Color(0xFFC98A2E)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFFBE3BA)
private val LightOnPrimaryContainer = Color(0xFF4A3103)
private val LightSecondary = Color(0xFF2F8F86)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFCFEEEA)
private val LightOnSecondaryContainer = Color(0xFF0B332F)
private val LightBackground = Color(0xFFFBF9F5)
private val LightOnBackground = Color(0xFF1E1B17)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF1E1B17)
private val LightSurfaceVariant = Color(0xFFEFEAE1)
private val LightOnSurfaceVariant = Color(0xFF4D473E)
private val LightOutline = Color(0xFF7A7367)
private val LightError = Color(0xFFC4432D)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFF8D9D0)
private val LightOnErrorContainer = Color(0xFF4A150A)

// --- Dark scheme --------------------------------------------------------------------------------
private val DarkPrimary = Color(0xFFE8B04B)
private val DarkOnPrimary = Color(0xFF3A2600)
private val DarkPrimaryContainer = Color(0xFF5B3D08)
private val DarkOnPrimaryContainer = Color(0xFFFBE3BA)
private val DarkSecondary = Color(0xFF4FB0A5)
private val DarkOnSecondary = Color(0xFF00332D)
private val DarkSecondaryContainer = Color(0xFF16443E)
private val DarkOnSecondaryContainer = Color(0xFFC9EFE9)
private val DarkBackground = Color(0xFF14120F)
private val DarkOnBackground = Color(0xFFF5F1EA)
private val DarkSurface = Color(0xFF1E1B17)
private val DarkOnSurface = Color(0xFFF5F1EA)
private val DarkSurfaceVariant = Color(0xFF2A2620)
private val DarkOnSurfaceVariant = Color(0xFFC9C2B6)
private val DarkOutline = Color(0xFF948C7E)
private val DarkError = Color(0xFFE5806A)
private val DarkOnError = Color(0xFF4A150A)
private val DarkErrorContainer = Color(0xFF6B2013)
private val DarkOnErrorContainer = Color(0xFFF8D9D0)

fun aiChatLightColorScheme() = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

fun aiChatDarkColorScheme() = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

/** Semantic status colors that sit outside the Material role system (connection badge, etc). */
object StatusColors {
    val Connected = Color(0xFF4CAF7D)
    val ConnectedDark = Color(0xFF6FCB9E)
}
