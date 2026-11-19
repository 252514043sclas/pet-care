package com.kavya.petcare

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                AddReminderScreen(
                    viewModel = viewModel,
                    onReminderSaved = { finish() },
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun AddReminderScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onReminderSaved: () -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activePet by viewModel.activePet.collectAsState()
    val allPets by viewModel.allPets.collectAsState()

    // Activity choices
    val standardActivities = listOf(
        "Bathing",
        "Brushing",
        "Nail Trimming",
        "Ear Cleaning",
        "Dental Care",
        "Haircut"
    )

    var reminderActivity by remember { mutableStateOf("") }
    var reminderDate by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf("") }
    var selectedPetId by remember {
        mutableStateOf(activePet.id.toIntOrNull()?.takeIf { it > 0 } ?: 1)
    }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Sync selected pet id if activePet changes initially
    LaunchedEffect(activePet) {
        val activeId = activePet.id.toIntOrNull()
        if (activeId != null && activeId > 0 && selectedPetId == 1 && allPets.any { it.id == activeId }) {
            selectedPetId = activeId
        }
    }

    // Permission launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission is needed for reminders", Toast.LENGTH_SHORT).show()
        }
    }

    // Calendar for dialogs
    val cal = Calendar.getInstance()

    // Native DatePickerDialog
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                reminderDate = sdf.format(chosen.time)
                validationError = null
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }
    }

    // Native TimePickerDialog
    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val amPm = if (hourOfDay >= 12) "PM" else "AM"
                val hour12 = when {
                    hourOfDay == 0 -> 12
                    hourOfDay > 12 -> hourOfDay - 12
                    else -> hourOfDay
                }
                reminderTime = String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
                validationError = null
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        )
    }

    Scaffold(
        topBar = {
            PetModernTopBar(
                title = "New Care Reminder",
                subtitle = "Set scheduled grooming alert",
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Validation error alert if any
            if (validationError != null) {
                Surface(
                    color = AlertRoseSoft,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AlertRose,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = validationError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = AlertRose,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 1. Select Pet Card
            PetModernCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = WarmPearl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Select Pet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        PetBadge(
                            text = "Required",
                            containerColor = MintSoft,
                            contentColor = EmeraldPrimary
                        )
                    }

                    if (allPets.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allPets) { petItem ->
                                val isSelected = petItem.id == selectedPetId
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) EmeraldPrimary else SoftBackground,
                                    modifier = Modifier
                                        .clickable { selectedPetId = petItem.id }
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) EmeraldPrimary else BorderSubtle,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Pets,
                                            contentDescription = null,
                                            tint = if (isSelected) WarmPearl else EmeraldPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = petItem.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) WarmPearl else TextPrimary
                                            )
                                            Text(
                                                text = petItem.species,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) WarmPearl.copy(alpha = 0.85f) else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Fallback single pet display
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MintSoft, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Pets, contentDescription = null, tint = EmeraldPrimary)
                            Text(
                                text = "${activePet.name} (${activePet.type})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // 2. Activity Selection Card
            PetModernCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = WarmPearl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "2. Grooming Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    // Quick activity chips
                    Text(
                        text = "Quick Select:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(standardActivities) { act ->
                            val isSelected = reminderActivity.equals(act, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    reminderActivity = act
                                    validationError = null
                                },
                                label = { Text(act) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = WarmPearl,
                                    containerColor = SoftBackground,
                                    labelColor = TextPrimary
                                )
                            )
                        }
                    }

                    PetTextField(
                        value = reminderActivity,
                        onValueChange = {
                            reminderActivity = it
                            validationError = null
                        },
                        label = "Activity / Task Name",
                        placeholder = "e.g., Bathing, Fur De-shedding, Teeth Care",
                        leadingIcon = Icons.Default.ContentCut
                    )
                }
            }

            // 3. Date & Time Selection Card
            PetModernCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = WarmPearl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "3. Reminder Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    // Date Picker Trigger Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Reminder Date",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() }
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (reminderDate.isNotBlank()) reminderDate else "Select reminder date",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (reminderDate.isNotBlank()) TextPrimary else TextSecondary,
                                        fontWeight = if (reminderDate.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = null,
                                    tint = MintSage,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Time Picker Trigger Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Reminder Time",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { timePickerDialog.show() }
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (reminderTime.isNotBlank()) reminderTime else "Select reminder time",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (reminderTime.isNotBlank()) TextPrimary else TextSecondary,
                                        fontWeight = if (reminderTime.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MintSage,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Notification toggle card
            PetModernCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = WarmPearl
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(MintSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Push Notification Alert",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Ring exact device alarm when due",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { isChecked ->
                            notificationsEnabled = isChecked
                            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldPrimary,
                            checkedTrackColor = MintSage.copy(alpha = 0.35f),
                            uncheckedThumbColor = WarmPearl,
                            uncheckedTrackColor = BorderSubtle
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 5. Save Reminder Button
            PetGradientButton(
                text = "Save Reminder",
                enabled = !isSubmitting,
                isLoading = isSubmitting,
                onClick = {
                    // Validation
                    if (reminderActivity.isBlank()) {
                        validationError = "Please enter or select a grooming activity name."
                        return@PetGradientButton
                    }
                    if (reminderDate.isBlank()) {
                        validationError = "Please select a reminder date."
                        return@PetGradientButton
                    }
                    if (reminderTime.isBlank()) {
                        validationError = "Please select a reminder time."
                        return@PetGradientButton
                    }
                    if (selectedPetId <= 0) {
                        validationError = "Please select a pet profile for this reminder."
                        return@PetGradientButton
                    }

                    validationError = null
                    isSubmitting = true

                    viewModel.createGroomingReminder(
                        petId = selectedPetId,
                        activity = reminderActivity.trim(),
                        reminderDate = reminderDate.trim(),
                        reminderTime = reminderTime.trim(),
                        notificationEnabled = notificationsEnabled,
                        status = "Scheduled"
                    ) { success, errMsg ->
                        isSubmitting = false
                        if (success) {
                            Toast.makeText(context, "Care reminder saved successfully!", Toast.LENGTH_SHORT).show()
                            onReminderSaved()
                        } else {
                            validationError = errMsg ?: "Failed to save reminder to server"
                            Toast.makeText(context, validationError, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
