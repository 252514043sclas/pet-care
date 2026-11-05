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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.MonitorWeight
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

class WeightTrackerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                WeightTrackerScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun WeightTrackerScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onBack: () -> Unit
) {
    var weightInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val activePet by viewModel.activePet.collectAsState()
    val weightHistory by viewModel.weightHistory.collectAsState()

    val currentWeightNum = activePet.weight.replace(" kg", "").toFloatOrNull() ?: 1.8f
    val targetWeight = String.format("%.1f", currentWeightNum * 0.95f)

    Scaffold(
        topBar = {
            PetModernTopBar(
                title = "Weight Tracker",
                subtitle = "Weight trends & milestones",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Current Weight Card
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
                                text = "${activePet.name}'s Weight",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Target: $targetWeight kg",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        PetBadge(
                            text = "Optimal Range",
                            containerColor = MintSoft,
                            contentColor = EmeraldPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = activePet.weight.replace(" kg", ""),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = " kg",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MintSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonitorWeight,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            PetSectionHeader(title = "Log New Weight")

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
                    PetTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = "Weight in kg",
                        placeholder = "e.g., 2.15",
                        leadingIcon = Icons.AutoMirrored.Filled.TrendingUp
                    )

                    PetGradientButton(
                        text = "Record Weight",
                        enabled = weightInput.isNotBlank() && !isSubmitting,
                        isLoading = isSubmitting,
                        onClick = {
                            val weightFloat = weightInput.toFloatOrNull()
                            if (weightFloat != null && weightFloat > 0f) {
                                isSubmitting = true
                                val todayDate = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date())
                                viewModel.addWeightEntry(todayDate, weightFloat) {
                                    isSubmitting = false
                                    onBack()
                                }
                            }
                        }
                    )
                }
            }

            if (weightHistory.isNotEmpty()) {
                PetSectionHeader(title = "Recent History")

                weightHistory.takeLast(5).reversed().forEach { entry ->
                    PetModernCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = WarmPearl
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(SoftBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = MintSage,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = entry.date,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                            PetBadge(
                                text = "${entry.weightKg} kg",
                                containerColor = MintSoft,
                                contentColor = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
