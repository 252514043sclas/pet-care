package com.kavya.petcare.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// Modern, Luxurious Pet Care Color Palette
// ============================================================================
val EmeraldDark = Color(0xFF0B3327)
val EmeraldPrimary = Color(0xFF135343)
val MintSage = Color(0xFF289A8C)
val MintLight = Color(0xFF38B2AC)
val MintSoft = Color(0xFFE8F5F2)
val SoftBackground = Color(0xFFF7FAF8)
val WarmPearl = Color(0xFFFFFFFF)
val BorderSubtle = Color(0xFFE3EBE6)
val AmberAccent = Color(0xFFF59E0B)
val AmberSoft = Color(0xFFFEF3C7)
val AlertRose = Color(0xFFE63946)
val AlertRoseSoft = Color(0xFFFFEBEE)
val SkyBlue = Color(0xFF2563EB)
val SkySoft = Color(0xFFEFF6FF)
val LavenderAccent = Color(0xFF7C3AED)
val LavenderSoft = Color(0xFFF5F3FF)

val TextPrimary = Color(0xFF11201A)
val TextSecondary = Color(0xFF4C5E57)
val TextMuted = Color(0xFF8A9A92)

val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF2FA294), Color(0xFF0F4738))
)

val HeroCardGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF38B2AC), Color(0xFF155E4E), Color(0xFF093125))
)

val WarmAmberGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFFBBF24), Color(0xFFD97706))
)

val SoftSurfaceGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF4F8F6))
)

// ============================================================================
// Modern Universal Top Bar Component
// ============================================================================
@Composable
fun PetModernTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    containerColor: Color = Color.Transparent,
    contentColor: Color = EmeraldDark
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable { onBack() },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = actions
                )
            }
        }
    }
}

// ============================================================================
// Modern Elevated Card with Spring Animation & Micro-interaction
// ============================================================================
@Composable
fun PetModernCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(22.dp),
    containerColor: Color = WarmPearl,
    elevation: Dp = 3.dp,
    borderColor: Color = BorderSubtle,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0x1A0D3B2E),
                spotColor = Color(0x1F0D3B2E)
            )
            .border(1.dp, borderColor, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(color = MintSage.copy(alpha = 0.2f)),
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        content = content
    )
}

// ============================================================================
// Modern Gradient Action Button with Loading State
// ============================================================================
@Composable
fun PetGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    gradient: Brush = PrimaryGradient
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "buttonScale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = if (enabled && !isLoading) 6.dp else 1.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = MintSage.copy(alpha = 0.35f),
                spotColor = EmeraldDark.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = rememberRipple(color = Color.White.copy(alpha = 0.3f)),
                onClick = onClick
            ),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) gradient else Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

// ============================================================================
// Modern Outlined Pill Button
// ============================================================================
@Composable
fun PetOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    borderColor: Color = MintSage,
    contentColor: Color = EmeraldDark
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(27.dp),
        border = BorderStroke(1.5.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            containerColor = Color.White
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================================================
// Status Pill Badge Component
// ============================================================================
@Composable
fun PetBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MintSoft,
    contentColor: Color = EmeraldDark,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun PetBadge(
    text: String,
    color: Color,
    textColor: Color = EmeraldDark,
    icon: ImageVector? = null
) {
    PetBadge(
        text = text,
        containerColor = color.copy(alpha = 0.16f),
        contentColor = textColor,
        icon = icon
    )
}

// ============================================================================
// Compact Stat Pill Metric (e.g. Weight, Health, Vaccination)
// ============================================================================
@Composable
fun PetStatPill(
    label: String,
    value: String,
    icon: ImageVector? = null,
    tintColor: Color = MintSage,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = tintColor.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(tintColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Column {
                Text(
                    text = label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = tintColor,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

// ============================================================================
// Section Header with Optional Action
// ============================================================================
@Composable
fun PetSectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldDark
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
        if (actionText != null && onAction != null) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionText,
                    color = MintSage,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ============================================================================
// Modern Text Input Field with Icon, Floating Label & Clean Borders
// ============================================================================
@Composable
fun PetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var passwordVisible by remember { mutableStateOf(!isPassword) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = if (placeholder != null) { { Text(placeholder, color = TextMuted) } } else null,
            singleLine = singleLine,
            isError = isError,
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = MintSage,
                unfocusedBorderColor = BorderSubtle,
                focusedLabelColor = EmeraldDark,
                unfocusedLabelColor = TextSecondary,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorBorderColor = AlertRose,
                errorLabelColor = AlertRose
            ),
            leadingIcon = if (leadingIcon != null) {
                {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MintSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = if (isError) AlertRose else MintSage,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null,
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = if (isError) AlertRose else TextSecondary
                        )
                    }
                }
            } else trailingIcon,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None
        )

        AnimatedVisibility(visible = isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage ?: "",
                color = AlertRose,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}
