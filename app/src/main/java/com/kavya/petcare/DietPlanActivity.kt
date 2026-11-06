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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.kavya.petcare.data.models.MealScheduleDto
import com.kavya.petcare.notifications.MealNotificationScheduler
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme
import java.util.Calendar

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

    LaunchedEffect(Unit) {
        viewModel.loadMealSchedules()
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
            val targetCalories = dietSummary?.targetCalories ?: 800
            val consumedCalories = dietSummary?.consumedCalories ?: 650
            val progressRatio = (consumedCalories.toFloat() / targetCalories.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
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
                                text = "Target: $targetCalories kcal / day",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                            Text(
                                text = "Divided into $mealCount balanced portions throughout the day",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        PetBadge(
                            text = "${(progressRatio * 100).toInt()}% Met",
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
                        val remaining = (targetCalories - consumedCalories).coerceAtLeast(0)
                        Text(
                            text = "$remaining kcal remaining",
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
                            text = "No feeding schedules configured yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PetGradientButton(
                            text = "Load Default Schedules",
                            onClick = { viewModel.loadMealSchedules() }
                        )
                    }
                }
            } else {
                sortedSchedules.forEach { schedule ->
                    FeedingMealCard(
                        schedule = schedule,
                        petName = activePet.name,
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
    petName: String,
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
                    PetBadge(
                        text = "${schedule.calories} kcal",
                        containerColor = SoftBackground,
                        contentColor = EmeraldPrimary
                    )

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
                } else {
                    append("Standard Pet Meal")
                }
            }

            Text(
                text = foodDisplay,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            // Feeding Instructions / Notes
            val instructions = if (schedule.instructions.isNotBlank()) {
                schedule.instructions
            } else if (schedule.mealDescription.isNotBlank() && schedule.mealDescription != schedule.foodName) {
                schedule.mealDescription
            } else {
                "Serve fresh to $petName with clean, room-temperature drinking water."
            }

            Text(
                text = instructions,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )

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
// Input Field Helper
// ============================================================================

@Composable
private fun DietInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = if (placeholder != null) { { Text(placeholder, color = TextMuted) } } else null,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            ),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = MintSage,
                unfocusedBorderColor = BorderSubtle,
                focusedLabelColor = EmeraldDark,
                unfocusedLabelColor = TextSecondary,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
    }
}

// ============================================================================
// Dialogs: Edit Meal & Add Meal
// ============================================================================

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
    val context = LocalContext.current

    var mealName by remember { mutableStateOf(schedule.mealName) }
    var scheduledTime by remember { mutableStateOf(schedule.scheduledTime) }
    var foodName by remember { mutableStateOf(schedule.foodName.ifBlank { schedule.mealDescription }) }
    var foodQuantity by remember { mutableStateOf(schedule.foodQuantity) }
    var caloriesText by remember { mutableStateOf(schedule.calories.toString()) }
    var instructions by remember { mutableStateOf(schedule.instructions) }
    var reminderEnabled by remember { mutableStateOf(schedule.isReminderEnabled == 1) }

    Dialog(onDismissRequest = onDismiss) {
        PetModernCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            containerColor = WarmPearl
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Edit ${schedule.mealName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )

                // Meal Name
                DietInputField(
                    value = mealName,
                    onValueChange = { mealName = it },
                    label = "Meal Period Name",
                    placeholder = "e.g. Breakfast, Lunch, Dinner"
                )

                // Scheduled Time with TimePicker Button
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Scheduled Feeding Time",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    OutlinedButton(
                        onClick = {
                            showTimePickerDialog(context, scheduledTime) { selectedTime ->
                                scheduledTime = selectedTime
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = scheduledTime,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldDark
                            )
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Pick Time",
                                tint = MintSage
                            )
                        }
                    }
                }

                // Food Name
                DietInputField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = "Food Name",
                    placeholder = "e.g. Premium Kibble, Timothy Hay"
                )

                // Food Quantity
                DietInputField(
                    value = foodQuantity,
                    onValueChange = { foodQuantity = it },
                    label = "Portion / Quantity",
                    placeholder = "e.g. 2 Cups, 1 Bowl, 100g"
                )

                // Calories
                DietInputField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it.filter { ch -> ch.isDigit() } },
                    label = "Estimated Calories (kcal)",
                    placeholder = "350",
                    keyboardType = KeyboardType.Number
                )

                // Instructions
                DietInputField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = "Feeding Notes / Instructions",
                    placeholder = "e.g. Serve with fresh water at room temperature",
                    singleLine = false
                )

                // Reminder Switch Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoftBackground)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Reminder Alert",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Send clock-exact alarm notification",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MintSage
                        )
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val caloriesInt = caloriesText.toIntOrNull() ?: 250
                            onSave(
                                mealName.trim().ifBlank { "Meal" },
                                scheduledTime.trim().ifBlank { "8:00 AM" },
                                foodName.trim(),
                                foodQuantity.trim(),
                                instructions.trim(),
                                caloriesInt,
                                reminderEnabled
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
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
    val context = LocalContext.current

    var mealName by remember { mutableStateOf("") }
    var scheduledTime by remember { mutableStateOf("4:30 PM") }
    var foodName by remember { mutableStateOf("") }
    var foodQuantity by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("150") }
    var instructions by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        PetModernCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            containerColor = WarmPearl
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Add Meal Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )

                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Breakfast", "Lunch", "Evening Snacks", "Dinner").forEach { preset ->
                        FilterChip(
                            selected = mealName.equals(preset, ignoreCase = true),
                            onClick = {
                                mealName = preset
                                when (preset) {
                                    "Breakfast" -> { scheduledTime = "8:00 AM"; caloriesText = "350" }
                                    "Lunch" -> { scheduledTime = "1:00 PM"; caloriesText = "200" }
                                    "Evening Snacks" -> { scheduledTime = "4:30 PM"; caloriesText = "100" }
                                    "Dinner" -> { scheduledTime = "7:00 PM"; caloriesText = "350" }
                                }
                            },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                DietInputField(
                    value = mealName,
                    onValueChange = { mealName = it },
                    label = "Meal Name",
                    placeholder = "e.g. Evening Snacks"
                )

                // Scheduled Time
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Scheduled Feeding Time",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    OutlinedButton(
                        onClick = {
                            showTimePickerDialog(context, scheduledTime) { selectedTime ->
                                scheduledTime = selectedTime
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = scheduledTime,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldDark
                            )
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Pick Time",
                                tint = MintSage
                            )
                        }
                    }
                }

                DietInputField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = "Food Name",
                    placeholder = "e.g. Organic Crunch Biscuits"
                )

                DietInputField(
                    value = foodQuantity,
                    onValueChange = { foodQuantity = it },
                    label = "Portion / Quantity",
                    placeholder = "e.g. 50g or 1 Bowl"
                )

                DietInputField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it.filter { ch -> ch.isDigit() } },
                    label = "Estimated Calories (kcal)",
                    placeholder = "150",
                    keyboardType = KeyboardType.Number
                )

                DietInputField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = "Feeding Notes",
                    placeholder = "e.g. Reward after afternoon play session",
                    singleLine = false
                )

                // Reminder switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoftBackground)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Reminder Alert",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Set daily notification alarm",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MintSage
                        )
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val name = mealName.trim().ifBlank { "Snack" }
                            val caloriesInt = caloriesText.toIntOrNull() ?: 150
                            onSave(
                                name,
                                scheduledTime.trim().ifBlank { "4:30 PM" },
                                foodName.trim(),
                                foodQuantity.trim(),
                                instructions.trim(),
                                caloriesInt,
                                reminderEnabled
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Add Schedule", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
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
    val cal = MealNotificationScheduler.parseTimeToCalendar(currentTimeStr)
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
