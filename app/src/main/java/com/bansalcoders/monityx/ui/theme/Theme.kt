package com.bansalcoders.monityx.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.bansalcoders.monityx.utils.ThemeMode

// ── Premium dark colour scheme ─────────────────────────────────────────────────
private val PremiumDarkColorScheme = darkColorScheme(
    primary                = NeonGreen,
    onPrimary              = OnNeonGreen,
    primaryContainer       = NeonGreenSurface,
    onPrimaryContainer     = NeonGreen,

    secondary              = SemanticBlue,
    onSecondary            = Color.White,
    secondaryContainer     = Color(0xFF0D1F3C),
    onSecondaryContainer   = SemanticBlue,

    tertiary               = SemanticAmber,
    onTertiary             = Color(0xFF2A1800),
    tertiaryContainer      = Color(0xFF2A1800),
    onTertiaryContainer    = SemanticAmber,

    background             = Background,
    onBackground           = OnBackground,

    surface                = Surface,
    onSurface              = OnSurface,
    surfaceVariant         = SurfaceVariant,
    onSurfaceVariant       = OnSurfaceDim,
    surfaceContainer       = SurfaceContainer,
    surfaceContainerHigh   = SurfaceVariant,
    surfaceContainerHighest= SurfaceContainer,

    error                  = SemanticRed,
    onError                = Color.White,
    errorContainer         = Color(0xFF3A0A0A),
    onErrorContainer       = SemanticRed,

    outline                = Outline,
    outlineVariant         = OutlineVariant,
    scrim                  = Color(0xCC000000),
)

// ── Light scheme (kept for ThemeMode.LIGHT but styled consistently) ────────────
private val PremiumLightColorScheme = lightColorScheme(
    primary                = Color(0xFF00A854),
    onPrimary              = Color.White,
    primaryContainer       = Color(0xFFB7F5D6),
    onPrimaryContainer     = Color(0xFF00210D),

    secondary              = Color(0xFF1565C0),
    onSecondary            = Color.White,
    secondaryContainer     = Color(0xFFD6E4FF),
    onSecondaryContainer   = Color(0xFF001E4A),

    tertiary               = Color(0xFFE65100),
    onTertiary             = Color.White,
    tertiaryContainer      = Color(0xFFFFDCC7),
    onTertiaryContainer    = Color(0xFF2A1200),

    background             = Color(0xFFF5F7FA),
    onBackground           = Color(0xFF0D1117),

    surface                = Color.White,
    onSurface              = Color(0xFF1A2236),
    surfaceVariant         = Color(0xFFEEF2F7),
    onSurfaceVariant       = Color(0xFF4B5768),

    error                  = Color(0xFFB71C1C),
    onError                = Color.White,

    outline                = Color(0xFFCDD5E0),
    outlineVariant         = Color(0xFFE8EDF5),
)

/**
 * Application theme composable.
 *
 * Always forces the premium dark scheme because the visual design is built
 * around it. ThemeMode.LIGHT still works — it switches to the light variant.
 * Dynamic colour (Material You) is intentionally disabled so our carefully
 * chosen neon-green palette is always applied.
 */
@Composable
fun SubscriptionManagerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
        ThemeMode.SYSTEM -> systemDark
    }

    // Always use our custom palette — skip Material You dynamic colours so the
    // premium neon-green branding is preserved on every device.
    val colorScheme = if (isDark) PremiumDarkColorScheme else PremiumLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            // Keep status-bar icons light on dark theme, dark on light theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content,
    )
}
