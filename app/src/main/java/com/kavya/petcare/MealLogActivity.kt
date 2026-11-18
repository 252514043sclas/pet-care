package com.kavya.petcare

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kavya.petcare.data.models.FoodAnalysisDto
import com.kavya.petcare.data.models.MealEntryDto
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SUPPORTED_UNITS = listOf("g", "cups", "pieces", "tbsp", "tsp")

class MealLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                MealLogScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun MealLogScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activePet by viewModel.activePet.collectAsState()
    val mealHistory by viewModel.mealHistory.collectAsState()
    val foodAnalysis by viewModel.foodAnalysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingFood.collectAsState()
    val analysisError by viewModel.foodAnalysisError.collectAsState()

    var foodInput by remember { mutableStateOf("") }
    var foodError by remember { mutableStateOf<String?>(null) }

    var selectedUnit by remember { mutableStateOf("g") }
    var portionQuantity by remember { mutableFloatStateOf(50f) }
    var quantityText by remember { mutableStateOf("50") }
    var quantityError by remember { mutableStateOf<String?>(null) }

    var isSubmitting by remember { mutableStateOf(false) }
    var filterTodayOnly by remember { mutableStateOf(false) }

    var showToxicConfirmDialog by remember { mutableStateOf(false) }
    var editingMeal by remember { mutableStateOf<MealEntryDto?>(null) }
    var deletingMeal by remember { mutableStateOf<MealEntryDto?>(null) }

    val todayFormatted = remember {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
    }

    LaunchedEffect(activePet.id, filterTodayOnly) {
        viewModel.loadMealHistory(
            dateFilter = if (filterTodayOnly) todayFormatted else null
        )
    }

    LaunchedEffect(foodInput, portionQuantity, selectedUnit, activePet.type) {
        val trimmed = foodInput.trim()
        if (trimmed.isBlank()) {
            viewModel.clearFoodAnalysis()
            return@LaunchedEffect
        }
        val qty = portionQuantity.toDouble()
        if (qty <= 0.0) return@LaunchedEffect
        delay(350)
        viewModel.analyzeFood(
            foodName = trimmed,
            quantity = qty,
            unit = selectedUnit,
            species = activePet.type
        )
    }

    val sliderRange = remember(selectedUnit) { getSliderRangeForUnit(selectedUnit) }

    fun executeSaveMeal() {
        if (isSubmitting) return
        val trimmedFood = foodInput.trim()
        val parsedQty = quantityText.trim().toDoubleOrNull() ?: portionQuantity.toDouble()
        if (trimmedFood.isBlank()) {
            foodError = "Please enter a food name."
            return
        }
        if (parsedQty <= 0.0) {
            quantityError = "Portion quantity must be greater than zero."
            return
        }
        if (selectedUnit !in SUPPORTED_UNITS) {
            Toast.makeText(context, "Please select a valid unit.", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        val now = Date()
        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(now)
        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now)

        val status = foodAnalysis?.safetyStatus ?: "UNKNOWN"
        val warning = foodAnalysis?.safetyWarning
        val estCalories = if (foodAnalysis?.caloriesAvailable == true) foodAnalysis?.estimatedCalories else null

        viewModel.saveMealEntry(
            foodItem = trimmedFood,
            portionQuantity = parsedQty,
            portionUnit = selectedUnit,
            calories = estCalories,
            safetyStatus = status,
            safetyWarning = warning,
            mealDate = dateStr,
            mealTime = timeStr
        ) { success, message ->
            isSubmitting = false
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            if (success) {
                foodInput = ""
                foodError = null
                quantityError = null
                viewModel.clearFoodAnalysis()
            }
        }
    }

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
            // 1. Food Details Card
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Food Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "For ${activePet.name} (${activePet.type})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = foodInput,
                        onValueChange = {
                            foodInput = it
                            foodError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Food Item") },
                        placeholder = { Text("Enter any food, fruit, veggie, seed, or treat", color = TextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Fastfood,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                        },
                        trailingIcon = if (foodInput.isNotEmpty()) {
                            {
                                IconButton(onClick = {
                                    foodInput = ""
                                    foodError = null
                                    viewModel.clearFoodAnalysis()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextMuted
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        isError = foodError != null,
                        supportingText = if (foodError != null) {
                            { Text(foodError!!, color = AlertRose) }
                        } else null,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = MintSage,
                            unfocusedBorderColor = BorderSubtle,
                            focusedLabelColor = EmeraldDark,
                            unfocusedLabelColor = TextSecondary,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            errorBorderColor = AlertRose,
                            errorLabelColor = AlertRose
                        )
                    )
                }
            }

            // 2. Species-Based Pet Food Safety Checker Card
            if (foodInput.isNotBlank()) {
                FoodSafetyCheckerCard(
                    petName = activePet.name,
                    petSpecies = activePet.type,
                    analysis = foodAnalysis,
                    isLoading = isAnalyzing,
                    error = analysisError
                )
            }

            // 3. Dynamic Portion Size & Calorie Estimation Card
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
                                text = "Portion Size",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = when {
                                    foodInput.isBlank() -> "Enter a food to estimate calories"
                                    isAnalyzing -> "Calculating nutrition estimate..."
                                    foodAnalysis?.caloriesAvailable == true ->
                                        "Estimate via ${foodAnalysis?.calorieSource ?: "nutrition database"}"
                                    analysisError != null -> "Unable to reach nutrition service"
                                    else -> "Nutritional data not found for this item"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        val calorieBadgeText = when {
                            foodInput.isBlank() -> "-- kcal"
                            isAnalyzing -> "Checking..."
                            foodAnalysis?.caloriesAvailable == true && foodAnalysis?.estimatedCalories != null ->
                                "~${foodAnalysis!!.estimatedCalories} kcal (est.)"
                            else -> "Calories unavailable"
                        }
                        val calorieBadgeBg = if (foodAnalysis?.caloriesAvailable == true) MintSoft else SoftBackground
                        val calorieBadgeFg = if (foodAnalysis?.caloriesAvailable == true) EmeraldPrimary else TextSecondary

                        PetBadge(
                            text = calorieBadgeText,
                            containerColor = calorieBadgeBg,
                            contentColor = calorieBadgeFg
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SUPPORTED_UNITS.forEach { unit ->
                            val selected = selectedUnit == unit
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selectedUnit != unit) {
                                        selectedUnit = unit
                                        val defQty = getDefaultQuantityForUnit(unit)
                                        portionQuantity = defQty
                                        quantityText = formatQuantityNumber(defQty)
                                        quantityError = null
                                    }
                                },
                                label = {
                                    Text(
                                        text = unit,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.White else EmeraldDark
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = SoftBackground,
                                    labelColor = EmeraldDark
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = if (selected) EmeraldPrimary else BorderSubtle
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { raw ->
                                val clean = filterMealDecimal(raw)
                                quantityText = clean
                                quantityError = null
                                val parsed = clean.toFloatOrNull()
                                if (parsed != null && parsed > 0f) {
                                    portionQuantity = parsed
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Quantity ($selectedUnit)") },
                            singleLine = true,
                            isError = quantityError != null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = MintSage,
                                unfocusedBorderColor = BorderSubtle,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                errorBorderColor = AlertRose
                            )
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${formatQuantityNumber(portionQuantity)} $selectedUnit",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            val gramsEst = foodAnalysis?.gramsEstimated
                            if (gramsEst != null && selectedUnit != "g") {
                                Text(
                                    text = "≈ ${formatQuantityNumber(gramsEst.toFloat())} g",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    if (quantityError != null) {
                        Text(
                            text = quantityError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = AlertRose
                        )
                    }

                    Slider(
                        value = portionQuantity.coerceIn(sliderRange.start, sliderRange.endInclusive),
                        onValueChange = { newVal ->
                            val snapped = snapQuantityForUnit(newVal, selectedUnit)
                            portionQuantity = snapped
                            quantityText = formatQuantityNumber(snapped)
                            quantityError = null
                        },
                        valueRange = sliderRange,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldPrimary,
                            activeTrackColor = MintSage,
                            inactiveTrackColor = BorderSubtle
                        )
                    )
                }
            }

            // 4. Save Meal Entry Button
            val isToxicFood = foodAnalysis?.safetyStatus == "TOXIC"
            PetGradientButton(
                text = if (isToxicFood) "Warning: Toxic Food — Log Anyway?" else "Save Meal Entry",
                enabled = foodInput.isNotBlank() && !isSubmitting && !isAnalyzing,
                isLoading = isSubmitting,
                onClick = {
                    val parsedQty = quantityText.trim().toDoubleOrNull()
                    if (foodInput.trim().isBlank()) {
                        foodError = "Please enter a food name."
                    } else if (parsedQty == null || parsedQty <= 0.0) {
                        quantityError = "Portion quantity must be greater than zero."
                    } else if (isToxicFood) {
                        showToxicConfirmDialog = true
                    } else {
                        executeSaveMeal()
                    }
                }
            )

            // 5. Meal History Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PetSectionHeader(title = "Meal History")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !filterTodayOnly,
                        onClick = { filterTodayOnly = false },
                        label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = filterTodayOnly,
                        onClick = { filterTodayOnly = true },
                        label = { Text("Today", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (mealHistory.isEmpty()) {
                PetModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = WarmPearl
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(SoftBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalDining,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Text(
                            text = "No meals recorded yet. Add your pet's first meal to start tracking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                mealHistory.forEach { entry ->
                    MealHistoryItemCard(
                        entry = entry,
                        onEdit = { editingMeal = entry },
                        onDelete = { deletingMeal = entry }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showToxicConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showToxicConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Dangerous,
                    contentDescription = null,
                    tint = AlertRose,
                    modifier = Modifier.size(34.dp)
                )
            },
            title = {
                Text(
                    text = "Toxic Food Warning!",
                    fontWeight = FontWeight.Bold,
                    color = AlertRose
                )
            },
            text = {
                Text(
                    text = "${foodAnalysis?.safetyWarning ?: "This food is toxic to ${activePet.type}."}\n\nDo NOT feed this to ${activePet.name}. Only save this record if you are logging an accidental exposure for veterinary reference.",
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showToxicConfirmDialog = false
                        executeSaveMeal()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AlertRose)
                ) {
                    Text("Log for Vet Record", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showToxicConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Cancel (Do Not Feed)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = WarmPearl,
            shape = RoundedCornerShape(18.dp)
        )
    }

    editingMeal?.let { meal ->
        EditMealEntryDialog(
            entry = meal,
            petSpecies = activePet.type,
            viewModel = viewModel,
            onDismiss = { editingMeal = null },
            onSave = { food, qty, unit, cal, status, warning ->
                viewModel.updateMealEntry(
                    id = meal.id,
                    foodItem = food,
                    portionQuantity = qty,
                    portionUnit = unit,
                    calories = cal,
                    safetyStatus = status,
                    safetyWarning = warning,
                    mealDate = meal.mealDate,
                    mealTime = meal.mealTime
                ) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    if (success) {
                        editingMeal = null
                    }
                }
            }
        )
    }

    deletingMeal?.let { meal ->
        AlertDialog(
            onDismissRequest = { deletingMeal = null },
            title = {
                Text(
                    text = "Delete Meal Entry?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Remove '${meal.foodItem}' (${meal.portionSize}) from Meal History?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMealEntry(meal.id) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (success) {
                                deletingMeal = null
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AlertRose)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMeal = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = WarmPearl,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun FoodSafetyCheckerCard(
    petName: String,
    petSpecies: String,
    analysis: FoodAnalysisDto?,
    isLoading: Boolean,
    error: String?
) {
    val status = analysis?.safetyStatus ?: "UNKNOWN"

    val (bgColor, borderColor, accentColor, icon, badgeLabel) = when {
        isLoading -> SafetyCardStyle(SoftBackground, BorderSubtle, MintSage, Icons.Default.HourglassTop, "CHECKING...")
        error != null -> SafetyCardStyle(AmberSoft, AmberAccent.copy(alpha = 0.5f), AmberAccent, Icons.Default.WarningAmber, "UNKNOWN")
        status == "SAFE" -> SafetyCardStyle(MintSoft, EmeraldPrimary.copy(alpha = 0.35f), EmeraldPrimary, Icons.Default.CheckCircle, "SAFE")
        status == "TOXIC" -> SafetyCardStyle(AlertRoseSoft, AlertRose.copy(alpha = 0.6f), AlertRose, Icons.Default.Dangerous, "TOXIC")
        else -> SafetyCardStyle(AmberSoft, AmberAccent.copy(alpha = 0.5f), AmberAccent, Icons.Default.WarningAmber, "UNKNOWN")
    }

    PetModernCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = bgColor,
        borderColor = borderColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MintSage
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = badgeLabel,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Pet Food Safety ($petSpecies)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                PetBadge(
                    text = badgeLabel,
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            }

            val messageText = when {
                isLoading -> "Checking safety for $petName ($petSpecies) and calculating nutrition..."
                error != null -> "Could not verify safety online ($error). Please consult a qualified veterinarian before feeding."
                !analysis?.safetyWarning.isNullOrBlank() -> analysis!!.safetyWarning
                else -> "Safety information is unavailable. Please verify with a qualified veterinarian before feeding."
            }

            Text(
                text = messageText,
                style = MaterialTheme.typography.bodySmall,
                color = if (status == "TOXIC" && !isLoading) AlertRose else TextPrimary,
                fontWeight = if (status == "TOXIC" && !isLoading) FontWeight.SemiBold else FontWeight.Normal,
                lineHeight = 19.sp
            )
        }
    }
}

private data class SafetyCardStyle(
    val bg: Color,
    val border: Color,
    val accent: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

@Composable
private fun MealHistoryItemCard(
    entry: MealEntryDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (statusBg, statusFg) = when (entry.safetyStatus.uppercase()) {
        "SAFE" -> Pair(MintSoft, EmeraldPrimary)
        "TOXIC" -> Pair(AlertRoseSoft, AlertRose)
        else -> Pair(AmberSoft, AmberAccent)
    }

    PetModernCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = WarmPearl
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.foodItem,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = buildString {
                            append(entry.portionSize)
                            if (entry.mealDate.isNotBlank()) {
                                append(" • ")
                                append(entry.mealDate)
                            }
                            if (entry.mealTime.isNotBlank()) {
                                append(" at ")
                                append(entry.mealTime)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                PetBadge(
                    text = entry.safetyStatus.uppercase(),
                    containerColor = statusBg,
                    contentColor = statusFg
                )
            }

            if (entry.safetyStatus.equals("TOXIC", ignoreCase = true) && !entry.safetyWarning.isNullOrBlank()) {
                Text(
                    text = entry.safetyWarning,
                    style = MaterialTheme.typography.labelSmall,
                    color = AlertRose,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val calLabel = if (entry.calories != null) {
                    "~${entry.calories} kcal (est.)"
                } else {
                    "Calories unavailable"
                }
                PetBadge(
                    text = calLabel,
                    containerColor = SoftBackground,
                    contentColor = if (entry.calories != null) EmeraldPrimary else TextSecondary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TextButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MintSage)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Meal",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", style = MaterialTheme.typography.labelMedium)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete Meal",
                            tint = AlertRose.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditMealEntryDialog(
    entry: MealEntryDto,
    petSpecies: String,
    viewModel: PetViewModel,
    onDismiss: () -> Unit,
    onSave: (
        foodItem: String,
        quantity: Double,
        unit: String,
        calories: Int?,
        safetyStatus: String,
        safetyWarning: String?
    ) -> Unit
) {
    val scope = rememberCoroutineScope()
    var foodText by remember { mutableStateOf(entry.foodItem) }
    var unit by remember { mutableStateOf(if (entry.portionUnit in SUPPORTED_UNITS) entry.portionUnit else "g") }
    var qtyText by remember { mutableStateOf(formatQuantityNumber(entry.portionQuantity.toFloat())) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .imePadding()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Edit Meal Entry",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF123F32)
                    )

                    OutlinedTextField(
                        value = foodText,
                        onValueChange = {
                            foodText = it
                            errorMsg = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Food Name") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF1C2925), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C2925),
                            unfocusedTextColor = Color(0xFF1C2925),
                            focusedBorderColor = Color(0xFF123F32),
                            unfocusedBorderColor = Color(0xFFDFE8E3),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SUPPORTED_UNITS.forEach { u ->
                            val selected = unit == u
                            FilterChip(
                                selected = selected,
                                onClick = { unit = u },
                                label = { Text(u) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF123F32),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = {
                            qtyText = filterMealDecimal(it)
                            errorMsg = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Portion Quantity ($unit)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(color = Color(0xFF1C2925), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C2925),
                            unfocusedTextColor = Color(0xFF1C2925),
                            focusedBorderColor = Color(0xFF123F32),
                            unfocusedBorderColor = Color(0xFFDFE8E3),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = AlertRose
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFDFE8E3))
                        ) {
                            Text("Cancel", color = Color(0xFF123F32), fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val f = foodText.trim()
                                val q = qtyText.trim().toDoubleOrNull()
                                if (f.isBlank()) {
                                    errorMsg = "Food name cannot be empty."
                                } else if (q == null || q <= 0.0) {
                                    errorMsg = "Quantity must be greater than zero."
                                } else {
                                    isSaving = true
                                    scope.launch {
                                        val analyzed = viewModel.analyzeFoodDirect(f, q, unit, petSpecies).getOrNull()
                                        isSaving = false
                                        onSave(
                                            f,
                                            q,
                                            unit,
                                            if (analyzed?.caloriesAvailable == true) analyzed.estimatedCalories else null,
                                            analyzed?.safetyStatus ?: "UNKNOWN",
                                            analyzed?.safetyWarning
                                        )
                                    }
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF123F32))
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getSliderRangeForUnit(unit: String): ClosedFloatingPointRange<Float> {
    return when (unit) {
        "g" -> 5f..500f
        "cups" -> 0.25f..5.0f
        "pieces" -> 1f..20f
        "tbsp" -> 0.5f..12f
        "tsp" -> 0.5f..12f
        else -> 1f..100f
    }
}

private fun getDefaultQuantityForUnit(unit: String): Float {
    return when (unit) {
        "g" -> 50f
        "cups" -> 1.0f
        "pieces" -> 1f
        "tbsp" -> 1f
        "tsp" -> 1f
        else -> 1f
    }
}

private fun snapQuantityForUnit(value: Float, unit: String): Float {
    return when (unit) {
        "g" -> (kotlin.math.round(value / 5f) * 5f).coerceAtLeast(5f)
        "cups" -> (kotlin.math.round(value * 4f) / 4f).coerceAtLeast(0.25f)
        "pieces" -> kotlin.math.round(value).coerceAtLeast(1f)
        "tbsp", "tsp" -> (kotlin.math.round(value * 2f) / 2f).coerceAtLeast(0.5f)
        else -> value
    }
}

private fun formatQuantityNumber(qty: Float): String {
    return if (qty % 1f == 0f) {
        qty.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
    }
}

private fun filterMealDecimal(raw: String): String {
    var dotSeen = false
    val sb = StringBuilder()
    for (ch in raw) {
        if (ch.isDigit()) {
            sb.append(ch)
        } else if (ch == '.' && !dotSeen) {
            dotSeen = true
            sb.append(ch)
        }
    }
    return sb.toString()
}
