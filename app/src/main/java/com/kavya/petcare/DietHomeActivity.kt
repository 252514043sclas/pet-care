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
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme

class DietHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                Scaffold(
                    topBar = {
                        PetModernTopBar(
                            title = "Diet & Nutrition",
                            subtitle = "Nutrition planning & tracking",
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
                        // Daily Calorie Summary Card
                        PetModernCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = WarmPearl
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Daily Calorie Target",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Balanced diet for active pet",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    PetBadge(
                                        text = "81% Met",
                                        containerColor = MintSoft,
                                        contentColor = EmeraldPrimary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "650",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = " / 800 kcal",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "150 kcal left",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AmberAccent
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { 650f / 800f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    color = MintSage,
                                    trackColor = BorderSubtle
                                )
                            }
                        }

                        PetSectionHeader(title = "Diet Tools & Guides")

                        DietNavCard(
                            title = "Today's Meal Plan",
                            description = "Structured morning, noon, and evening schedule",
                            icon = Icons.Default.Restaurant,
                            accentColor = EmeraldPrimary,
                            onClick = {
                                startActivity(Intent(this@DietHomeActivity, DietPlanActivity::class.java))
                            }
                        )

                        DietNavCard(
                            title = "Meal Log",
                            description = "Record meals, snacks, and portion sizes",
                            icon = Icons.Default.Fastfood,
                            accentColor = AmberAccent,
                            onClick = {
                                startActivity(Intent(this@DietHomeActivity, MealLogActivity::class.java))
                            }
                        )

                        DietNavCard(
                            title = "Safe vs Toxic Foods",
                            description = "Check toxic ingredients before feeding your pet",
                            icon = Icons.Default.WarningAmber,
                            accentColor = AlertRose,
                            onClick = {
                                startActivity(Intent(this@DietHomeActivity, SafeVsToxicFoodsActivity::class.java))
                            }
                        )

                        DietNavCard(
                            title = "Weight Tracker",
                            description = "Monitor weight trends, target range & history",
                            icon = Icons.Default.MonitorWeight,
                            accentColor = SkyBlue,
                            onClick = {
                                startActivity(Intent(this@DietHomeActivity, WeightTrackerActivity::class.java))
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun DietNavCard(
        title: String,
        description: String,
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
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
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
