package com.kavya.petcare

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kavya.petcare.data.models.GroomingReminderDto
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GroomingCalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                GroomingCalendarScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onNavigateToAddReminder = {
                        startActivity(Intent(this, AddReminderActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh live grooming reminders whenever returning to this screen
        PetViewModel.getShared(this).loadGroomingReminders()
    }
}

enum class GroomingFilterTab(val label: String) {
    ALL("All"),
    SCHEDULED("Scheduled"),
    COMPLETED("Completed")
}

private val CoralSoft = Color(0xFFFFEBEE)
private val CoralAlert = Color(0xFFE63946)

@Composable
fun GroomingCalendarScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onBack: () -> Unit,
    onNavigateToAddReminder: () -> Unit = {}
) {
    val context = LocalContext.current
    val activePet by viewModel.activePet.collectAsState()
    val allPets by viewModel.allPets.collectAsState()
    val reminders by viewModel.groomingReminders.collectAsState()
    val isLoading by viewModel.isLoadingGrooming.collectAsState()
    val errorMessage by viewModel.groomingError.collectAsState()

    var selectedTab by remember { mutableStateOf(GroomingFilterTab.SCHEDULED) }
    var reminderToEdit by remember { mutableStateOf<GroomingReminderDto?>(null) }
    var reminderToDelete by remember { mutableStateOf<GroomingReminderDto?>(null) }

    // Fetch on initial compose
    LaunchedEffect(Unit) {
        viewModel.loadGroomingReminders()
    }

    // Filter reminders based on tab
    val scheduledCount = reminders.count { !it.isCompleted }
    val completedCount = reminders.count { it.isCompleted }
    val totalCount = reminders.size

    val filteredReminders = remember(reminders, selectedTab) {
        when (selectedTab) {
            GroomingFilterTab.ALL -> reminders
            GroomingFilterTab.SCHEDULED -> reminders.filter { !it.isCompleted }
            GroomingFilterTab.COMPLETED -> reminders.filter { it.isCompleted }
        }
    }

    Scaffold(
        topBar = {
            PetModernTopBar(
                title = "Grooming Calendar",
                subtitle = "Care schedule for ${activePet.name}",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onNavigateToAddReminder) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Add Care Reminder",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(26.dp)
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Filter Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarmPearl, RoundedCornerShape(16.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GroomingFilterTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val count = when (tab) {
                        GroomingFilterTab.ALL -> totalCount
                        GroomingFilterTab.SCHEDULED -> scheduledCount
                        GroomingFilterTab.COMPLETED -> completedCount
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTab = tab },
                        color = if (isSelected) EmeraldPrimary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${tab.label} ($count)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) WarmPearl else TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            // Error banner if any
            if (errorMessage != null && reminders.isEmpty()) {
                Surface(
                    color = CoralSoft,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = CoralAlert,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = CoralAlert,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.loadGroomingReminders() }) {
                            Text("Retry", color = CoralAlert, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PetSectionHeader(
                    title = when (selectedTab) {
                        GroomingFilterTab.ALL -> "All Care Reminders ($totalCount)"
                        GroomingFilterTab.SCHEDULED -> "Scheduled Routines ($scheduledCount)"
                        GroomingFilterTab.COMPLETED -> "Completed Care ($completedCount)"
                    }
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = EmeraldPrimary
                    )
                }
            }

            // List of dynamic database records OR empty state
            if (filteredReminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    GroomingEmptyState(
                        tab = selectedTab,
                        onAddReminder = onNavigateToAddReminder
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(
                        items = filteredReminders,
                        key = { it.id }
                    ) { reminder ->
                        GroomingReminderCard(
                            reminder = reminder,
                            onToggleStatus = {
                                val targetStatus = if (reminder.isCompleted) "Scheduled" else "Completed"
                                viewModel.updateGroomingStatus(reminder.id, targetStatus) { success, _ ->
                                    if (success) {
                                        val toastMsg = if (targetStatus == "Completed") {
                                            "Marked '${reminder.activity}' as Completed! 🎉"
                                        } else {
                                            "Moved '${reminder.activity}' back to Scheduled."
                                        }
                                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onEdit = { reminderToEdit = reminder },
                            onDelete = { reminderToDelete = reminder }
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                ) {
                    Text("Back", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onNavigateToAddReminder,
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = WarmPearl
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Reminder", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Edit Reminder Dialog
    if (reminderToEdit != null) {
        EditGroomingReminderDialog(
            reminder = reminderToEdit!!,
            allPets = allPets,
            onDismiss = { reminderToEdit = null },
            onSave = { petId, activity, date, time, notifEnabled ->
                viewModel.updateGroomingReminder(
                    id = reminderToEdit!!.id,
                    petId = petId,
                    activity = activity,
                    reminderDate = date,
                    reminderTime = time,
                    notificationEnabled = notifEnabled
                ) { success, err ->
                    if (success) {
                        Toast.makeText(context, "Reminder updated successfully!", Toast.LENGTH_SHORT).show()
                        reminderToEdit = null
                    } else {
                        Toast.makeText(context, err ?: "Failed to update reminder", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (reminderToDelete != null) {
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            title = {
                Text(
                    text = "Delete Care Reminder?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete the scheduled routine '${reminderToDelete!!.activity}' for ${reminderToDelete!!.petName}? This will permanently remove the record from the database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = reminderToDelete!!
                        reminderToDelete = null
                        viewModel.deleteGroomingReminder(toDelete.id) { success, _ ->
                            if (success) {
                                Toast.makeText(context, "Reminder deleted.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralAlert,
                        contentColor = WarmPearl
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { reminderToDelete = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = WarmPearl,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

/**
 * Single dynamic grooming reminder card
 */
@Composable
private fun GroomingReminderCard(
    reminder: GroomingReminderDto,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = reminder.isCompleted

    PetModernCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (isCompleted) MintSoft.copy(alpha = 0.45f) else WarmPearl
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Icon + Activity + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isCompleted) MintSage.copy(alpha = 0.2f) else MintSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.ContentCut,
                        contentDescription = null,
                        tint = if (isCompleted) MintSage else EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = reminder.activity,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) TextSecondary else TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = MintSage,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${reminder.petName} • ${reminder.petSpecies}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MintSage
                        )
                    }
                }

                PetBadge(
                    text = if (isCompleted) "Completed" else "Scheduled",
                    containerColor = if (isCompleted) MintSoft else EmeraldPrimary.copy(alpha = 0.12f),
                    contentColor = if (isCompleted) TextSecondary else EmeraldPrimary
                )
            }

            // Middle Row: Date, Time & Notification indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftBackground, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${formatDisplayDate(reminder.reminderDate)} • ${reminder.reminderTime}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (reminder.notificationEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (reminder.notificationEnabled) EmeraldPrimary else TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (reminder.notificationEnabled) "Alert on" else "Alert off",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reminder.notificationEnabled) EmeraldPrimary else TextSecondary
                    )
                }
            }

            // Bottom Action Row: Mark Completed/Scheduled, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status action button
                if (!isCompleted) {
                    FilledTonalButton(
                        onClick = onToggleStatus,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MintSoft,
                            contentColor = EmeraldPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mark as Completed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onToggleStatus,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mark as Scheduled",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Edit and Delete Icon Actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Reminder",
                            tint = MintSage,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Reminder",
                            tint = CoralAlert,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Required empty state display
 */
@Composable
private fun GroomingEmptyState(
    tab: GroomingFilterTab,
    onAddReminder: () -> Unit
) {
    PetModernCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = WarmPearl
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MintSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "No grooming reminders yet.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Add your first care reminder to keep your pet healthy and happy.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onAddReminder,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = WarmPearl
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Reminder",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Edit Reminder Modal Dialog
 */
@Composable
private fun EditGroomingReminderDialog(
    reminder: GroomingReminderDto,
    allPets: List<com.kavya.petcare.data.models.PetDto>,
    onDismiss: () -> Unit,
    onSave: (petId: Int, activity: String, reminderDate: String, reminderTime: String, notifEnabled: Boolean) -> Unit
) {
    val context = LocalContext.current
    var editActivity by remember { mutableStateOf(reminder.activity) }
    var editDate by remember { mutableStateOf(reminder.reminderDate) }
    var editTime by remember { mutableStateOf(reminder.reminderTime) }
    var editPetId by remember { mutableStateOf(reminder.petId) }
    var editNotification by remember { mutableStateOf(reminder.notificationEnabled) }

    val cal = Calendar.getInstance()

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                editDate = sdf.format(chosen.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

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
                editTime = String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = WarmPearl,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Care Reminder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Pet selection (if multiple pets exist)
                if (allPets.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Pet Profile",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allPets) { petItem ->
                                val isSelected = petItem.id == editPetId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { editPetId = petItem.id },
                                    label = { Text(petItem.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = WarmPearl
                                    )
                                )
                            }
                        }
                    }
                }

                // Activity field
                PetTextField(
                    value = editActivity,
                    onValueChange = { editActivity = it },
                    label = "Grooming Activity",
                    placeholder = "e.g., Bathing, Nail Trim",
                    leadingIcon = Icons.Default.ContentCut
                )

                // Date Picker trigger
                Column {
                    Text(
                        text = "Date",
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
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (editDate.isNotBlank()) editDate else "Select Date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Time Picker trigger
                Column {
                    Text(
                        text = "Time",
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
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (editTime.isNotBlank()) editTime else "Select Time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Notification toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SoftBackground, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Push Notification",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Switch(
                        checked = editNotification,
                        onCheckedChange = { editNotification = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldPrimary,
                            checkedTrackColor = MintSage.copy(alpha = 0.35f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (editActivity.isNotBlank() && editDate.isNotBlank() && editTime.isNotBlank()) {
                                onSave(editPetId, editActivity.trim(), editDate.trim(), editTime.trim(), editNotification)
                            } else {
                                Toast.makeText(context, "Please complete all fields", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = WarmPearl
                        )
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Formats a stored date into a readable format "MMM dd, yyyy"
 */
private fun formatDisplayDate(dateStr: String): String {
    return try {
        val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = sdfIn.parse(dateStr)
        if (parsed != null) {
            val sdfOut = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            sdfOut.format(parsed)
        } else {
            dateStr
        }
    } catch (_: Exception) {
        dateStr
    }
}
