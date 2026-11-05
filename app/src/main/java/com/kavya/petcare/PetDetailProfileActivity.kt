package com.kavya.petcare

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.components.PetAvatar
import com.kavya.petcare.ui.components.PetImageLoader
import com.kavya.petcare.ui.theme.PetCareTheme
import java.io.ByteArrayOutputStream
import java.io.InputStream

class PetDetailProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                PetDetailProfileScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onLoggedOut = {
                        val intent = Intent(this@PetDetailProfileActivity, LoginActivity::class.java).apply {
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
fun PetDetailProfileScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val activePet by viewModel.activePet.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showPhotoChooserDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var userMessage by remember { mutableStateOf<String?>(null) }
    var localPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSavingPet by remember { mutableStateOf(false) }

    // Edit form states
    var editName by remember(activePet) { mutableStateOf(activePet.name) }
    var editBreed by remember(activePet) { mutableStateOf(activePet.breed) }
    var editType by remember(activePet) { mutableStateOf(activePet.type) }
    var editAge by remember(activePet) { mutableStateOf(activePet.age) }
    var editWeight by remember(activePet) { mutableStateOf(activePet.weight.replace(" kg", "")) }
    var editGender by remember(activePet) { mutableStateOf(activePet.gender) }
    var editMicrochip by remember(activePet) { mutableStateOf(activePet.microchipId) }
    var editError by remember { mutableStateOf<String?>(null) }

    fun openEditDialog() {
        editName = activePet.name
        editBreed = activePet.breed
        editType = activePet.type
        editAge = activePet.age
        editWeight = activePet.weight.replace(" kg", "")
        editGender = activePet.gender
        editMicrochip = activePet.microchipId
        editError = null
        isSavingPet = false
        showEditDialog = true
    }

    // Image helper: convert bitmap to base64
    fun uploadBitmap(bitmap: Bitmap) {
        localPreviewBitmap = bitmap
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        val base64 = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        PetImageLoader.cacheBitmap(context, base64, bitmap)
        val petId = activePet.id.toIntOrNull() ?: 1
        viewModel.uploadPetPhoto(petId, base64, bitmap) { _, msg ->
            userMessage = msg
        }
    }

    LaunchedEffect(activePet.id) {
        localPreviewBitmap = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    uploadBitmap(bitmap)
                }
            } catch (e: Exception) {
                userMessage = "Could not load selected image: ${e.localizedMessage}"
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            uploadBitmap(bitmap)
        }
    }

    // ------------------------------------------------------------------------
    // Dialog: Edit Pet Details
    // ------------------------------------------------------------------------
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSavingPet) {
                    showEditDialog = false
                    editError = null
                }
            },
            title = {
                Text(
                    text = "Edit Pet Profile",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (editError != null) {
                        Text(
                            text = editError ?: "",
                            color = AlertRose,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it; editError = null },
                        label = { Text("Pet Name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editType,
                        onValueChange = { editType = it; editError = null },
                        label = { Text("Species / Animal * (e.g. Rabbit, Dog, Cat)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editBreed,
                        onValueChange = { editBreed = it; editError = null },
                        label = { Text("Breed *") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editAge,
                            onValueChange = { editAge = it; editError = null },
                            label = { Text("Age") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = editWeight,
                            onValueChange = { editWeight = it; editError = null },
                            label = { Text("Weight (kg)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editGender,
                            onValueChange = { editGender = it; editError = null },
                            label = { Text("Gender") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = editMicrochip,
                            onValueChange = { editMicrochip = it; editError = null },
                            label = { Text("Microchip ID") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = editName.trim()
                        val trimmedType = editType.trim()
                        val trimmedBreed = editBreed.trim()

                        if (trimmedName.isBlank()) {
                            editError = "Pet name cannot be empty."
                            return@Button
                        }
                        if (trimmedType.isBlank()) {
                            editError = "Pet species / type cannot be empty."
                            return@Button
                        }
                        if (trimmedBreed.isBlank()) {
                            editError = "Pet breed cannot be empty."
                            return@Button
                        }

                        val numericPetId = activePet.id.toIntOrNull() ?: 1
                        isSavingPet = true
                        editError = null

                        viewModel.editPetProfile(
                            petId = numericPetId,
                            name = trimmedName,
                            breed = trimmedBreed,
                            age = editAge,
                            weight = editWeight,
                            type = trimmedType,
                            gender = editGender,
                            microchipId = editMicrochip,
                            onSuccess = {
                                isSavingPet = false
                                showEditDialog = false
                                userMessage = "Pet profile updated successfully!"
                            },
                            onError = { errMsg ->
                                isSavingPet = false
                                editError = errMsg
                            }
                        )
                    },
                    enabled = !isSavingPet,
                    colors = ButtonDefaults.buttonColors(containerColor = MintSage),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isSavingPet) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Changes")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isSavingPet) {
                            showEditDialog = false
                            editError = null
                        }
                    }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Dialog: Photo Chooser
    // ------------------------------------------------------------------------
    if (showPhotoChooserDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoChooserDialog = false },
            title = {
                Text(
                    text = "Update Pet Photo",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            },
            text = {
                Text(
                    text = "Choose a photo source for ${activePet.name}:",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPhotoChooserDialog = false
                        galleryLauncher.launch("image/*")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintSage),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gallery")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showPhotoChooserDialog = false
                        cameraLauncher.launch(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Camera")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Dialog: Logout Confirmation
    // ------------------------------------------------------------------------
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out", fontWeight = FontWeight.Bold, color = AlertRose) },
            text = { Text("Are you sure you want to log out of Pet Care?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout { onLoggedOut() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRose),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Main Profile Screen Layout
    // ------------------------------------------------------------------------
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SoftBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Modern Top Bar
            PetModernTopBar(
                title = "Pet Profile",
                onBack = onBack,
                actions = {
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable { openEditDialog() },
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, BorderSubtle),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MintSage, modifier = Modifier.size(18.dp))
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable { showLogoutDialog = true },
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, BorderSubtle),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = AlertRose, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            )

            // User Notification Message Banner
            if (userMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
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
                            text = userMessage ?: "",
                            color = EmeraldDark,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { userMessage = null }) {
                            Text("OK", color = MintSage, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Pet Avatar with Camera Badge
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.size(124.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(116.dp)
                        .shadow(12.dp, CircleShape, ambientColor = MintSage.copy(alpha = 0.35f))
                        .clip(CircleShape)
                        .clickable { showPhotoChooserDialog = true },
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(3.5.dp, MintSage.copy(alpha = 0.35f))
                ) {
                    if (localPreviewBitmap != null) {
                        Image(
                            bitmap = localPreviewBitmap!!.asImageBitmap(),
                            contentDescription = "Pet Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        PetAvatar(
                            imageUrl = activePet.imageUrl,
                            species = activePet.type,
                            petName = activePet.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Camera Badge Button
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .clickable { showPhotoChooserDialog = true },
                    shape = CircleShape,
                    color = MintSage,
                    border = BorderStroke(2.dp, Color.White)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change Photo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = activePet.name,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldDark
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                PetBadge(text = activePet.type.uppercase(), color = MintSage)
                Text("•", color = TextMuted)
                Text(
                    text = activePet.breed,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(24.dp))

            // Quick Stats 4-Pill Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PetStatPill(
                    label = "Weight",
                    value = activePet.weight,
                    icon = Icons.Default.MonitorWeight,
                    tintColor = SkyBlue,
                    modifier = Modifier.weight(1f)
                )
                PetStatPill(
                    label = "Age",
                    value = activePet.age,
                    icon = Icons.Default.Cake,
                    tintColor = AmberAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PetStatPill(
                    label = "Health",
                    value = "${activePet.healthScore}/100",
                    icon = Icons.Default.Favorite,
                    tintColor = AlertRose,
                    modifier = Modifier.weight(1f)
                )
                PetStatPill(
                    label = "Vaccine",
                    value = activePet.vaccinationStatus,
                    icon = Icons.Default.Shield,
                    tintColor = MintSage,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Profile Details Section Card
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                PetSectionHeader(title = "Pet Profile Information")
                Spacer(Modifier.height(12.dp))

                PetModernCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ProfileInfoRow(icon = Icons.Default.Pets, label = "Species / Animal", value = activePet.type)
                        HorizontalDivider(color = BorderSubtle)
                        ProfileInfoRow(icon = Icons.Default.Category, label = "Breed", value = activePet.breed)
                        HorizontalDivider(color = BorderSubtle)
                        ProfileInfoRow(icon = Icons.Default.CalendarToday, label = "Age", value = activePet.age)
                        HorizontalDivider(color = BorderSubtle)
                        ProfileInfoRow(icon = Icons.Default.Scale, label = "Weight", value = activePet.weight)
                        HorizontalDivider(color = BorderSubtle)
                        ProfileInfoRow(icon = Icons.Default.Female, label = "Gender", value = activePet.gender)
                        HorizontalDivider(color = BorderSubtle)
                        ProfileInfoRow(icon = Icons.Default.QrCode, label = "Microchip ID", value = activePet.microchipId)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PetGradientButton(
                    text = "Edit Pet Profile",
                    icon = Icons.Default.Edit,
                    onClick = { openEditDialog() }
                )

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
fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MintSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MintSage,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(text = label, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Text(text = value, fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 14.sp)
    }
}
