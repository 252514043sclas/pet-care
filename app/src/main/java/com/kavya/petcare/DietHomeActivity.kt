package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme

class DietHomeActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        val vm = PetViewModel.getShared(this)
        vm.loadMealSchedules()
        vm.loadDietSummary()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val context = LocalContext.current
                val viewModel = remember { PetViewModel.getShared(context) }

                val activePet by viewModel.activePet.collectAsState()
                val dietSummary by viewModel.dietSummary.collectAsState()
                val mealSchedules by viewModel.mealSchedules.collectAsState()

                LaunchedEffect(activePet.id) {
                    val petId = activePet.id.toIntOrNull()
                    viewModel.loadMealSchedules(petId)
                    viewModel.loadDietSummary(petId)
                }

                val targetCalories = dietSummary?.targetCalories ?: 0
                val consumedCalories = dietSummary?.consumedCalories ?: 0
                val remainingCalories = (targetCalories - consumedCalories).coerceAtLeast(0)
                val hasSchedule = (dietSummary?.hasSchedule == true) || (mealSchedules.isNotEmpty() && targetCalories > 0)
                val progressRatio = if (targetCalories > 0) {
                    (consumedCalories.toFloat() / targetCalories.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val percentMet = if (targetCalories > 0) {
                    ((consumedCalories.toFloat() / targetCalories.toFloat()) * 100).toInt()
                } else {
                    0
                }

                Scaffold(
                    topBar = {
                        PetModernTopBar(
                            title = "Diet & Nutrition",
                            subtitle = if (activePet.name.isNotBlank()) "Nutrition planning for ${activePet.name}" else "Nutrition planning & tracking",
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Daily Calorie Target",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (hasSchedule) {
                                                "Target calculated from ${mealSchedules.size} scheduled meal${if (mealSchedules.size == 1) "" else "s"}"
                                            } else {
                                                "Set up your feeding schedule to view your daily calorie target."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    PetBadge(
                                        text = "$percentMet% Met",
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
                                            text = "$consumedCalories",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = if (targetCalories > 0) " / $targetCalories kcal" else " / 0 kcal/day",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = if (targetCalories > 0) "$remainingCalories kcal left" else "0 kcal left",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AmberAccent
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { progressRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    color = MintSage,
                                    trackColor = BorderSubtle
                                )

                                // When no feeding schedule exists: show clear informational message with setup action
                                if (!hasSchedule) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                startActivity(Intent(this@DietHomeActivity, DietPlanActivity::class.java))
                                            },
                                        color = AmberSoft.copy(alpha = 0.6f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = AmberAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Set up your feeding schedule to view your daily calorie target.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Tap to configure meals & calculate target",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Calorie Breakdown by Configured Meals (when feeding schedule exists)
                        if (hasSchedule && mealSchedules.isNotEmpty()) {
                            PetSectionHeader(title = "Scheduled Meals Breakdown")
                            PetModernCard(
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = WarmPearl
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    mealSchedules.forEach { meal ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.White)
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(MintSoft),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Schedule,
                                                        contentDescription = null,
                                                        tint = EmeraldPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = meal.mealName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                    val detailText = listOfNotNull(
                                                        meal.scheduledTime.takeIf { it.isNotBlank() },
                                                        meal.foodName.takeIf { it.isNotBlank() }?.let { f ->
                                                            if (meal.foodQuantity.isNotBlank()) "$f (${meal.foodQuantity})" else f
                                                        }
                                                    ).joinToString(" • ")
                                                    if (detailText.isNotBlank()) {
                                                        Text(
                                                            text = detailText,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                }
                                            }
                                            PetBadge(
                                                text = "${meal.calories} kcal",
                                                containerColor = MintSoft,
                                                contentColor = EmeraldPrimary
                                            )
                                        }
                                    }
                                }
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
