package com.kavya.petcare

import android.app.DatePickerDialog
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
import com.kavya.petcare.data.models.CreateHealthRecordRequest
import com.kavya.petcare.data.models.PetHealthRecordDto
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HealthScoreDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                HealthScoreDetailScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun HealthScoreDetailScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activePet by viewModel.activePet.collectAsState()
    val healthOverview by viewModel.healthScoreOverview.collectAsState()
    val isLoading by viewModel.isLoadingHealthScore.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<PetHealthRecordDto?>(null) }
    var recordToDelete by remember { mutableStateOf<PetHealthRecordDto?>(null) }

    LaunchedEffect(activePet.id) {
        val petId = activePet.id.toIntOrNull()
        if (petId != null && petId > 0) {
            viewModel.loadHealthScore(petId)
        }
    }

    val petId = activePet.id.toIntOrNull() ?: 1
    val hasData = healthOverview?.hasSufficientData == true && healthOverview?.score != null
    val score = healthOverview?.score
    val statusLabel = healthOverview?.statusLabel ?: if (hasData) "Good" else "Not enough health data"
    val breakdown = healthOverview?.breakdown ?: emptyList()
    val records = healthOverview?.healthRecords ?: emptyList()

    Scaffold(
        topBar = {
            PetModernTopBar(
                title = "Health Score",
                subtitle = "Wellness assessment for ${activePet.name}",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Add Health Record",
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Score Card
            PetModernCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = WarmPearl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (hasData && score != null) {
                        // Score Circle
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(
                                    if (score >= 80) MintSoft else if (score >= 60) AmberSoft else AlertRoseSoft
                                )
                                .border(
                                    width = 3.dp,
                                    color = if (score >= 80) EmeraldPrimary else if (score >= 60) AmberAccent else AlertRose,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$score",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (score >= 80) EmeraldDark else if (score >= 60) AmberAccent else AlertRose
                                )
                                Text(
                                    text = "OUT OF 100",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Status Badge
                        PetBadge(
                            text = statusLabel,
                            containerColor = if (score >= 80) MintSoft else if (score >= 60) AmberSoft else AlertRoseSoft,
                            contentColor = if (score >= 80) EmeraldPrimary else if (score >= 60) AmberAccent else AlertRose
                        )

                        Text(
                            text = "Informational wellness indicator calculated from logged checkups, symptoms, weight stability, and immunization records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    } else {
                        // Empty / Insufficient data state
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(AlertRoseSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = AlertRose,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Text(
                            text = "Not Enough Health Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "We cannot determine an accurate wellness score yet. Log veterinary checkups, recorded symptoms, or weight entries to calculate ${activePet.name}'s score.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = WarmPearl
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add First Health Record", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Score Calculation Factors Breakdown (if data exists)
            if (breakdown.isNotEmpty()) {
                PetSectionHeader(title = "Scoring Breakdown Factors")

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
                        breakdown.forEachIndexed { index, factor ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (factor.impact < 0) AlertRoseSoft else MintSoft
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (factor.impact < 0) Icons.Default.Remove else Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (factor.impact < 0) AlertRose else EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = factor.factor,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = factor.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                if (factor.impact != 0) {
                                    Text(
                                        text = if (factor.impact > 0) "+${factor.impact}" else "${factor.impact}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (factor.impact > 0) EmeraldPrimary else AlertRose
                                    )
                                }
                            }

                            if (index < breakdown.size - 1) {
                                HorizontalDivider(color = BorderSubtle)
                            }
                        }
                    }
                }
            }

            // 3. Health History & Checkup Records
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PetSectionHeader(title = "Health & Checkup History (${records.size})")

                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Record", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            }

            if (records.isEmpty()) {
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
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = MintSage,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No health records recorded yet.",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Keep a history of checkups, illnesses, and vet notes to maintain ${activePet.name}'s clinical profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    records.forEach { record ->
                        HealthRecordCard(
                            record = record,
                            onEdit = { recordToEdit = record },
                            onDelete = { recordToDelete = record }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Add Record Dialog
    if (showAddDialog) {
        AddEditHealthRecordDialog(
            petName = activePet.name,
            initialRecord = null,
            onDismiss = { showAddDialog = false },
            onSave = { request ->
                viewModel.addHealthRecord(petId, request) { success, err ->
                    if (success) {
                        Toast.makeText(context, "Health record added!", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    } else {
                        Toast.makeText(context, err ?: "Failed to add record", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Edit Record Dialog
    if (recordToEdit != null) {
        AddEditHealthRecordDialog(
            petName = activePet.name,
            initialRecord = recordToEdit,
            onDismiss = { recordToEdit = null },
            onSave = { request ->
                viewModel.updateHealthRecord(petId, recordToEdit!!.id, request) { success, err ->
                    if (success) {
                        Toast.makeText(context, "Health record updated!", Toast.LENGTH_SHORT).show()
                        recordToEdit = null
                    } else {
                        Toast.makeText(context, err ?: "Failed to update record", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete Health Record?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Are you sure you want to remove '${recordToDelete!!.title}'? Your pet's health score will be automatically recalculated.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        val id = recordToDelete!!.id
                        recordToDelete = null
                        viewModel.deleteHealthRecord(petId, id) { success, _ ->
                            if (success) {
                                Toast.makeText(context, "Record deleted.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRose)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { recordToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun HealthRecordCard(
    record: PetHealthRecordDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sevColor = when (record.severity.lowercase()) {
        "severe" -> AlertRose
        "moderate" -> AmberAccent
        "mild" -> SkyBlue
        else -> MintSage
    }
    val sevBg = when (record.severity.lowercase()) {
        "severe" -> AlertRoseSoft
        "moderate" -> AmberSoft
        "mild" -> SkySoft
        else -> MintSoft
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    PetBadge(
                        text = record.recordType.uppercase(),
                        containerColor = MintSoft,
                        contentColor = EmeraldPrimary
                    )

                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                PetBadge(
                    text = record.severity,
                    containerColor = sevBg,
                    contentColor = sevColor
                )
            }

            if (!record.description.isNullOrBlank()) {
                Text(
                    text = record.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            HorizontalDivider(color = BorderSubtle)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Text(
                        text = record.recordedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    if (!record.vetName.isNullOrBlank()) {
                        Text("•", color = TextMuted)
                        Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Text(
                            text = record.vetName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MintSage, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertRose, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditHealthRecordDialog(
    petName: String,
    initialRecord: PetHealthRecordDto?,
    onDismiss: () -> Unit,
    onSave: (CreateHealthRecordRequest) -> Unit
) {
    val context = LocalContext.current
    val isEdit = initialRecord != null

    val recordTypes = listOf("Checkup", "Symptom", "Vet Visit", "Medication", "Surgery", "General")
    val severities = listOf("Normal", "Mild", "Moderate", "Severe")

    var selectedType by remember { mutableStateOf(initialRecord?.recordType ?: "Checkup") }
    var title by remember { mutableStateOf(initialRecord?.title ?: "") }
    var description by remember { mutableStateOf(initialRecord?.description ?: "") }
    var severity by remember { mutableStateOf(initialRecord?.severity ?: "Normal") }
    var recordedDate by remember {
        mutableStateOf(initialRecord?.recordedDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time))
    }
    var vetName by remember { mutableStateOf(initialRecord?.vetName ?: "") }

    val cal = Calendar.getInstance()
    val datePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val c = Calendar.getInstance().apply { set(year, month, day) }
                recordedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = WarmPearl,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEdit) "Edit Health Record" else "Add Health Record",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Record Type Chips
                Text("Record Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recordTypes) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = WarmPearl
                            )
                        )
                    }
                }

                // Title
                PetTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Title / Reason",
                    placeholder = "e.g., Annual Wellness Exam, Coughing",
                    leadingIcon = Icons.Default.MedicalServices
                )

                // Date Picker field
                Text("Date Recorded", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePicker.show() }
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = recordedDate, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    }
                }

                // Severity
                Text("Condition Severity", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(severities) { sev ->
                        FilterChip(
                            selected = severity == sev,
                            onClick = { severity = sev },
                            label = { Text(sev) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (sev == "Severe") AlertRose else if (sev == "Moderate") AmberAccent else EmeraldPrimary,
                                selectedLabelColor = WarmPearl
                            )
                        )
                    }
                }

                // Description
                PetTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description / Doctor Advice",
                    placeholder = "Enter examination details or observed symptoms",
                    leadingIcon = Icons.Default.Description
                )

                // Vet Name
                PetTextField(
                    value = vetName,
                    onValueChange = { vetName = it },
                    label = "Veterinarian / Clinic Name",
                    placeholder = "e.g., Dr. Sharma, City Pet Hospital",
                    leadingIcon = Icons.Default.LocalHospital
                )

                Spacer(Modifier.height(4.dp))

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
                            if (title.isNotBlank()) {
                                onSave(
                                    CreateHealthRecordRequest(
                                        recordType = selectedType,
                                        title = title.trim(),
                                        description = description.trim().ifEmpty { null },
                                        severity = severity,
                                        recordedDate = recordedDate.trim(),
                                        vetName = vetName.trim().ifEmpty { null },
                                        notes = null
                                    )
                                )
                            } else {
                                Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = WarmPearl
                        )
                    ) {
                        Text(if (isEdit) "Save Changes" else "Add Record", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
