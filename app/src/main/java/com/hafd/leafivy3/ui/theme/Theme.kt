package com.hafd.leafivy3.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

// ── Build a colour scheme from a seed ─────────────────────────────────────

fun buildLightScheme(seed: AppColorSeed): ColorScheme {
    val p = Color(seed.primary)
    return lightColorScheme(
        primary               = p,
        onPrimary             = Color.White,
        primaryContainer      = p.toneUp(0.88f),
        onPrimaryContainer    = p.toneDown(0.82f),
        secondary             = p.blend(Color.Gray, 0.55f),
        onSecondary           = Color.White,
        secondaryContainer    = p.toneUp(0.90f).blend(Color(0xFFE8E8E8), 0.3f),
        onSecondaryContainer  = p.toneDown(0.78f),
        tertiary              = p.rotateHue(60f).blend(Color.Gray, 0.5f),
        onTertiary            = Color.White,
        tertiaryContainer     = p.rotateHue(60f).toneUp(0.88f),
        onTertiaryContainer   = p.rotateHue(60f).toneDown(0.82f),
        background            = Color(0xFFF8F9FF),
        onBackground          = Color(0xFF1A1C20),
        surface               = Color(0xFFF8F9FF),
        onSurface             = Color(0xFF1A1C20),
        surfaceVariant        = p.toneUp(0.92f).blend(Color(0xFFDDE3EA), 0.5f),
        onSurfaceVariant      = Color(0xFF414752),
        surfaceContainerLowest  = Color.White,
        surfaceContainerLow     = p.toneUp(0.95f).blend(Color(0xFFF0F4FF), 0.6f),
        surfaceContainer        = p.toneUp(0.93f).blend(Color(0xFFE8EEF8), 0.5f),
        surfaceContainerHigh    = p.toneUp(0.90f).blend(Color(0xFFDDE5F2), 0.5f),
        surfaceContainerHighest = p.toneUp(0.87f).blend(Color(0xFFD7DFEC), 0.5f),
        outline               = Color(0xFF717882),
        outlineVariant        = p.toneUp(0.85f).blend(Color(0xFFBCC5DB), 0.6f),
        error                 = Color(0xFFBA1A1A),
        onError               = Color.White,
        errorContainer        = Color(0xFFFFDAD6),
        onErrorContainer      = Color(0xFF410002),
    )
}

fun buildDarkScheme(seed: AppColorSeed): ColorScheme {
    val p = Color(seed.primary)
    return darkColorScheme(
        primary               = p.toneUp(0.78f),
        onPrimary             = p.toneDown(0.72f),
        primaryContainer      = p.toneDown(0.35f),
        onPrimaryContainer    = p.toneUp(0.88f),
        secondary             = p.blend(Color.LightGray, 0.6f),
        onSecondary           = p.toneDown(0.65f),
        secondaryContainer    = p.toneDown(0.55f).blend(Color(0xFF2A2D35), 0.6f),
        onSecondaryContainer  = p.toneUp(0.88f),
        tertiary              = p.rotateHue(60f).toneUp(0.75f),
        onTertiary            = p.rotateHue(60f).toneDown(0.65f),
        tertiaryContainer     = p.rotateHue(60f).toneDown(0.45f),
        onTertiaryContainer   = p.rotateHue(60f).toneUp(0.88f),
        background            = Color(0xFF101418),
        onBackground          = Color(0xFFE2E2E9),
        surface               = Color(0xFF101418),
        onSurface             = Color(0xFFE2E2E9),
        surfaceVariant        = Color(0xFF3E4759),
        onSurfaceVariant      = Color(0xFFBCC5DB),
        surfaceContainerLowest  = Color(0xFF0B0E12),
        surfaceContainerLow     = Color(0xFF181C22),
        surfaceContainer        = Color(0xFF1C2028),
        surfaceContainerHigh    = Color(0xFF252A32),
        surfaceContainerHighest = Color(0xFF2F343D),
        outline               = Color(0xFF8B9198),
        outlineVariant        = Color(0xFF3E4759),
        error                 = Color(0xFFFFB4AB),
        onError               = Color(0xFF690005),
        errorContainer        = Color(0xFF93000A),
        onErrorContainer      = Color(0xFFFFDAD6),
    )
}

// ── Color math helpers ─────────────────────────────────────────────────────

private fun Color.toneUp(amount: Float): Color {
    val r = red + (1f - red) * amount
    val g = green + (1f - green) * amount
    val b = blue + (1f - blue) * amount
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), alpha)
}

private fun Color.toneDown(amount: Float): Color {
    val r = red * (1f - amount)
    val g = green * (1f - amount)
    val b = blue * (1f - amount)
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), alpha)
}

private fun Color.blend(other: Color, ratio: Float): Color = Color(
    red   = red   * (1f - ratio) + other.red   * ratio,
    green = green * (1f - ratio) + other.green * ratio,
    blue  = blue  * (1f - ratio) + other.blue  * ratio,
    alpha = alpha
)

private fun Color.rotateHue(degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees) % 360f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

// ── Theme entry point ──────────────────────────────────────────────────────

@Composable
fun leafivyTheme(
    themePrefs: ThemePrefs? = null,
    content: @Composable () -> Unit
) {
    val prefs = themePrefs ?: run {
        val s by ThemePreferences.flow.collectAsState()
        s
    }

    val context = LocalContext.current

    val darkTheme = when (prefs.darkMode) {
        AppDarkMode.SYSTEM -> isSystemInDarkTheme()
        AppDarkMode.LIGHT  -> false
        AppDarkMode.DARK   -> true
    }

    val colorScheme = when {
        prefs.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> buildDarkScheme(prefs.colorSeed)
        else      -> buildLightScheme(prefs.colorSeed)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = AppShapes,
        content     = content
    )
}
