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
import com.kavya.petcare.data.models.CreateVaccinationRequest
import com.kavya.petcare.data.models.PetVaccinationDto
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class VaccinationDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                VaccinationDetailScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun VaccinationDetailScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activePet by viewModel.activePet.collectAsState()
    val vacOverview by viewModel.vaccinationOverview.collectAsState()
    val isLoading by viewModel.isLoadingVaccines.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var vaccineToEdit by remember { mutableStateOf<PetVaccinationDto?>(null) }
    var vaccineToDelete by remember { mutableStateOf<PetVaccinationDto?>(null) }

    val petId = activePet.id.toIntOrNull() ?: 1

    LaunchedEffect(activePet.id) {
        if (petId > 0) {
            viewModel.loadVaccinations(petId)
        }
    }

    val overallStatus = vacOverview?.overallStatus ?: "NO RECORDS"
    val vaccines = vacOverview?.vaccinations ?: emptyList()

    Scaffold(
        topBar = {
            PetModernTopBar(
                title = "Vaccination Records",
                subtitle = "Immunization schedule for ${activePet.name}",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Add Vaccine",
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
            // 1. Overall Status Hero Card
            val (statusColor, statusBg, statusDesc) = when (overallStatus.uppercase()) {
                "UP TO DATE" -> Triple(
                    EmeraldPrimary,
                    MintSoft,
                    "All recorded immunizations are complete and active. No vaccinations are due or overdue."
                )
                "DUE SOON" -> Triple(
                    AmberAccent,
                    AmberSoft,
                    "At least one vaccination is due in the next 30 days. Plan your veterinary booster visit."
                )
                "OVERDUE" -> Triple(
                    AlertRose,
                    AlertRoseSoft,
                    "One or more vaccinations are past due date! Schedule an immunization appointment promptly."
                )
                else -> Triple(
                    TextSecondary,
                    WarmPearl,
                    "No vaccination records found for this pet profile. Record previous shots or upcoming schedules."
                )
            }

            PetModernCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = WarmPearl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(statusBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (overallStatus == "UP TO DATE") Icons.Default.Shield else if (overallStatus == "OVERDUE") Icons.Default.Warning else Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "VACCINATION STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    PetBadge(
                        text = overallStatus,
                        containerColor = statusBg,
                        contentColor = statusColor
                    )

                    Text(
                        text = statusDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            // 2. Species-Specific Guidance Info Card
            SpeciesVaccineGuidanceCard(species = activePet.type)

            // 3. Vaccination History List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PetSectionHeader(title = "Recorded Vaccines (${vaccines.size})")

                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Vaccine", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            }

            if (vaccines.isEmpty()) {
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
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MintSage,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No vaccination records yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Add ${activePet.name}'s immunizations to automatically calculate status and receive upcoming booster reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Add Vaccination Record", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    vaccines.forEach { vac ->
                        VaccineRecordCard(
                            vaccine = vac,
                            onEdit = { vaccineToEdit = vac },
                            onDelete = { vaccineToDelete = vac }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Add Vaccine Dialog
    if (showAddDialog) {
        AddEditVaccineDialog(
            species = activePet.type,
            initialVaccine = null,
            onDismiss = { showAddDialog = false },
            onSave = { request ->
                viewModel.addVaccination(petId, request) { success, err ->
                    if (success) {
                        Toast.makeText(context, "Vaccination record added!", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    } else {
                        Toast.makeText(context, err ?: "Failed to save record", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Edit Vaccine Dialog
    if (vaccineToEdit != null) {
        AddEditVaccineDialog(
            species = activePet.type,
            initialVaccine = vaccineToEdit,
            onDismiss = { vaccineToEdit = null },
            onSave = { request ->
                viewModel.updateVaccination(petId, vaccineToEdit!!.id, request) { success, err ->
                    if (success) {
                        Toast.makeText(context, "Vaccination record updated!", Toast.LENGTH_SHORT).show()
                        vaccineToEdit = null
                    } else {
                        Toast.makeText(context, err ?: "Failed to update record", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (vaccineToDelete != null) {
        AlertDialog(
            onDismissRequest = { vaccineToDelete = null },
            title = { Text("Delete Vaccination Record?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Are you sure you want to delete '${vaccineToDelete!!.vaccineName}'? The overall vaccination status will be recalculated immediately.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        val id = vaccineToDelete!!.id
                        vaccineToDelete = null
                        viewModel.deleteVaccination(petId, id) { success, _ ->
                            if (success) {
                                Toast.makeText(context, "Vaccination deleted.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRose)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { vaccineToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun VaccineRecordCard(
    vaccine: PetVaccinationDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (statusColor, statusBg) = when (vaccine.status.uppercase()) {
        "OVERDUE" -> Pair(AlertRose, AlertRoseSoft)
        "DUE SOON" -> Pair(AmberAccent, AmberSoft)
        else -> Pair(EmeraldPrimary, MintSoft)
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalInformation,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = vaccine.vaccineName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (vaccine.dueDescription.isNotBlank()) {
                            Text(
                                text = vaccine.dueDescription,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }

                PetBadge(
                    text = vaccine.status,
                    containerColor = statusBg,
                    contentColor = statusColor
                )
            }

            // Dates Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftBackground, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Administered", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(vaccine.vaccinationDate, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Next Due Date", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(vaccine.nextDueDate, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }

            if (!vaccine.clinicName.isNullOrBlank() || !vaccine.batchNumber.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!vaccine.clinicName.isNullOrBlank()) {
                        Text(
                            text = "Clinic: ${vaccine.clinicName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    if (!vaccine.batchNumber.isNullOrBlank()) {
                        Text(
                            text = "Lot #${vaccine.batchNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle)

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MintSage, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertRose, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SpeciesVaccineGuidanceCard(species: String) {
    val normSpecies = species.lowercase()

    val (title, info) = when {
        normSpecies.contains("dog") -> Pair(
            "Veterinary Core Vaccines (Dog)",
            "Core vaccines recommended by veterinarians include Rabies, DHPP (Distemper, Hepatitis, Parvovirus, Parainfluenza), and Bordetella."
        )
        normSpecies.contains("cat") -> Pair(
            "Veterinary Core Vaccines (Cat)",
            "Core feline vaccines include Rabies, FVRCP (Feline Viral Rhinotracheitis, Calicivirus, Panleukopenia), and FeLV for outdoor cats."
        )
        normSpecies.contains("rabbit") -> Pair(
            "Veterinary Immunizations (Rabbit)",
            "Key rabbit vaccines include RHDV2 (Rabbit Hemorrhagic Disease Virus) and Myxomatosis (where endemic)."
        )
        normSpecies.contains("bird") || normSpecies.contains("pigeon") -> Pair(
            "Avian Immunizations (Bird / Pigeon)",
            "Pigeons and domestic birds commonly receive PMV-1 (Paramyxovirus) and Poxvirus vaccines."
        )
        normSpecies.contains("hamster") || normSpecies.contains("guinea") -> Pair(
            "Pocket Pet Preventative Care",
            "Hamsters and guinea pigs generally do not receive routine viral vaccines. Routine wellness exams, weight checks, and parasite control are recommended."
        )
        normSpecies.contains("fish") -> Pair(
            "Aquarium Care Wellness",
            "Pet fish do not receive routine viral vaccinations. Water quality testing, optimal temperature, and balanced nutrition are primary health factors."
        )
        else -> Pair(
            "Species Guidance",
            "Ensure core vaccinations are kept current in accordance with your licensed veterinarian's guidance."
        )
    }

    PetModernCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MintSoft
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EmeraldDark)
                Text(text = info, style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun AddEditVaccineDialog(
    species: String,
    initialVaccine: PetVaccinationDto?,
    onDismiss: () -> Unit,
    onSave: (CreateVaccinationRequest) -> Unit
) {
    val context = LocalContext.current
    val isEdit = initialVaccine != null

    val normSpecies = species.lowercase()
    val speciesPresets = when {
        normSpecies.contains("dog") -> listOf("Rabies", "DHPP (DAPP)", "Bordetella", "Leptospirosis", "Lyme")
        normSpecies.contains("cat") -> listOf("Rabies", "FVRCP", "FeLV (Leukemia)")
        normSpecies.contains("rabbit") -> listOf("RHDV2", "Myxomatosis")
        normSpecies.contains("bird") || normSpecies.contains("pigeon") -> listOf("PMV-1 (Paramyxovirus)", "Poxvirus")
        else -> listOf("Core Vaccine", "Booster Vaccine", "Parasite Prevention")
    }

    var vaccineName by remember { mutableStateOf(initialVaccine?.vaccineName ?: "") }
    var vaccinationDate by remember {
        mutableStateOf(initialVaccine?.vaccinationDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time))
    }
    var nextDueDate by remember {
        mutableStateOf(
            initialVaccine?.nextDueDate ?: run {
                val nextYear = Calendar.getInstance().apply { add(Calendar.YEAR, 1) }
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(nextYear.time)
            }
        )
    }
    var clinicName by remember { mutableStateOf(initialVaccine?.clinicName ?: "") }
    var batchNumber by remember { mutableStateOf(initialVaccine?.batchNumber ?: "") }
    var notes by remember { mutableStateOf(initialVaccine?.notes ?: "") }

    val cal = Calendar.getInstance()
    val administeredDatePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val c = Calendar.getInstance().apply { set(year, month, day) }
                vaccinationDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    val dueDatePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val c = Calendar.getInstance().apply { set(year, month, day) }
                nextDueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
            },
            cal.get(Calendar.YEAR) + 1,
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
                        text = if (isEdit) "Edit Vaccination" else "Add Vaccination",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Quick presets
                Text("Quick Select (${species}):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(speciesPresets) { preset ->
                        FilterChip(
                            selected = vaccineName.equals(preset, ignoreCase = true),
                            onClick = { vaccineName = preset },
                            label = { Text(preset) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = WarmPearl
                            )
                        )
                    }
                }

                PetTextField(
                    value = vaccineName,
                    onValueChange = { vaccineName = it },
                    label = "Vaccine Name",
                    placeholder = "e.g., Rabies, DHPP, RHDV2",
                    leadingIcon = Icons.Default.Vaccines
                )

                // Administered Date Picker
                Column {
                    Text("Vaccination Date", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { administeredDatePicker.show() }
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = vaccinationDate, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Next Due Date Picker
                Column {
                    Text("Next Due Date (Booster)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dueDatePicker.show() }
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = nextDueDate, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.Event, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Clinic Name
                PetTextField(
                    value = clinicName,
                    onValueChange = { clinicName = it },
                    label = "Veterinary Clinic / Doctor",
                    placeholder = "e.g., Companion Animal Clinic",
                    leadingIcon = Icons.Default.LocalHospital
                )

                // Batch Number
                PetTextField(
                    value = batchNumber,
                    onValueChange = { batchNumber = it },
                    label = "Batch / Lot Number (Optional)",
                    placeholder = "e.g., LOT-89421A",
                    leadingIcon = Icons.Default.QrCode
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
                            if (vaccineName.isNotBlank() && vaccinationDate.isNotBlank() && nextDueDate.isNotBlank()) {
                                onSave(
                                    CreateVaccinationRequest(
                                        vaccineName = vaccineName.trim(),
                                        vaccinationDate = vaccinationDate.trim(),
                                        nextDueDate = nextDueDate.trim(),
                                        batchNumber = batchNumber.trim().ifEmpty { null },
                                        clinicName = clinicName.trim().ifEmpty { null },
                                        notes = notes.trim().ifEmpty { null }
                                    )
                                )
                            } else {
                                Toast.makeText(context, "Please enter vaccine name and dates", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = WarmPearl
                        )
                    ) {
                        Text(if (isEdit) "Save Changes" else "Add Vaccine", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
