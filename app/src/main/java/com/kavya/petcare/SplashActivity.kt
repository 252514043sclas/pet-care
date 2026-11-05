package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.data.repository.PetCareRepository
import com.kavya.petcare.data.session.SessionManager
import com.kavya.petcare.ui.EmeraldDark
import com.kavya.petcare.ui.MintLight
import com.kavya.petcare.ui.MintSage
import com.kavya.petcare.ui.theme.PetCareTheme
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager.getInstance(this)
        val repository = PetCareRepository(this)

        setContent {
            PetCareTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val isCheckingSession by remember { mutableStateOf(session.isLoggedIn()) }

                    SplashScreen(
                        isCheckingSession = isCheckingSession,
                        onGetStarted = {
                            val targetClass = if (session.isOnboardingCompleted()) {
                                LoginActivity::class.java
                            } else {
                                OnboardingActivity::class.java
                            }
                            val intent = Intent(this@SplashActivity, targetClass).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                    )

                    LaunchedEffect(Unit) {
                        if (session.isLoggedIn()) {
                            val startTime = System.currentTimeMillis()
                            val isValid = repository.validateSession()
                            val elapsed = System.currentTimeMillis() - startTime
                            val remainingDelay = (700L - elapsed).coerceAtLeast(0L)
                            delay(remainingDelay)

                            if (isValid && session.isLoggedIn()) {
                                val intent = Intent(this@SplashActivity, HomeDashboardActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            } else {
                                val intent = Intent(this@SplashActivity, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            }
                        } else if (session.isOnboardingCompleted()) {
                            delay(900L)
                            val intent = Intent(this@SplashActivity, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(
    isCheckingSession: Boolean = false,
    onGetStarted: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF289A8C),
                        Color(0xFF135343),
                        Color(0xFF093327)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background circles
        Box(
            modifier = Modifier
                .size(320.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .size(126.dp)
                    .scale(pulseScale),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 16.dp,
                border = BorderStroke(3.dp, Color.White.copy(alpha = 0.7f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = "Pet Care Logo",
                        tint = EmeraldDark,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "PET CARE COMPANION",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Modern Wellness",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Intelligent, loving care for your happy companions",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(52.dp))

            if (isCheckingSession) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.height(56.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(34.dp),
                        strokeWidth = 3.dp
                    )
                }
            } else {
                Button(
                    onClick = onGetStarted,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = EmeraldDark
                    ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Get Started",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }

        Text(
            text = "AI-POWERED PET WELLNESS PLATFORM",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
    }
}

