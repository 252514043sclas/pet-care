package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme

class HealthHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                Scaffold(
                    topBar = {
                        PetModernTopBar(
                            title = "Health & Diagnostics",
                            subtitle = "AI-driven wellness assessments",
                            onBack = { finish() }
                        )
                    },
                    containerColor = SoftBackground
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Banner Card
                        PetModernCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MintSoft
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Smart Health Checker",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDark
                                    )
                                    PetBadge(
                                        text = "AI Powered",
                                        containerColor = WarmPearl,
                                        contentColor = EmeraldPrimary
                                    )
                                }
                                Text(
                                    text = "Scan visible symptoms, skin, coat, or eyes for instant veterinary triage guidance.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        PetSectionHeader(title = "Diagnostic Tools")

                        HealthActionCard(
                            title = "AI Photo Scan",
                            subtitle = "Capture skin, coat, teeth, or eye condition for instant AI visual check",
                            icon = Icons.Default.CameraAlt,
                            accentColor = AlertRose,
                            onClick = {
                                startActivity(Intent(this@HealthHomeActivity, PhotoScanActivity::class.java))
                            }
                        )

                        HealthActionCard(
                            title = "Symptom Checker",
                            subtitle = "Select observable signs like itching, lethargy, or loss of appetite",
                            icon = Icons.Default.Healing,
                            accentColor = EmeraldPrimary,
                            onClick = {
                                startActivity(Intent(this@HealthHomeActivity, SymptomSelectionActivity::class.java))
                            }
                        )

                        HealthActionCard(
                            title = "Condition Encyclopedia",
                            subtitle = "Browse verified diseases, emergency warning signs, and care guidelines",
                            icon = Icons.Default.HealthAndSafety,
                            accentColor = LavenderAccent,
                            onClick = {
                                startActivity(Intent(this@HealthHomeActivity, DiseaseDetailActivity::class.java))
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun HealthActionCard(
        title: String,
        subtitle: String,
        icon: ImageVector,
        accentColor: Color,
        onClick: () -> Unit
    ) {
        PetModernCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            containerColor = WarmPearl
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
