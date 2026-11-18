package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.data.models.MealEntryDto
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme

class SafeVsToxicFoodsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                SafeVsToxicFoodsScreen(
                    viewModel = viewModel,
                    onNavigateToLogMeal = {
                        startActivity(Intent(this, MealLogActivity::class.java))
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh dynamically from persistent meal history whenever returning from Log Meal
        PetViewModel.getShared(this).loadMealHistory()
    }
}

private data class GroupedFoodSafetyRecord(
    val key: String,
    val foodName: String,
    val petName: String,
    val petSpecies: String,
    val safetyStatus: String, // SAFE | TOXIC | UNKNOWN
    val explanation: String,
    val latestDate: String,
    val latestTime: String,
    val loggedCount: Int
)

private fun normalizeSafetyStatus(raw: String?): String {
    return when (raw?.trim()?.uppercase()) {
        "SAFE" -> "SAFE"
        "TOXIC" -> "TOXIC"
        else -> "UNKNOWN"
    }
}

private fun groupMealHistoryToSafetyRecords(
    meals: List<MealEntryDto>,
    fallbackPetName: String,
    fallbackPetSpecies: String
): List<GroupedFoodSafetyRecord> {
    if (meals.isEmpty()) return emptyList()

    // Meals arrive newest-first from backend; LinkedHashMap preserves latest-first group order
    val grouped = LinkedHashMap<String, MutableList<MealEntryDto>>()
    for (meal in meals) {
        val normalizedFood = meal.foodItem.trim().lowercase()
        if (normalizedFood.isBlank()) continue
        val key = "${meal.petId}_$normalizedFood"
        grouped.getOrPut(key) { mutableListOf() }.add(meal)
    }

    return grouped.map { (key, entries) ->
        val latest = entries.first()
        val petName = latest.petName.ifBlank { fallbackPetName }
        val petSpecies = latest.petSpecies.ifBlank { fallbackPetSpecies }
        val status = normalizeSafetyStatus(latest.safetyStatus)

        val explanation = when {
            !latest.safetyWarning.isNullOrBlank() -> latest.safetyWarning
            status == "TOXIC" -> "Verified toxic or dangerous for $petSpecies. Do NOT feed this item."
            status == "SAFE" -> "Verified generally safe for $petSpecies when appropriately prepared and fed in suitable amounts."
            else -> "Safety information for \"${latest.foodItem}\" could not be verified for $petSpecies. Please consult a qualified veterinarian before feeding."
        }

        GroupedFoodSafetyRecord(
            key = key,
            foodName = latest.foodItem.trim(),
            petName = petName,
            petSpecies = petSpecies,
            safetyStatus = status,
            explanation = explanation,
            latestDate = latest.mealDate,
            latestTime = latest.mealTime,
            loggedCount = entries.size
        )
    }
}

@Composable
fun SafeVsToxicFoodsScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onNavigateToLogMeal: () -> Unit = {},
    onBack: () -> Unit
) {
    val activePet by viewModel.activePet.collectAsState()
    val mealHistory by viewModel.mealHistory.collectAsState()
    val isLoading by viewModel.isLoadingMeals.collectAsState()
    val loadError by viewModel.mealHistoryError.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") } // "All", "Safe Only", "Toxic Only"

    // Load meal-derived food safety records whenever screen opens or active pet changes
    LaunchedEffect(activePet.id) {
        viewModel.loadMealHistory()
    }

    val groupedRecords = remember(mealHistory, activePet.name, activePet.type) {
        groupMealHistoryToSafetyRecords(
            meals = mealHistory,
            fallbackPetName = activePet.name,
            fallbackPetSpecies = activePet.type
        )
    }

    val filteredRecords = remember(groupedRecords, selectedFilter) {
        when (selectedFilter) {
            // UNKNOWN must NEVER appear in Safe Only or Toxic Only
            "Safe Only" -> groupedRecords.filter { it.safetyStatus == "SAFE" }
            "Toxic Only" -> groupedRecords.filter { it.safetyStatus == "TOXIC" }
            else -> groupedRecords
        }
    }

    Scaffold(
        topBar = {
            PetModernTopBar(
                title = "Safe vs Toxic Foods",
                subtitle = "Veterinary dietary guide",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onNavigateToLogMeal) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Log Meal",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        containerColor = SoftBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Active Pet Context Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        tint = MintSage,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Showing foods logged for ${activePet.name} (${activePet.type})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }

            // Filter Pills Row (Preserves Existing Design)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Safe Only", "Toxic Only").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        onClick = { selectedFilter = filter },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) EmeraldPrimary else WarmPearl,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldPrimary else BorderSubtle
                        )
                    ) {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Error Banner if API failed while cached records exist (with Retry action)
            if (loadError != null && groupedRecords.isNotEmpty()) {
                PetModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = AmberSoft,
                    borderColor = AmberAccent.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Offline mode: could not sync latest food safety checks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.loadMealHistory() }) {
                            Text("Retry", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Main Content: Loading / Error / Empty / Dynamic Food Cards
            when {
                isLoading && groupedRecords.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = EmeraldPrimary)
                            Text(
                                text = "Checking food safety records...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                loadError != null && groupedRecords.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        PetModernCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = WarmPearl
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(AmberSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = AmberAccent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Food safety service unavailable",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = loadError ?: "Unable to verify food safety records right now.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                PetGradientButton(
                                    text = "Retry",
                                    onClick = { viewModel.loadMealHistory() }
                                )
                            }
                        }
                    }
                }

                groupedRecords.isEmpty() -> {
                    // Exact Empty State Requested
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        PetModernCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = WarmPearl
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(MintSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalDining,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = "No food entries yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Log a meal for your pet to see food safety information here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                PetGradientButton(
                                    text = "Log a Meal",
                                    onClick = onNavigateToLogMeal
                                )
                            }
                        }
                    }
                }

                filteredRecords.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        PetModernCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = WarmPearl
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = when (selectedFilter) {
                                        "Safe Only" -> "No verified safe foods logged yet"
                                        "Toxic Only" -> "No toxic foods logged"
                                        else -> "No matching food records"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = when (selectedFilter) {
                                        "Safe Only" -> "Only foods verified as SAFE for ${activePet.type} appear here. Unverified foods appear under All."
                                        "Toxic Only" -> "Great news! None of ${activePet.name}'s logged meals are classified as TOXIC for ${activePet.type}."
                                        else -> "Try changing the filter or logging a new meal."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                TextButton(onClick = { selectedFilter = "All" }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Show All Logged Foods", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredRecords, key = { it.key }) { record ->
                            DynamicFoodSafetyCard(record = record)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicFoodSafetyCard(record: GroupedFoodSafetyRecord) {
    val (badgeBg, badgeColor, statusIcon) = when (record.safetyStatus) {
        "SAFE" -> Triple(MintSoft, EmeraldPrimary, Icons.Default.Check)
        "TOXIC" -> Triple(AlertRoseSoft, AlertRose, Icons.Default.Close)
        else -> Triple(AmberSoft, AmberAccent, Icons.Default.WarningAmber)
    }

    PetModernCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = WarmPearl,
        borderColor = when (record.safetyStatus) {
            "TOXIC" -> AlertRose.copy(alpha = 0.35f)
            "UNKNOWN" -> AmberAccent.copy(alpha = 0.35f)
            else -> BorderSubtle
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = record.safetyStatus,
                    tint = badgeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header: Food Name + Safety Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PetBadge(
                        text = record.safetyStatus,
                        containerColor = badgeBg,
                        contentColor = badgeColor
                    )
                }

                // Pet Name, Species & Logged Count Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = MintSage,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${record.petName} • ${record.petSpecies}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldDark
                        )
                    }

                    if (record.loggedCount > 1) {
                        PetBadge(
                            text = "Logged ${record.loggedCount} times",
                            containerColor = SoftBackground,
                            contentColor = TextSecondary
                        )
                    }
                }

                // Species-Specific Explanation
                Text(
                    text = record.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (record.safetyStatus == "TOXIC") AlertRose else TextSecondary,
                    fontWeight = if (record.safetyStatus == "TOXIC") FontWeight.Medium else FontWeight.Normal,
                    lineHeight = 18.sp
                )

                // Latest Logged Date & Time
                val timestampLabel = buildString {
                    if (record.latestDate.isNotBlank()) {
                        append(record.latestDate)
                    }
                    if (record.latestTime.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(record.latestTime)
                    }
                }
                if (timestampLabel.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (record.loggedCount > 1) "Latest: $timestampLabel" else "Logged: $timestampLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
