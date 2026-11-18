package com.kavya.petcare

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MonitorWeight
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
import com.kavya.petcare.data.models.WeightEntryDto
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    val context = LocalContext.current
    val activePet by viewModel.activePet.collectAsState()
    val weightOverview by viewModel.weightOverview.collectAsState()
    val weightEntries by viewModel.weightEntries.collectAsState()

    var weightInput by remember { mutableStateOf("") }
    var weightError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var editingEntry by remember { mutableStateOf<WeightEntryDto?>(null) }
    var deletingEntry by remember { mutableStateOf<WeightEntryDto?>(null) }
    var showTargetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(activePet.id) {
        viewModel.loadWeightHistory()
    }

    val latestRecordedWeight: Double? = weightEntries.firstOrNull()?.weightKg ?: weightOverview?.currentWeight
    val targetWeight: Double? = weightOverview?.targetWeight
    val statusText: String = remember(latestRecordedWeight, targetWeight) {
        when {
            latestRecordedWeight == null -> "No Records"
            targetWeight == null -> "Tracked"
            else -> {
                val diffRatio = (latestRecordedWeight - targetWeight) / targetWeight.coerceAtLeast(0.01)
                when {
                    kotlin.math.abs(diffRatio) <= 0.05 -> "On Target"
                    diffRatio > 0.05 -> "Above Target"
                    else -> "Below Target"
                }
            }
        }
    }

    val statusBgColor = when (statusText) {
        "On Target", "Tracked" -> MintSoft
        "Above Target", "Below Target" -> AmberSoft
        else -> SoftBackground
    }
    val statusFgColor = when (statusText) {
        "On Target", "Tracked" -> EmeraldPrimary
        "Above Target", "Below Target" -> AmberAccent
        else -> TextSecondary
    }

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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${activePet.name}'s Weight",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable { showTargetDialog = true }
                            ) {
                                Text(
                                    text = if (targetWeight != null) {
                                        "Target: ${formatWeightValue(targetWeight)} kg"
                                    } else {
                                        "Target: Not set (tap to set)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (targetWeight != null) TextSecondary else MintSage,
                                    fontWeight = if (targetWeight != null) FontWeight.Normal else FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit Target Weight",
                                    tint = MintSage,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        PetBadge(
                            text = statusText,
                            containerColor = statusBgColor,
                            contentColor = statusFgColor
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (latestRecordedWeight != null) {
                                    formatWeightValue(latestRecordedWeight)
                                } else {
                                    "--"
                                },
                                fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (latestRecordedWeight != null) EmeraldPrimary else TextMuted
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

                    if (latestRecordedWeight != null && weightEntries.isNotEmpty()) {
                        Text(
                            text = "Latest recorded on ${weightEntries.first().recordedDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
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
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { raw ->
                            val filtered = filterDecimalInput(raw)
                            weightInput = filtered
                            weightError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Weight in kg") },
                        placeholder = { Text("Enter weight (e.g., 2.15)", color = TextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                        },
                        singleLine = true,
                        isError = weightError != null,
                        supportingText = if (weightError != null) {
                            { Text(weightError!!, color = AlertRose) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

                    PetGradientButton(
                        text = "Record Weight",
                        enabled = weightInput.isNotBlank() && !isSubmitting,
                        isLoading = isSubmitting,
                        onClick = {
                            val parsed = weightInput.trim().toFloatOrNull()
                            when {
                                parsed == null -> {
                                    weightError = "Please enter a valid number."
                                }
                                parsed <= 0f -> {
                                    weightError = "Weight must be greater than 0 kg."
                                }
                                parsed > 500f -> {
                                    weightError = "Please enter a realistic weight (under 500 kg)."
                                }
                                else -> {
                                    weightError = null
                                    isSubmitting = true
                                    val todayDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
                                    viewModel.addWeightEntry(
                                        date = todayDate,
                                        weightKg = parsed
                                    ) { success, message ->
                                        isSubmitting = false
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        if (success) {
                                            weightInput = ""
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            PetSectionHeader(title = "Recent History")

            if (weightEntries.isEmpty()) {
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
                                imageVector = Icons.Default.MonitorWeight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Text(
                            text = "No weight records yet. Record your first weight to track progress.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                weightEntries.forEach { entry ->
                    WeightHistoryCard(
                        entry = entry,
                        onEdit = { editingEntry = entry },
                        onDelete = { deletingEntry = entry }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    editingEntry?.let { entry ->
        EditWeightDialog(
            entry = entry,
            onDismiss = { editingEntry = null },
            onSave = { newWeight, newDate ->
                viewModel.updateWeightEntry(
                    weightId = entry.id,
                    weightKg = newWeight,
                    date = newDate,
                    notes = entry.notes
                ) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    if (success) {
                        editingEntry = null
                    }
                }
            }
        )
    }

    deletingEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { deletingEntry = null },
            title = {
                Text(
                    text = "Delete Weight Record?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Remove ${formatWeightValue(entry.weightKg)} kg recorded on ${entry.recordedDate}? This cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWeightEntry(entry.id) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (success) {
                                deletingEntry = null
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AlertRose)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEntry = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = WarmPearl,
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (showTargetDialog) {
        TargetWeightDialog(
            currentTarget = targetWeight,
            onDismiss = { showTargetDialog = false },
            onSave = { newTarget ->
                viewModel.updateTargetWeight(newTarget) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    if (success) {
                        showTargetDialog = false
                    }
                }
            }
        )
    }
}

@Composable
private fun WeightHistoryCard(
    entry: WeightEntryDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    PetModernCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = WarmPearl
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SoftBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = MintSage,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = entry.recordedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (!entry.notes.isNullOrBlank()) {
                        Text(
                            text = entry.notes,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PetBadge(
                    text = "${formatWeightValue(entry.weightKg)} kg",
                    containerColor = MintSoft,
                    contentColor = EmeraldPrimary
                )

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit weight",
                        tint = MintSage,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete weight",
                        tint = AlertRose.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditWeightDialog(
    entry: WeightEntryDto,
    onDismiss: () -> Unit,
    onSave: (weightKg: Float, date: String) -> Unit
) {
    val context = LocalContext.current
    var weightText by remember { mutableStateOf(formatWeightValue(entry.weightKg)) }
    var dateText by remember { mutableStateOf(entry.recordedDate) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

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
                    .widthIn(max = 460.dp)
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
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Edit Weight Record",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF123F32)
                    )

                    OutlinedTextField(
                        value = weightText,
                        onValueChange = {
                            weightText = filterDecimalInput(it)
                            errorMsg = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        isError = errorMsg != null,
                        supportingText = if (errorMsg != null) {
                            { Text(errorMsg!!, color = AlertRose) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(color = Color(0xFF1C2925), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C2925),
                            unfocusedTextColor = Color(0xFF1C2925),
                            focusedBorderColor = Color(0xFF123F32),
                            unfocusedBorderColor = Color(0xFFDFE8E3),
                            focusedLabelColor = Color(0xFF123F32),
                            unfocusedLabelColor = Color(0xFF53645D),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    OutlinedButton(
                        onClick = {
                            showWeightDatePicker(context) { picked ->
                                dateText = picked
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFDFE8E3))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateText,
                                color = Color(0xFF1C2925),
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Date",
                                tint = Color(0xFF123F32),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
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
                                val parsed = weightText.trim().toFloatOrNull()
                                if (parsed == null || parsed <= 0f || parsed > 500f) {
                                    errorMsg = "Enter a valid positive weight (0.01 - 500 kg)."
                                } else {
                                    onSave(parsed, dateText.ifBlank { entry.recordedDate })
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF123F32))
                        ) {
                            Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetWeightDialog(
    currentTarget: Double?,
    onDismiss: () -> Unit,
    onSave: (Double?) -> Unit
) {
    var targetInput by remember { mutableStateOf(currentTarget?.let { formatWeightValue(it) } ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Set Target Weight",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF123F32)
                )
                Text(
                    text = "Target weight is tracked independently from your recorded weights.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF53645D)
                )

                OutlinedTextField(
                    value = targetInput,
                    onValueChange = {
                        targetInput = filterDecimalInput(it)
                        errorMsg = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Target Weight (kg)") },
                    placeholder = { Text("Leave blank to clear", color = Color(0xFF53645D)) },
                    singleLine = true,
                    isError = errorMsg != null,
                    supportingText = if (errorMsg != null) {
                        { Text(errorMsg!!, color = AlertRose) }
                    } else null,
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
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
                            val trimmed = targetInput.trim()
                            if (trimmed.isEmpty()) {
                                onSave(null)
                            } else {
                                val parsed = trimmed.toDoubleOrNull()
                                if (parsed == null || parsed <= 0.0 || parsed > 500.0) {
                                    errorMsg = "Enter a valid positive weight."
                                } else {
                                    onSave(parsed)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF123F32))
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun filterDecimalInput(raw: String): String {
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

private fun formatWeightValue(weight: Double): String {
    return if (weight % 1.0 == 0.0) {
        String.format(Locale.US, "%.1f", weight)
    } else {
        String.format(Locale.US, "%.2f", weight).trimEnd('0').trimEnd('.')
    }
}

private fun showWeightDatePicker(context: Context, onDatePicked: (String) -> Unit) {
    val cal = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val picked = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            val formatted = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(picked.time)
            onDatePicked(formatted)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
