package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.data.session.SessionManager
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager.getInstance(this).isLoggedIn()) {
            val intent = Intent(this, HomeDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }
        setContent {
            PetCareTheme {
                OnboardingScreen(
                    onContinue = {
                        SessionManager.getInstance(this@OnboardingActivity).setOnboardingCompleted(true)
                        startActivity(Intent(this@OnboardingActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }

    val titles = listOf(
        "Complete Pet Care in Your Pocket",
        "AI Diagnostic & Health Checks",
        "Effortless Meals & Daily Routine"
    )

    val descriptions = listOf(
        "Monitor your pets' wellness, vaccination status, and personal details in one secure place.",
        "Scan symptoms and track health changes with real-time veterinary care recommendations.",
        "Keep daily feeding schedules, weight logs, and grooming reminders perfectly organized."
    )

    val icons = listOf(Icons.Default.Pets, Icons.Default.Favorite, Icons.Default.Notifications)
    val accentColors = listOf(MintSage, Color(0xFFE63946), AmberAccent)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onContinue,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Skip",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.weight(0.8f))

        // Multi-layer illustration with decorative aura
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(accentColors[page].copy(alpha = 0.08f))
            )
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(accentColors[page].copy(alpha = 0.15f))
            )
            Surface(
                modifier = Modifier
                    .size(118.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = accentColors[page].copy(alpha = 0.3f),
                        spotColor = accentColors[page].copy(alpha = 0.4f)
                    ),
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(2.dp, accentColors[page].copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icons[page],
                        contentDescription = "Illustration",
                        tint = accentColors[page],
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(44.dp))

        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "textContent"
        ) { targetPage ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = titles[targetPage],
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = descriptions[targetPage],
                    fontSize = 15.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        // Modern expanding capsule page indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val isCurrent = index == page
                val width by animateDpAsState(
                    targetValue = if (isCurrent) 28.dp else 8.dp,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                    label = "indicatorWidth"
                )
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(width)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isCurrent) MintSage else Color(0xFFD4DFDA)
                        )
                )
            }
        }

        Spacer(Modifier.weight(1f))

        PetGradientButton(
            text = if (page == 2) "Get Started" else "Continue",
            onClick = {
                if (page < 2) {
                    page++
                } else {
                    onContinue()
                }
            }
        )

        Spacer(Modifier.height(12.dp))
    }
}
