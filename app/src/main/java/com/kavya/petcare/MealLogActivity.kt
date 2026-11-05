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
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme

class MealLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                MealLogScreen(
                    viewModel = viewModel,
                    onMealLogged = { finish() },
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun MealLogScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onMealLogged: () -> Unit,
    onBack: () -> Unit = {}
) {
    var mealInput by remember { mutableStateOf("") }
    var portionSize by remember { mutableFloatStateOf(1.0f) }
    var isSubmitting by remember { mutableStateOf(false) }

    val calculatedCalories = (portionSize * 150).toInt()

    Scaffold(
        topBar = {
            PetModernTopBar(
                title = "Log Meal",
                subtitle = "Track food & portion size",
                onBack = onBack
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PetModernCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = WarmPearl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MintSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalDining,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Food Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Enter food item or snack name",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    PetTextField(
                        value = mealInput,
                        onValueChange = { mealInput = it },
                        label = "Food Item",
                        placeholder = "e.g., Premium Salmon Kibble & Greens",
                        leadingIcon = Icons.Default.Fastfood
                    )
                }
            }

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
                                text = "Portion Size",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Estimated ~150 kcal per cup",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        PetBadge(
                            text = "~$calculatedCalories kcal",
                            containerColor = MintSoft,
                            contentColor = EmeraldPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${String.format("%.1f", portionSize)} Cups",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }

                    Slider(
                        value = portionSize,
                        onValueChange = { portionSize = it },
                        valueRange = 0.5f..3.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldPrimary,
                            activeTrackColor = MintSage,
                            inactiveTrackColor = BorderSubtle
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            PetGradientButton(
                text = "Save Meal Entry",
                enabled = mealInput.isNotBlank() && !isSubmitting,
                isLoading = isSubmitting,
                onClick = {
                    if (mealInput.isNotBlank()) {
                        isSubmitting = true
                        viewModel.addMeal(
                            mealName = mealInput,
                            calories = calculatedCalories,
                            time = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date()),
                            onDone = {
                                isSubmitting = false
                                onMealLogged()
                            }
                        )
                    }
                }
            )
        }
    }
}
