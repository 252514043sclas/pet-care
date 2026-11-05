package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme
import kotlinx.coroutines.delay

class AnalyzingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                var stepText by remember { mutableStateOf("Processing visual features...") }

                LaunchedEffect(Unit) {
                    delay(800)
                    stepText = "Cross-referencing veterinary database..."
                    delay(900)
                    stepText = "Generating clinical report..."
                    delay(800)
                    startActivity(Intent(this@AnalyzingActivity, ResultActivity::class.java))
                    finish()
                }

                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Scaffold(
                    topBar = {
                        PetModernTopBar(
                            title = "AI Health Scanner",
                            subtitle = "Analyzing clinical data",
                            onBack = { finish() }
                        )
                    },
                    containerColor = SoftBackground
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(28.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(MintSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(36.dp))

                            Text(
                                text = "Analyzing Health Indicators",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stepText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MintSage,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Our veterinary AI is evaluating symptoms and scan results to produce an assessment.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = TextSecondary,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            LinearProgressIndicator(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = MintSage,
                                trackColor = BorderSubtle
                            )
                        }
                    }
                }
            }
        }
    }
}
