package com.kavya.petcare

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.EmeraldDark
import com.kavya.petcare.ui.MintSage
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
    val isLoading by viewModel.isLoading.collectAsState()

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
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()
        val base64 = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        PetImageLoader.cacheBitmap(base64, bitmap)
        val petId = activePet.id.toIntOrNull() ?: 1
        viewModel.uploadPetPhoto(petId, base64) { success, msg ->
            userMessage = msg
        }
    }

    // Gallery Picker Launcher
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

    // Camera Capture Launcher
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (editError != null) {
                        Text(
                            text = editError ?: "",
                            color = Color(0xFFC62828),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it; editError = null },
                        label = { Text("Pet Name *") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF111816), fontSize = 15.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111816),
                            unfocusedTextColor = Color(0xFF111816)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editType,
                        onValueChange = { editType = it; editError = null },
                        label = { Text("Species / Type * (e.g. Rabbit, Dog, Cat)") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF111816), fontSize = 15.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111816),
                            unfocusedTextColor = Color(0xFF111816)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editBreed,
                        onValueChange = { editBreed = it; editError = null },
                        label = { Text("Breed *") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF111816), fontSize = 15.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111816),
                            unfocusedTextColor = Color(0xFF111816)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editAge,
                            onValueChange = { editAge = it; editError = null },
                            label = { Text("Age (e.g. 1.5 yrs)") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color(0xFF111816), fontSize = 15.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111816),
                                unfocusedTextColor = Color(0xFF111816)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = editWeight,
                            onValueChange = { editWeight = it; editError = null },
                            label = { Text("Weight (kg)") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color(0xFF111816), fontSize = 15.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111816),
                                unfocusedTextColor = Color(0xFF111816)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editGender,
                            onValueChange = { editGender = it },
                            label = { Text("Gender") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color(0xFF111816), fontSize = 15.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111816),
                                unfocusedTextColor = Color(0xFF111816)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = editMicrochip,
                            onValueChange = { editMicrochip = it },
                            label = { Text("Microchip ID") },
                            singleLine = true,
                            textStyle = TextStyle(color = Color(0xFF111816), fontSize = 15.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111816),
                                unfocusedTextColor = Color(0xFF111816)
                            ),
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
                        if (trimmedName.isBlank() || trimmedBreed.isBlank() || trimmedType.isBlank()) {
                            editError = "Please fill in all required fields (Name, Type, Breed)."
                            return@Button
                        }
                        val petId = activePet.id.toIntOrNull()?.takeIf { it > 0 }
                            ?: com.kavya.petcare.data.session.SessionManager.getInstance(context).getActivePetId()
                        isSavingPet = true
                        editError = null
                        viewModel.editPetProfile(
                            petId = petId,
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
                            onError = { err ->
                                isSavingPet = false
                                editError = err
                            }
                        )
                    },
                    enabled = !isSavingPet,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF267D6D))
                ) {
                    if (isSavingPet) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
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
                    },
                    enabled = !isSavingPet
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Dialog: Photo Source Chooser
    // ------------------------------------------------------------------------
    if (showPhotoChooserDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoChooserDialog = false },
            title = {
                Text(
                    text = "Change Pet Photo",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select a photo source for ${activePet.name}:", color = Color(0xFF2D3748))
                    Button(
                        onClick = {
                            showPhotoChooserDialog = false
                            cameraLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF267D6D))
                    ) {
                        Text("📷 Take Photo with Camera")
                    }
                    OutlinedButton(
                        onClick = {
                            showPhotoChooserDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🖼️ Choose from Gallery", color = Color(0xFF267D6D))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoChooserDialog = false }) {
                    Text("Cancel", color = Color.Gray)
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
            title = {
                Text(
                    text = "Confirm Logout",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to log out of Pet Care?",
                    color = Color(0xFF2D3748),
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout {
                            onLoggedOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Logout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Screen Layout
    // ------------------------------------------------------------------------
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF6F8F3)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EmeraldDark
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Pet Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark
                    )
                }

                Row {
                    IconButton(onClick = { openEditDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = Color(0xFF267D6D)
                        )
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color(0xFFC62828)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // User Notification Message Banner
            if (userMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5F2)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = userMessage ?: "",
                            color = Color(0xFF184D43),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { userMessage = null }) {
                            Text("OK", color = Color(0xFF267D6D), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Pet Avatar with Camera Badge
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.size(110.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .clickable { showPhotoChooserDialog = true },
                    shape = CircleShape,
                    color = Color(0xFFE8F5F2),
                    shadowElevation = 4.dp
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

                // Edit photo badge
                Surface(
                    modifier = Modifier
                        .size(34.dp)
                        .clickable { showPhotoChooserDialog = true },
                    shape = CircleShape,
                    color = Color(0xFF267D6D),
                    shadowElevation = 4.dp
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

            Spacer(Modifier.height(12.dp))

            Text(
                text = activePet.name,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldDark
            )

            Text(
                text = "${activePet.breed} ${activePet.type}",
                fontSize = 15.sp,
                color = Color(0xFF4A5568)
            )

            Spacer(Modifier.height(24.dp))

            // Profile Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ProfileRow(label = "Species", value = activePet.type)
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    ProfileRow(label = "Breed", value = activePet.breed)
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    ProfileRow(label = "Age", value = activePet.age)
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    ProfileRow(label = "Weight", value = activePet.weight)
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    ProfileRow(label = "Gender", value = activePet.gender)
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    ProfileRow(label = "Microchip ID", value = activePet.microchipId)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Health Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5F2))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Health & Vaccination Status",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Vaccinations: ${activePet.vaccinationStatus}. Health score: ${activePet.healthScore}/100.",
                        color = Color(0xFF2D3748),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = { openEditDialog() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF267D6D))
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit Pet Details", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFC62828))
                Spacer(Modifier.width(8.dp))
                Text("Log Out", color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF556B64), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(text = value, fontWeight = FontWeight.Bold, color = Color(0xFF111816), fontSize = 15.sp)
    }
}
