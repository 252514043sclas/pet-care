package com.kavya.petcare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kavya.petcare.data.session.SessionManager
import com.kavya.petcare.ui.*

private val ModernLightColorScheme = lightColorScheme(
    primary = LightMintSage,
    onPrimary = Color.White,
    primaryContainer = LightMintSoft,
    onPrimaryContainer = LightEmeraldDark,
    secondary = LightEmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6EAE3),
    onSecondaryContainer = LightEmeraldDark,
    tertiary = LightAmberAccent,
    onTertiary = Color.White,
    background = LightSoftBackground,
    onBackground = LightTextPrimary,
    surface = LightWarmPearl,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFF0F4F2),
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorderSubtle,
    error = LightAlertRose,
    onError = Color.White,
    errorContainer = LightAlertRoseSoft,
    onErrorContainer = LightAlertRose
)

private val ModernDarkColorScheme = darkColorScheme(
    primary = DarkMintLight,
    onPrimary = Color(0xFF032B21),
    primaryContainer = DarkMintSoft,
    onPrimaryContainer = DarkMintLight,
    secondary = DarkEmeraldDark,
    onSecondary = Color(0xFF02261C),
    secondaryContainer = Color(0xFF1E3830),
    onSecondaryContainer = DarkEmeraldDark,
    tertiary = DarkAmberAccent,
    onTertiary = Color(0xFF382300),
    background = DarkSoftBackground,
    onBackground = DarkTextPrimary,
    surface = DarkWarmPearl,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF1E2F28),
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderSubtle,
    error = DarkAlertRose,
    onError = Color(0xFF490005),
    errorContainer = DarkAlertRoseSoft,
    onErrorContainer = DarkAlertRose
)

val ModernShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun PetCareTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val session = remember(context) { SessionManager.getInstance(context) }
    val themeMode by session.themeModeFlow.collectAsState()
    val fontSize by session.fontSizeFlow.collectAsState()
    val systemInDark = isSystemInDarkTheme()

    val isDark = when {
        darkTheme != null -> darkTheme
        themeMode.equals("dark", ignoreCase = true) -> true
        themeMode.equals("light", ignoreCase = true) -> false
        else -> systemInDark
    }

    val fontScaleMultiplier = when (fontSize.lowercase()) {
        "small" -> 0.85f
        "large" -> 1.15f
        else -> 1.0f
    }

    val currentDensity = LocalDensity.current
    val customDensity = remember(currentDensity.density, currentDensity.fontScale, fontScaleMultiplier) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * fontScaleMultiplier
        )
    }

    val colorScheme = if (isDark) ModernDarkColorScheme else ModernLightColorScheme
    val typography = remember(isDark) { getPetTypography(isDark) }

    CompositionLocalProvider(
        LocalPetDarkTheme provides isDark,
        LocalDensity provides customDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = ModernShapes,
            content = content
        )
    }
}
