package com.kavya.petcare

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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.WbSunny
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

class DietPlanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                Scaffold(
                    topBar = {
                        PetModernTopBar(
                            title = "Feeding Schedule",
                            subtitle = "Daily nutrition plan",
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
                        PetModernCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MintSoft
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Brightness5,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Target: 800 kcal / day",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDark
                                    )
                                    Text(
                                        text = "Divided into 3 balanced portions throughout the day",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        PetSectionHeader(title = "Daily Meal Breakdown")

                        MealPlanCard(
                            period = "Breakfast",
                            time = "8:00 AM",
                            meal = "2 Cups Premium Kibble",
                            notes = "Serve with fresh, clean water at room temperature",
                            calories = "350 kcal",
                            icon = Icons.Default.WbSunny,
                            accentColor = AmberAccent
                        )

                        MealPlanCard(
                            period = "Lunch / Midday",
                            time = "1:00 PM",
                            meal = "Healthy Carrot Snack & Training Treats",
                            notes = "Crunchy raw baby carrots aid dental health",
                            calories = "100 kcal",
                            icon = Icons.Default.Brightness5,
                            accentColor = MintSage
                        )

                        MealPlanCard(
                            period = "Dinner",
                            time = "6:00 PM",
                            meal = "2 Cups Kibble + Warm Chicken Broth",
                            notes = "Low-sodium broth promotes hydration and appetite",
                            calories = "350 kcal",
                            icon = Icons.Default.Bedtime,
                            accentColor = LavenderAccent
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        PetGradientButton(
                            text = "Back to Diet Center",
                            onClick = { finish() }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MealPlanCard(
        period: String,
        time: String,
        meal: String,
        notes: String,
        calories: String,
        icon: ImageVector,
        accentColor: Color
    ) {
        PetModernCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = WarmPearl
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = period,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = time,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                    }

                    PetBadge(
                        text = calories,
                        containerColor = SoftBackground,
                        contentColor = EmeraldPrimary
                    )
                }

                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                Text(
                    text = meal,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
