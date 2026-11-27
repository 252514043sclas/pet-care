package com.kavya.petcare

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.components.PetAvatar
import com.kavya.petcare.ui.theme.PetCareTheme
import java.io.ByteArrayOutputStream
import java.io.InputStream

class AddPetProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val petType = intent.getStringExtra("EXTRA_PET_TYPE") ?: "Rabbit"
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                AddPetProfileScreen(
                    initialType = petType,
                    viewModel = viewModel,
                    onBack = { finish() },
                    onSave = {
                        val intent = Intent(this@AddPetProfileActivity, HomeDashboardActivity::class.java).apply {
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
fun AddPetProfileScreen(
    initialType: String = "Dog",
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onBack: () -> Unit = {},
    onSave: () -> Unit
) {
    val context = LocalContext.current

    val speciesList = listOf("Dog", "Cat", "Rabbit", "Bird", "Hamster", "Other")
    var selectedType by remember {
        mutableStateOf(if (speciesList.contains(initialType)) initialType else "Dog")
    }

    var petName by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedBase64 by remember { mutableStateOf<String?>(null) }
    var showPhotoChooserDialog by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()

    fun handleSelectedBitmap(bitmap: Bitmap) {
        selectedBitmap = bitmap
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val byteArray = stream.toByteArray()
            selectedBase64 = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (_: Exception) {}
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    handleSelectedBitmap(bitmap)
                }
            } catch (_: Exception) {}
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            handleSelectedBitmap(bitmap)
        }
    }

    // Photo Chooser Dialog
    if (showPhotoChooserDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoChooserDialog = false },
            title = {
                Text(
                    text = "Add Pet Photo",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            },
            text = {
                Text(
                    text = "Choose an image source for ${petName.ifBlank { "your pet" }}:",
                    color = Color(0xFF2D3748)
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SoftBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PetModernTopBar(
                title = "Add Pet Profile",
                onBack = onBack
            )

            Spacer(Modifier.height(8.dp))

            // Avatar Preview with Decorative Glow & Camera Action
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.size(112.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(104.dp)
                        .shadow(10.dp, CircleShape, ambientColor = MintSage.copy(alpha = 0.35f))
                        .clip(CircleShape)
                        .clickable { showPhotoChooserDialog = true },
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(3.dp, MintSage.copy(alpha = 0.35f))
                ) {
                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = petName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        PetAvatar(
                            imageUrl = null,
                            species = initialType,
                            petName = petName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Camera Badge Button
                Surface(
                    modifier = Modifier
                        .size(34.dp)
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
                            contentDescription = "Upload Pet Photo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Tell us about your $selectedType",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldDark
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "We will tailor care reminders, meals, and health tips",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(20.dp))

            // Species Selector Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Select Species",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldDark
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    speciesList.take(3).forEach { sp ->
                        val isSelected = selectedType.equals(sp, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedType = sp },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MintSage else Color.White,
                            border = BorderStroke(1.5.dp, if (isSelected) MintSage else BorderSubtle)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sp,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else EmeraldDark
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    speciesList.drop(3).forEach { sp ->
                        val isSelected = selectedType.equals(sp, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedType = sp },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MintSage else Color.White,
                            border = BorderStroke(1.5.dp, if (isSelected) MintSage else BorderSubtle)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sp,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else EmeraldDark
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Error Banner
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = AlertRose.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, AlertRose.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = AlertRose,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Form Card
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                PetModernCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PetTextField(
                            value = petName,
                            onValueChange = { petName = it; errorMessage = null },
                            label = "Pet Name *",
                            placeholder = "Enter your pet's name (e.g. Milo)",
                            leadingIcon = Icons.Default.Pets
                        )

                        PetTextField(
                            value = breed,
                            onValueChange = { breed = it; errorMessage = null },
                            label = "Breed *",
                            placeholder = "Enter breed (e.g. Beagle, Persian)",
                            leadingIcon = Icons.Default.Category
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = age,
                                onValueChange = { age = it; errorMessage = null },
                                label = { Text("Age (yrs) *") },
                                placeholder = { Text("e.g. 2.0") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MintSage,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedLabelColor = EmeraldDark
                                )
                            )

                            OutlinedTextField(
                                value = weight,
                                onValueChange = { weight = it; errorMessage = null },
                                label = { Text("Weight (kg) *") },
                                placeholder = { Text("e.g. 5.5") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MintSage,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedLabelColor = EmeraldDark
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                PetGradientButton(
                    text = "Save & Complete Setup",
                    isLoading = isLoading,
                    onClick = {
                        val trimmedName = petName.trim()
                        val trimmedBreed = breed.trim()
                        val trimmedAge = age.trim()
                        val trimmedWeight = weight.trim()

                        if (trimmedName.isBlank()) {
                            errorMessage = "Please enter your pet's name."
                            return@PetGradientButton
                        }
                        if (trimmedBreed.isBlank()) {
                            errorMessage = "Please enter your pet's breed."
                            return@PetGradientButton
                        }
                        if (trimmedAge.isBlank()) {
                            errorMessage = "Please enter your pet's age."
                            return@PetGradientButton
                        }
                        if (trimmedWeight.isBlank()) {
                            errorMessage = "Please enter your pet's weight."
                            return@PetGradientButton
                        }
                        if (trimmedWeight.toDoubleOrNull() == null) {
                            errorMessage = "Please enter a valid numeric weight (e.g. 4.5)."
                            return@PetGradientButton
                        }

                        errorMessage = null
                        viewModel.createPetProfile(
                            name = trimmedName,
                            breed = trimmedBreed,
                            age = trimmedAge,
                            weight = trimmedWeight,
                            type = selectedType,
                            imageBase64 = selectedBase64,
                            bitmap = selectedBitmap,
                            onComplete = onSave
                        )
                    }
                )
            }
        }
    }
}
