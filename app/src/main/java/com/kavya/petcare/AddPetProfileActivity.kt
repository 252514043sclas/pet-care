package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.theme.PetCareTheme

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
                    onSave = {
                        startActivity(Intent(this@AddPetProfileActivity, HomeDashboardActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun AddPetProfileScreen(
    initialType: String = "Rabbit",
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onSave: () -> Unit
) {
    var petName by remember { mutableStateOf(if (initialType == "Rabbit") "Clover" else "Max") }
    var breed by remember { mutableStateOf(if (initialType == "Rabbit") "Holland Lop" else "Golden Retriever") }
    var age by remember { mutableStateOf(if (initialType == "Rabbit") "1.2" else "2.0") }
    var weight by remember { mutableStateOf(if (initialType == "Rabbit") "1.8" else "24.5") }
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8F3))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            "Add Pet Profile",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF184D43)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Tell us more about your ${initialType.lowercase()}",
            color = Color.Gray,
            fontSize = 15.sp
        )

        Spacer(Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = petName,
                    onValueChange = { petName = it },
                    label = { Text("Pet Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Breed") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age (yrs)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.createPetProfile(
                    name = petName,
                    breed = breed,
                    age = "$age yrs",
                    weight = weight,
                    type = initialType,
                    onComplete = onSave
                )
            },
            enabled = !isLoading && petName.isNotBlank() && breed.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF267D6D))
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("Save Profile")
            }
        }
    }
}
