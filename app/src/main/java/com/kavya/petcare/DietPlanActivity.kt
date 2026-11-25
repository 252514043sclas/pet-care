package com.kavya.petcare

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.kavya.petcare.data.models.MealScheduleDto
import com.kavya.petcare.notifications.MealNotificationScheduler
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme
import java.util.Calendar

// ============================================================================
// Dialog Palette — Dark Green, Dark Charcoal, Grey-Green & Clean White
// ============================================================================
private val DialogWhite = Color(0xFFFFFFFF)
private val DialogTitleDarkGreen = Color(0xFF123F32)
private val InputDarkCharcoal = Color(0xFF1C2925)
private val LabelGreyGreen = Color(0xFF53645D)
private val BorderLightGreyGreen = Color(0xFFDFE8E3)
private val DialogCardBackground = Color(0xFFF7FAF8)

class DietPlanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PetCareTheme {
                DietPlanScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun DietPlanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel = remember { PetViewModel.getShared(context) }

    val activePet by viewModel.activePet.collectAsState()
    val mealSchedules by viewModel.mealSchedules.collectAsState()
    val dietSummary by viewModel.dietSummary.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<MealScheduleDto?>(null) }
    var deletingSchedule by remember { mutableStateOf<MealScheduleDto?>(null) }

    // Android 13+ Notification Permission State
    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotifPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Meal reminders enabled!", Toast.LENGTH_SHORT).show()
            val petId = activePet.id.toIntOrNull() ?: 1
            MealNotificationScheduler.rescheduleAllForPet(context, petId, activePet.name, mealSchedules)
        } else {
            Toast.makeText(context, "Notifications are required for meal alerts", Toast.LENGTH_LONG).show()
        }
    }

    // Android 12+ Exact Alarm Permission Check
    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager }
    val canExactAlarm = remember(mealSchedules) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    LaunchedEffect(activePet.id) {
        val petId = activePet.id.toIntOrNull()
        viewModel.loadMealSchedules(petId)
        viewModel.loadDietSummary(petId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            PetModernTopBar(
                title = "Feeding Schedule",
                subtitle = "Daily nutrition plan",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Add Meal",
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Notification Permission Warning Banner (if denied on API 33+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                PetModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = AmberSoft,
                    borderColor = AmberAccent.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notifications Disabled",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Enable notifications so you never miss ${activePet.name}'s feeding times.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        TextButton(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.textButtonColors(contentColor = EmeraldPrimary)
                        ) {
                            Text("Enable", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Exact Alarm Settings Banner (if Android 12+ needs exact alarm grant)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canExactAlarm) {
                PetModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MintSoft,
                    borderColor = MintSage.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MintSage,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exact Alarms Permission",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Allow exact alarms to get on-time feeding alerts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = EmeraldPrimary)
                        ) {
                            Text("Grant", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Daily Calorie Target Section
            val targetCalories = dietSummary?.targetCalories ?: 0
            val consumedCalories = dietSummary?.consumedCalories ?: 0
            val remainingCalories = (targetCalories - consumedCalories).coerceAtLeast(0)
            val hasSchedule = (dietSummary?.hasSchedule == true) || (mealSchedules.isNotEmpty() && targetCalories > 0)
            val progressRatio = if (targetCalories > 0) (consumedCalories.toFloat() / targetCalories.toFloat()).coerceIn(0f, 1f) else 0f
            val percentMet = if (targetCalories > 0) ((consumedCalories.toFloat() / targetCalories.toFloat()) * 100).toInt() else 0
            val mealCount = mealSchedules.size

            PetModernCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = WarmPearl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MintSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brightness5,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (targetCalories > 0) "Target: $targetCalories kcal / day" else "Target: 0 kcal / day",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                            Text(
                                text = if (hasSchedule) {
                                    "Divided into $mealCount balanced portion${if (mealCount == 1) "" else "s"} throughout the day"
                                } else {
                                    "Set up your feeding schedule to view your daily calorie target."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        PetBadge(
                            text = "$percentMet% Met",
                            containerColor = MintSoft,
                            contentColor = EmeraldPrimary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MintSage,
                        trackColor = BorderSubtle
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$consumedCalories kcal consumed",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = if (targetCalories > 0) "$remainingCalories kcal remaining" else "0 kcal remaining",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AmberAccent
                        )
                    }
                }
            }

            // Daily Meal Breakdown Header + Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Meal Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )

                TextButton(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MintSage)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Meal", fontWeight = FontWeight.SemiBold)
                }
            }

            // Meal Schedule Cards
            val sortedSchedules = remember(mealSchedules) {
                mealSchedules.sortedWith(compareBy { schedule ->
                    val name = schedule.mealName.lowercase()
                    when {
                        name.contains("breakfast") -> 1
                        name.contains("lunch") -> 2
                        name.contains("snack") || name.contains("midday") || name.contains("evening") -> 3
                        name.contains("dinner") || name.contains("night") -> 4
                        else -> 5
                    }
                })
            }

            if (sortedSchedules.isEmpty() && !isLoading) {
                PetModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = WarmPearl
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No feeding schedules configured yet.\nSet up your feeding schedule to view your daily calorie target.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PetGradientButton(
                            text = "Add Feeding Schedule",
                            onClick = { showAddDialog = true }
                        )
                    }
                }
            } else {
                sortedSchedules.forEach { schedule ->
                    FeedingMealCard(
                        schedule = schedule,
                        onToggleReminder = {
                            viewModel.toggleMealReminder(schedule.id) { success ->
                                if (success) {
                                    val statusText = if (schedule.isReminderEnabled == 1) "disabled" else "enabled"
                                    Toast.makeText(context, "${schedule.mealName} reminder $statusText", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onEdit = { editingSchedule = schedule },
                        onDelete = { deletingSchedule = schedule }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            PetGradientButton(
                text = "Back to Diet Center",
                onClick = onBack
            )
        }
    }

    // Edit Meal Dialog
    editingSchedule?.let { schedule ->
        MealEditDialog(
            schedule = schedule,
            onDismiss = { editingSchedule = null },
            onSave = { updatedName, updatedTime, foodName, foodQty, instructions, calories, reminderEnabled ->
                viewModel.updateMealSchedule(
                    id = schedule.id,
                    mealName = updatedName,
                    scheduledTime = updatedTime,
                    foodName = foodName,
                    foodQuantity = foodQty,
                    instructions = instructions,
                    calories = calories,
                    isReminderEnabled = if (reminderEnabled) 1 else 0,
                    onSuccess = {
                        Toast.makeText(context, "Meal updated successfully", Toast.LENGTH_SHORT).show()
                        editingSchedule = null
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    // Add Meal Dialog
    if (showAddDialog) {
        MealAddDialog(
            onDismiss = { showAddDialog = false },
            onSave = { mealName, scheduledTime, foodName, foodQty, instructions, calories, reminderEnabled ->
                viewModel.createMealSchedule(
                    mealName = mealName,
                    scheduledTime = scheduledTime,
                    foodName = foodName,
                    foodQuantity = foodQty,
                    instructions = instructions,
                    calories = calories,
                    isReminderEnabled = if (reminderEnabled) 1 else 0,
                    onSuccess = {
                        Toast.makeText(context, "Meal schedule created", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    // Delete Confirmation Dialog
    deletingSchedule?.let { schedule ->
        AlertDialog(
            onDismissRequest = { deletingSchedule = null },
            title = {
                Text(
                    text = "Delete Meal Schedule?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '${schedule.mealName}' from ${activePet.name}'s feeding schedule?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMealSchedule(
                            id = schedule.id,
                            onSuccess = {
                                Toast.makeText(context, "Meal deleted", Toast.LENGTH_SHORT).show()
                                deletingSchedule = null
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                deletingSchedule = null
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AlertRose)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSchedule = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = WarmPearl,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun FeedingMealCard(
    schedule: MealScheduleDto,
    onToggleReminder: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val meta = remember(schedule.mealName) {
        getMealMeta(schedule.mealName)
    }

    val isReminderOn = schedule.isReminderEnabled == 1

    PetModernCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = WarmPearl
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Icon + Period/Time + Switch & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(meta.accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = meta.icon,
                            contentDescription = null,
                            tint = meta.accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = schedule.mealName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = schedule.scheduledTime,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (schedule.calories > 0) {
                        PetBadge(
                            text = "${schedule.calories} kcal",
                            containerColor = SoftBackground,
                            contentColor = EmeraldPrimary
                        )
                    }

                    // Reminder Switch
                    Switch(
                        checked = isReminderOn,
                        onCheckedChange = { onToggleReminder() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MintSage,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = BorderSubtle
                        )
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            // Food Name & Quantity
            val foodDisplay = buildString {
                if (schedule.foodQuantity.isNotBlank()) {
                    append(schedule.foodQuantity.trim())
                    append(" ")
                }
                if (schedule.foodName.isNotBlank()) {
                    append(schedule.foodName.trim())
                } else if (schedule.mealDescription.isNotBlank()) {
                    append(schedule.mealDescription.trim())
                }
            }.trim()

            if (foodDisplay.isNotBlank()) {
                Text(
                    text = foodDisplay,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            // Feeding Instructions / Notes
            val instructions = if (schedule.instructions.isNotBlank()) {
                schedule.instructions.trim()
            } else if (schedule.mealDescription.isNotBlank() && schedule.mealDescription != schedule.foodName) {
                schedule.mealDescription.trim()
            } else {
                ""
            }

            if (instructions.isNotBlank()) {
                Text(
                    text = instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            // Action row: Reminder status badge + Edit & Delete options
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
                        imageVector = if (isReminderOn) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                        contentDescription = null,
                        tint = if (isReminderOn) MintSage else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isReminderOn) "Reminder active" else "Reminder off",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isReminderOn) MintSage else TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MintSage)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
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
                            contentDescription = "Delete",
                            tint = AlertRose.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// High-Contrast Input Field Helper
// ============================================================================

@Composable
private fun DietInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 5,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (isError) AlertRose else LabelGreyGreen
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LabelGreyGreen.copy(alpha = 0.75f)
                )
            },
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(
                color = InputDarkCharcoal,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            shape = RoundedCornerShape(14.dp),
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = InputDarkCharcoal,
                unfocusedTextColor = InputDarkCharcoal,
                focusedBorderColor = DialogTitleDarkGreen,
                unfocusedBorderColor = BorderLightGreyGreen,
                focusedLabelColor = DialogTitleDarkGreen,
                unfocusedLabelColor = LabelGreyGreen,
                focusedPlaceholderColor = LabelGreyGreen.copy(alpha = 0.75f),
                unfocusedPlaceholderColor = LabelGreyGreen.copy(alpha = 0.75f),
                focusedContainerColor = DialogWhite,
                unfocusedContainerColor = DialogWhite,
                cursorColor = DialogTitleDarkGreen,
                errorBorderColor = AlertRose,
                errorLabelColor = AlertRose,
                errorTextColor = InputDarkCharcoal,
                errorContainerColor = DialogWhite
            )
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = AlertRose,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

// ============================================================================
// Unified Feeding Schedule Form Dialog (Add & Edit)
// ============================================================================

@Composable
private fun MealScheduleFormDialog(
    title: String,
    initialMealName: String = "",
    initialScheduledTime: String = "",
    initialFoodName: String = "",
    initialFoodQuantity: String = "",
    initialCaloriesText: String = "",
    initialInstructions: String = "",
    initialReminderEnabled: Boolean = true,
    confirmButtonText: String = "Save Changes",
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (
        mealName: String,
        scheduledTime: String,
        foodName: String,
        foodQuantity: String,
        instructions: String,
        calories: Int,
        reminderEnabled: Boolean
    ) -> Unit
) {
    val context = LocalContext.current

    var mealName by remember { mutableStateOf(initialMealName) }
    var scheduledTime by remember { mutableStateOf(initialScheduledTime) }
    var foodName by remember { mutableStateOf(initialFoodName) }
    var foodQuantity by remember { mutableStateOf(initialFoodQuantity) }
    var caloriesText by remember { mutableStateOf(initialCaloriesText) }
    var instructions by remember { mutableStateOf(initialInstructions) }
    var reminderEnabled by remember { mutableStateOf(initialReminderEnabled) }

    var mealNameError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* prevent click through */ }
                    ),
                shape = RoundedCornerShape(24.dp),
                color = DialogWhite,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, BorderLightGreyGreen)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Dialog Header
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DialogTitleDarkGreen
                        )
                        Text(
                            text = if (isEditing) "Edit feeding schedule details" else "Enter meal information and schedule time",
                            style = MaterialTheme.typography.bodySmall,
                            color = LabelGreyGreen
                        )
                    }

                    // 1. Meal Period Name
                    DietInputField(
                        value = mealName,
                        onValueChange = {
                            mealName = it
                            mealNameError = null
                        },
                        label = "Meal Period Name",
                        placeholder = "Enter meal name",
                        isError = mealNameError != null,
                        errorMessage = mealNameError
                    )

                    // 2. Scheduled Feeding Time (Time Picker)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Scheduled Feeding Time",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (timeError != null) AlertRose else LabelGreyGreen,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        OutlinedButton(
                            onClick = {
                                showTimePickerDialog(context, scheduledTime) { selectedTime ->
                                    scheduledTime = selectedTime
                                    timeError = null
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = DialogWhite,
                                contentColor = if (scheduledTime.isNotBlank()) InputDarkCharcoal else LabelGreyGreen
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (timeError != null) AlertRose else BorderLightGreyGreen
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (scheduledTime.isNotBlank()) scheduledTime else "Select feeding time",
                                    style = TextStyle(
                                        color = if (scheduledTime.isNotBlank()) InputDarkCharcoal else LabelGreyGreen.copy(alpha = 0.75f),
                                        fontSize = 15.sp,
                                        fontWeight = if (scheduledTime.isNotBlank()) FontWeight.Medium else FontWeight.Normal
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Pick Time",
                                    tint = if (scheduledTime.isNotBlank()) DialogTitleDarkGreen else LabelGreyGreen
                                )
                            }
                        }
                        if (timeError != null) {
                            Text(
                                text = timeError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = AlertRose,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }

                    // 3. Food Name
                    DietInputField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = "Food Name",
                        placeholder = "Enter food name"
                    )

                    // 4. Portion / Quantity
                    DietInputField(
                        value = foodQuantity,
                        onValueChange = { foodQuantity = it },
                        label = "Portion / Quantity",
                        placeholder = "Enter portion or quantity"
                    )

                    // 5. Estimated Calories
                    DietInputField(
                        value = caloriesText,
                        onValueChange = { caloriesText = it.filter { ch -> ch.isDigit() } },
                        label = "Estimated Calories (kcal)",
                        placeholder = "Leave empty to auto-calculate",
                        keyboardType = KeyboardType.Number
                    )
                    Text(
                        text = "Leave empty to automatically calculate calories from nutritional data",
                        style = MaterialTheme.typography.labelSmall,
                        color = LabelGreyGreen,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // 6. Feeding Notes / Instructions (Comfortable Multiline)
                    DietInputField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = "Feeding Notes / Instructions",
                        placeholder = "Enter your feeding instructions",
                        singleLine = false,
                        minLines = 3,
                        maxLines = 5
                    )

                    // 7. Daily Reminder Alert Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DialogCardBackground)
                            .border(1.dp, BorderLightGreyGreen, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Reminder Alert",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = DialogTitleDarkGreen
                            )
                            Text(
                                text = "Send clock-exact alarm notification",
                                style = MaterialTheme.typography.bodySmall,
                                color = LabelGreyGreen
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DialogTitleDarkGreen,
                                uncheckedThumbColor = LabelGreyGreen,
                                uncheckedTrackColor = BorderLightGreyGreen
                            )
                        )
                    }

                    // Action Buttons (Cancel & Save)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = DialogWhite,
                                contentColor = DialogTitleDarkGreen
                            ),
                            border = BorderStroke(1.5.dp, BorderLightGreyGreen)
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DialogTitleDarkGreen
                            )
                        }

                        Button(
                            onClick = {
                                var isValid = true
                                if (mealName.trim().isBlank()) {
                                    mealNameError = "Please enter a meal name"
                                    isValid = false
                                }
                                if (scheduledTime.trim().isBlank()) {
                                    timeError = "Please select a feeding time"
                                    isValid = false
                                }

                                if (isValid) {
                                    val caloriesInt = caloriesText.trim().toIntOrNull() ?: 0
                                    onSave(
                                        mealName.trim(),
                                        scheduledTime.trim(),
                                        foodName.trim(),
                                        foodQuantity.trim(),
                                        instructions.trim(),
                                        caloriesInt,
                                        reminderEnabled
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DialogTitleDarkGreen,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = confirmButtonText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealEditDialog(
    schedule: MealScheduleDto,
    onDismiss: () -> Unit,
    onSave: (
        mealName: String,
        scheduledTime: String,
        foodName: String,
        foodQuantity: String,
        instructions: String,
        calories: Int,
        reminderEnabled: Boolean
    ) -> Unit
) {
    MealScheduleFormDialog(
        title = "Edit ${schedule.mealName}",
        initialMealName = schedule.mealName,
        initialScheduledTime = schedule.scheduledTime,
        initialFoodName = schedule.foodName,
        initialFoodQuantity = schedule.foodQuantity,
        initialCaloriesText = if (schedule.calories > 0) schedule.calories.toString() else "",
        initialInstructions = schedule.instructions,
        initialReminderEnabled = schedule.isReminderEnabled == 1,
        confirmButtonText = "Save Changes",
        isEditing = true,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

@Composable
private fun MealAddDialog(
    onDismiss: () -> Unit,
    onSave: (
        mealName: String,
        scheduledTime: String,
        foodName: String,
        foodQuantity: String,
        instructions: String,
        calories: Int,
        reminderEnabled: Boolean
    ) -> Unit
) {
    MealScheduleFormDialog(
        title = "Add Meal Schedule",
        initialMealName = "",
        initialScheduledTime = "",
        initialFoodName = "",
        initialFoodQuantity = "",
        initialCaloriesText = "",
        initialInstructions = "",
        initialReminderEnabled = true,
        confirmButtonText = "Add Schedule",
        isEditing = false,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

// ============================================================================
// Helpers
// ============================================================================

private data class MealMeta(
    val icon: ImageVector,
    val accentColor: Color
)

private fun getMealMeta(mealName: String): MealMeta {
    val lower = mealName.lowercase()
    return when {
        lower.contains("breakfast") -> MealMeta(Icons.Default.WbSunny, AmberAccent)
        lower.contains("lunch") -> MealMeta(Icons.Default.Brightness5, MintSage)
        lower.contains("snack") || lower.contains("midday") || lower.contains("evening") ->
            MealMeta(Icons.Default.Fastfood, Color(0xFFFF8A65))
        lower.contains("dinner") || lower.contains("night") -> MealMeta(Icons.Default.Bedtime, LavenderAccent)
        else -> MealMeta(Icons.Default.Restaurant, EmeraldPrimary)
    }
}

private fun showTimePickerDialog(
    context: Context,
    currentTimeStr: String,
    onTimeSelected: (String) -> Unit
) {
    val cal = if (currentTimeStr.isNotBlank()) {
        MealNotificationScheduler.parseTimeToCalendar(currentTimeStr)
    } else {
        Calendar.getInstance()
    }
    val initialHour = cal.get(Calendar.HOUR_OF_DAY)
    val initialMinute = cal.get(Calendar.MINUTE)

    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val amPm = if (hourOfDay < 12) "AM" else "PM"
            val displayHour = when {
                hourOfDay == 0 -> 12
                hourOfDay > 12 -> hourOfDay - 12
                else -> hourOfDay
            }
            val formattedTime = String.format("%d:%02d %s", displayHour, minute, amPm)
            onTimeSelected(formattedTime)
        },
        initialHour,
        initialMinute,
        false
    ).show()
}
