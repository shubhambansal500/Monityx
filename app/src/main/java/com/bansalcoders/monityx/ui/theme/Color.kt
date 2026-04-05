package com.bansalcoders.monityx.ui.theme

import androidx.compose.ui.graphics.Color

// ── Premium dark theme palette ─────────────────────────────────────────────────

// Backgrounds — deep navy-black layered surfaces
val Background        = Color(0xFF0A0E1A)   // deepest background
val Surface           = Color(0xFF111827)   // card / surface
val SurfaceVariant    = Color(0xFF1A2236)   // elevated card
val SurfaceContainer  = Color(0xFF1F2D42)   // chips, input fields

// Neon green accent (primary brand colour)
val NeonGreen         = Color(0xFF00E676)   // primary CTA, highlights
val NeonGreenDim      = Color(0xFF00C853)   // pressed / secondary action
val NeonGreenSurface  = Color(0xFF003D1A)   // container behind green content
val OnNeonGreen       = Color(0xFF00210D)   // text on green buttons

// On-surface text
val OnBackground      = Color(0xFFE8F0FE)   // primary text
val OnSurface         = Color(0xFFCDD5E0)   // secondary text
val OnSurfaceDim      = Color(0xFF6B7B8D)   // hints, placeholders

// Semantic accents
val SemanticRed       = Color(0xFFFF5252)
val SemanticAmber     = Color(0xFFFFAB40)
val SemanticBlue      = Color(0xFF448AFF)

// Outline / divider
val Outline           = Color(0xFF243044)
val OutlineVariant    = Color(0xFF1A2640)

// Legacy purple/pink kept only for compatibility – not used in UI
val Purple10  = Color(0xFF0A0E1A)
val Purple20  = Color(0xFF111827)
val Purple30  = Color(0xFF1A2236)
val Purple40  = Color(0xFF00E676)
val Purple80  = Color(0xFF00E676)
val Purple90  = Color(0xFF003D1A)

val PurpleGrey10 = Color(0xFF111827)
val PurpleGrey20 = Color(0xFF1A2236)
val PurpleGrey30 = Color(0xFF1F2D42)
val PurpleGrey40 = Color(0xFF6B7B8D)
val PurpleGrey80 = Color(0xFFCDD5E0)
val PurpleGrey90 = Color(0xFFE8F0FE)

val Pink10  = Color(0xFF3E0020)
val Pink20  = Color(0xFF640036)
val Pink30  = Color(0xFF8D0050)
val Pink40  = Color(0xFFFF5252)
val Pink80  = Color(0xFFFFB1C2)
val Pink90  = Color(0xFFFFD9E2)

// ── Chart colours (vibrant against dark background) ───────────────────────────
val ChartColors = listOf(
    Color(0xFF00E676),   // neon green
    Color(0xFF448AFF),   // blue
    Color(0xFFFF5252),   // red
    Color(0xFFFFAB40),   // amber
    Color(0xFF40C4FF),   // light blue
    Color(0xFFEA80FC),   // purple
    Color(0xFF69F0AE),   // mint green
    Color(0xFFFFFF00),   // yellow
    Color(0xFF80D8FF),   // sky blue
    Color(0xFFFF80AB),   // pink
    Color(0xFFA7FFEB),   // teal
    Color(0xFFFFD180),   // peach
    Color(0xFFCCFF90),   // lime
    Color(0xFFFF9E80),   // deep orange
)
