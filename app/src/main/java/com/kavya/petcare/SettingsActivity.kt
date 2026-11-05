package com.kavya.petcare

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.data.models.PetDto
import com.kavya.petcare.data.session.SessionManager
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.components.PetAvatar
import com.kavya.petcare.ui.components.PetImageLoader
import com.kavya.petcare.ui.components.UserAvatar
import com.kavya.petcare.ui.theme.PetCareTheme
import java.io.ByteArrayOutputStream
import java.io.InputStream

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onNavigateAddPet = {
                        startActivity(Intent(this@SettingsActivity, SelectPetTypeActivity::class.java))
                    },
                    onLoggedOut = {
                        val intent = Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onBack: () -> Unit,
    onNavigateAddPet: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val session = remember { SessionManager.getInstance(context) }
    val allPets by viewModel.allPets.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Dialog & Feedback States
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showReportProblemDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var petToDelete by remember { mutableStateOf<PetDto?>(null) }
    var petToEdit by remember { mutableStateOf<PetDto?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showAvatarChooserDialog by remember { mutableStateOf(false) }
    val userAvatarUrl by viewModel.userAvatarUrl.collectAsState()

    fun uploadUserBitmap(bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        val base64 = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        PetImageLoader.cacheBitmap(context, base64, bitmap)
        viewModel.uploadUserAvatar(base64, bitmap) { success, msg ->
            statusMessage = msg
        }
    }

    val userGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    uploadUserBitmap(bitmap)
                }
            } catch (e: Exception) {
                statusMessage = "Could not load image: ${e.localizedMessage}"
            }
        }
    }

    val userCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            uploadUserBitmap(bitmap)
        }
    }

    // Read version info
    val versionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadAllPets()
        viewModel.loadPreferences()
    }

    // ------------------------------------------------------------------------
    // Dialog: Edit Account Profile
    // ------------------------------------------------------------------------
    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(session.getUserName()) }
        var emailInput by remember { mutableStateOf(session.getUserEmail()) }
        var profileError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Account Profile", fontWeight = FontWeight.Bold, color = EmeraldDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("User ID: #${session.getUserId()}", fontSize = 13.sp, color = Color.Gray)
                    if (profileError != null) {
                        Text(profileError ?: "", color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it; profileError = null },
                        label = { Text("Full Name") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF111816), fontSize = 15.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it; profileError = null },
                        label = { Text("Email Address") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF111816), fontSize = 15.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isBlank() || emailInput.isBlank()) {
                            profileError = "Name and email are required."
                            return@Button
                        }
                        viewModel.updateUserProfile(nameInput, emailInput) { success, msg ->
                            if (success) {
                                showEditProfileDialog = false
                                statusMessage = msg
                            } else {
                                profileError = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF267D6D))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Dialog: Change Password
    // ------------------------------------------------------------------------
    if (showChangePasswordDialog) {
        var currentPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var passError by remember { mutableStateOf<String?>(null) }
        var passVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Password", fontWeight = FontWeight.Bold, color = EmeraldDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (passError != null) {
                        Text(passError ?: "", color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it; passError = null },
                        label = { Text("Current Password") },
                        singleLine = true,
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        textStyle = TextStyle(color = Color(0xFF111816), fontSize = 15.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it; passError = null },
                        label = { Text("New Password (min 6 chars)") },
                        singleLine = true,
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        textStyle = TextStyle(color = Color(0xFF111816), fontSize = 15.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it; passError = null },
                        label = { Text("Confirm New Password") },
                        singleLine = true,
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        textStyle = TextStyle(color = Color(0xFF111816), fontSize = 15.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { passVisible = !passVisible }) {
                            Text(if (passVisible) "Hide Passwords" else "Show Passwords", fontSize = 12.sp, color = Color(0xFF267D6D))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPass.isBlank() || newPass.isBlank()) {
                            passError = "Please fill in all password fields."
                            return@Button
                        }
                        if (newPass.length < 6) {
                            passError = "New password must be at least 6 characters."
                            return@Button
                        }
                        if (newPass != confirmPass) {
                            passError = "New passwords do not match."
                            return@Button
                        }
                        viewModel.changePassword(currentPass, newPass) { success, msg ->
                            if (success) {
                                showChangePasswordDialog = false
                                statusMessage = msg
                            } else {
                                passError = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF267D6D))
                ) {
                    Text("Update Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Dialog: Delete Pet Confirmation
    // ------------------------------------------------------------------------
    petToDelete?.let { pet ->
        AlertDialog(
            onDismissRequest = { petToDelete = null },
            title = { Text("Delete Pet Profile", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
            text = {
                Text("Are you sure you want to delete ${pet.name} (${pet.breed})? This action cannot be undone.", color = Color(0xFF2D3748))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePet(pet.id) { success, msg ->
                            petToDelete = null
                            statusMessage = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { petToDelete = null }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Dialog: Report a Problem
    // ------------------------------------------------------------------------
    if (showReportProblemDialog) {
        var subject by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Bug Report") }
        var description by remember { mutableStateOf("") }
        var reportError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showReportProblemDialog = false },
            title = { Text("Report a Problem", fontWeight = FontWeight.Bold, color = EmeraldDark) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (reportError != null) {
                        Text(reportError ?: "", color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Category:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2D3748))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Bug", "Account", "Feeding", "General").forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it; reportError = null },
                        label = { Text("Subject *") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF111816), fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it; reportError = null },
                        label = { Text("Describe the issue in detail *") },
                        minLines = 3,
                        maxLines = 5,
                        textStyle = TextStyle(color = Color(0xFF111816), fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subject.isBlank() || description.isBlank()) {
                            reportError = "Please provide both a subject and description."
                            return@Button
                        }
                        viewModel.submitSupportReport(category, subject, description) { success, msg ->
                            if (success) {
                                showReportProblemDialog = false
                                statusMessage = msg
                            } else {
                                reportError = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF267D6D))
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportProblemDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Dialog: FAQs
    // ------------------------------------------------------------------------
    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text("Pet Care FAQs", fontWeight = FontWeight.Bold, color = EmeraldDark) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FaqItem(
                        q = "How do I switch active pets?",
                        a = "You can manage your pets in Pet Management below or tap on any pet card to make it your active companion on the dashboard."
                    )
                    FaqItem(
                        q = "How do care reminders work?",
                        a = "Reminders alert you when it's time for daily feeding, medication, grooming, or veterinary checkups. Toggles in Settings let you customize push notifications."
                    )
                    FaqItem(
                        q = "Is the AI health scan a medical diagnosis?",
                        a = "No. The AI scan provides symptom guidance and preliminary recommendations only. Always consult a licensed veterinarian for urgent medical concerns."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFaqDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF267D6D))
                ) {
                    Text("Close")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Dialog: Privacy Policy
    // ------------------------------------------------------------------------
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, color = EmeraldDark) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Pet Care Companion Privacy Commitment", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmeraldDark)
                    Text(
                        "Your privacy is our priority. Pet Care Companion securely manages your account and pet care records. " +
                        "All passwords are encrypted with bcrypt, mobile sessions use secure cryptographic tokens, and pet data is private to your authenticated account.",
                        fontSize = 13.sp,
                        color = Color(0xFF2D3748)
                    )
                    Text("Data We Collect:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EmeraldDark)
                    Text("• Account credentials (name, email)\n• Pet profile information (breed, weight, age)\n• Care schedules, meal entries, and symptom logs", fontSize = 12.sp, color = Color(0xFF4A5568))
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyPolicyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF267D6D))
                ) {
                    Text("Got It")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Dialog: Logout Confirmation
    // ------------------------------------------------------------------------
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
            text = { Text("Are you sure you want to log out of Pet Care?", color = Color(0xFF2D3748)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout { onLoggedOut() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Dialog: Avatar Photo Chooser
    // ------------------------------------------------------------------------
    if (showAvatarChooserDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarChooserDialog = false },
            title = {
                Text(
                    text = "Profile Photo",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            },
            text = {
                Text(
                    text = "Choose a photo source for your account profile:",
                    color = Color(0xFF2D3748)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAvatarChooserDialog = false
                        userGalleryLauncher.launch("image/*")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintSage)
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gallery")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showAvatarChooserDialog = false
                        userCameraLauncher.launch(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Camera")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Main Settings Layout
    // ------------------------------------------------------------------------
    Surface(modifier = Modifier.fillMaxSize(), color = SoftBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            // Modern Top Bar
            PetModernTopBar(
                title = "App Settings",
                subtitle = "Manage account, pets, and preferences",
                onBack = onBack
            )

            // Status message banner
            if (statusMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MintSoft,
                    border = BorderStroke(1.dp, MintSage.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusMessage ?: "",
                            color = EmeraldDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { statusMessage = null }) {
                            Text("OK", color = MintSage, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // ================================================================
                // Section A: Account Settings
                // ================================================================
                Column {
                    SettingsSectionHeader(title = "Account Settings", icon = Icons.Default.Person)
                    PetModernCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    UserAvatar(
                                        avatarUrl = userAvatarUrl,
                                        userName = session.getUserName(),
                                        shadowElevation = 3.dp,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clickable { showAvatarChooserDialog = true }
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .shadow(2.dp, CircleShape)
                                            .clip(CircleShape)
                                            .clickable { showAvatarChooserDialog = true },
                                        shape = CircleShape,
                                        color = MintSage,
                                        border = BorderStroke(1.5.dp, Color.White)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Change Photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.getUserName(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = EmeraldDark
                                    )
                                    Text(
                                        text = "${session.getUserEmail()}  •  ID #${session.getUserId()}",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            HorizontalDivider(color = BorderSubtle)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showEditProfileDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.2.dp, MintSage)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MintSage)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Edit Info", fontSize = 13.sp, color = EmeraldDark, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { showChangePasswordDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.2.dp, MintSage)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MintSage)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Password", fontSize = 13.sp, color = EmeraldDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ================================================================
                // Section B: Pet Management
                // ================================================================
                Column {
                    SettingsSectionHeader(title = "Pet Management", icon = Icons.Default.Pets)
                    PetModernCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            if (allPets.isEmpty()) {
                                Text("No pets registered yet.", fontSize = 13.sp, color = TextMuted, modifier = Modifier.padding(vertical = 8.dp))
                            } else {
                                val activePetId = session.getActivePetId()
                                allPets.forEachIndexed { index, pet ->
                                    val isActive = pet.id == activePetId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                viewModel.selectActivePet(pet)
                                                statusMessage = "${pet.name} is now your active pet."
                                            }
                                            .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PetAvatar(
                                                imageUrl = pet.imageUrl,
                                                species = pet.species,
                                                petName = pet.name,
                                                modifier = Modifier.size(46.dp)
                                            )
                                            Spacer(Modifier.width(14.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(pet.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EmeraldDark)
                                                    if (isActive) {
                                                        Spacer(Modifier.width(8.dp))
                                                        PetBadge(text = "ACTIVE", color = MintSage, textColor = EmeraldDark)
                                                    }
                                                }
                                                Text("${pet.species} • ${pet.breed} • ${pet.weightKg} kg", fontSize = 12.sp, color = TextSecondary)
                                            }
                                        }
                                        IconButton(onClick = { petToDelete = pet }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRose, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    if (index < allPets.size - 1) {
                                        HorizontalDivider(color = BorderSubtle)
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            PetGradientButton(
                                text = "Add Another Pet",
                                icon = Icons.Default.Add,
                                onClick = onNavigateAddPet
                            )
                        }
                    }
                }

                // ================================================================
                // Section C: Notifications
                // ================================================================
                Column {
                    SettingsSectionHeader(title = "Notifications", icon = Icons.Default.Notifications)
                    PetModernCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SettingSwitchRow(
                                title = "Vaccination Reminders",
                                subtitle = "Alerts for upcoming booster shots",
                                checked = preferences.notifVaccination,
                                onCheckedChange = { checked ->
                                    viewModel.updatePreferences(mapOf("notif_vaccination" to checked))
                                }
                            )
                            HorizontalDivider(color = BorderSubtle)
                            SettingSwitchRow(
                                title = "Medication Reminders",
                                subtitle = "Daily prescription alerts",
                                checked = preferences.notifMedication,
                                onCheckedChange = { checked ->
                                    viewModel.updatePreferences(mapOf("notif_medication" to checked))
                                }
                            )
                            HorizontalDivider(color = BorderSubtle)
                            SettingSwitchRow(
                                title = "Appointment Reminders",
                                subtitle = "Vet clinic and grooming bookings",
                                checked = preferences.notifAppointments,
                                onCheckedChange = { checked ->
                                    viewModel.updatePreferences(mapOf("notif_appointments" to checked))
                                }
                            )
                        }
                    }
                }

                // ================================================================
                // Section D: Appearance
                // ================================================================
                Column {
                    SettingsSectionHeader(title = "Appearance", icon = Icons.Default.Palette)
                    PetModernCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Theme Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = EmeraldDark)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                                    FilterChip(
                                        selected = preferences.themeMode == mode,
                                        onClick = { viewModel.updatePreferences(mapOf("theme_mode" to mode)) },
                                        label = { Text(label, fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            HorizontalDivider(color = BorderSubtle)

                            Text("Font Size", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = EmeraldDark)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("small" to "Small", "normal" to "Default", "large" to "Large").forEach { (size, label) ->
                                    FilterChip(
                                        selected = preferences.fontSize == size,
                                        onClick = { viewModel.updatePreferences(mapOf("font_size" to size)) },
                                        label = { Text(label, fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // ================================================================
                // Section E: Privacy & Security
                // ================================================================
                Column {
                    SettingsSectionHeader(title = "Privacy & Security", icon = Icons.Default.Security)
                    PetModernCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SettingsNavigationRow(
                                title = "Manage App Permissions",
                                subtitle = "Camera, storage, and notification permissions",
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                            HorizontalDivider(color = BorderSubtle)
                            SettingsNavigationRow(
                                title = "Privacy Policy",
                                subtitle = "How we protect and handle your pet data",
                                onClick = { showPrivacyPolicyDialog = true }
                            )
                        }
                    }
                }

                // ================================================================
                // Section F: Help & Support
                // ================================================================
                Column {
                    SettingsSectionHeader(title = "Help & Support", icon = Icons.Default.HelpOutline)
                    PetModernCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SettingsNavigationRow(
                                title = "Pet Care FAQs",
                                subtitle = "Tips, care guides, and answers to common questions",
                                onClick = { showFaqDialog = true }
                            )
                            HorizontalDivider(color = BorderSubtle)
                            SettingsNavigationRow(
                                title = "Report a Problem",
                                subtitle = "Submit issue reports or feedback to our team",
                                onClick = { showReportProblemDialog = true }
                            )
                            HorizontalDivider(color = BorderSubtle)
                            SettingsNavigationRow(
                                title = "Contact Support",
                                subtitle = "Email our dedicated care team at support@petcare.com",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:support@petcare.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "Pet Care App Support Request")
                                    }
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                }
                            )
                        }
                    }
                }

                // ================================================================
                // Section G: About App
                // ================================================================
                Column {
                    SettingsSectionHeader(title = "About App", icon = Icons.Default.Info)
                    PetModernCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Pet Care Companion", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldDark)
                            Text("Version: $versionName", fontSize = 13.sp, color = TextSecondary)
                            Text("Smart, intelligent companion care for healthy, happy pets.", fontSize = 12.sp, color = TextMuted)
                        }
                    }
                }

                // ================================================================
                // Section H: Logout Option
                // ================================================================
                PetOutlinedButton(
                    text = "Log Out of Account",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    borderColor = AlertRose.copy(alpha = 0.5f),
                    contentColor = AlertRose,
                    onClick = { showLogoutDialog = true }
                )
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(MintSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MintSage, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldDark
        )
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = EmeraldDark)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MintSage,
                uncheckedTrackColor = BorderSubtle
            )
        )
    }
}

@Composable
fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = EmeraldDark)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun FaqItem(q: String, a: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .background(SoftBackground, RoundedCornerShape(12.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(q, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EmeraldDark, modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MintSage
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Text(a, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
        }
    }
}


