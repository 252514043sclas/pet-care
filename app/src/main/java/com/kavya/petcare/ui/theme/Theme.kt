package com.kavya.petcare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kavya.petcare.ui.*

private val ModernLightColorScheme = lightColorScheme(
    primary = MintSage,
    onPrimary = Color.White,
    primaryContainer = MintSoft,
    onPrimaryContainer = EmeraldDark,
    secondary = EmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6EAE3),
    onSecondaryContainer = EmeraldDark,
    tertiary = AmberAccent,
    onTertiary = Color.White,
    background = SoftBackground,
    onBackground = TextPrimary,
    surface = WarmPearl,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF0F4F2),
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = AlertRose,
    onError = Color.White,
    errorContainer = AlertRoseSoft,
    onErrorContainer = AlertRose
)

private val ModernDarkColorScheme = darkColorScheme(
    primary = MintLight,
    onPrimary = Color(0xFF032B21),
    primaryContainer = Color(0xFF0F4C3D),
    onPrimaryContainer = MintSoft,
    secondary = Color(0xFF81C7B8),
    onSecondary = Color(0xFF02261C),
    background = Color(0xFF0F1714),
    onBackground = Color(0xFFE8ECE9),
    surface = Color(0xFF16221D),
    onSurface = Color(0xFFE8ECE9),
    surfaceVariant = Color(0xFF1E2F28),
    onSurfaceVariant = Color(0xFF9EABA4),
    outline = Color(0xFF2C4139),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF490005)
)

val ModernShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun PetCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ModernDarkColorScheme else ModernLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PetTypography,
        shapes = ModernShapes,
        content = content
    )
}
