package com.kavya.petcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.EmeraldDark
import com.kavya.petcare.ui.MintSage
import com.kavya.petcare.ui.theme.PetCareTheme

class PetDetailProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                PetDetailProfileScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun PetDetailProfileScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onBack: () -> Unit
) {
    val activePet by viewModel.activePet.collectAsState()

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = EmeraldDark
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Pet Profile Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            }

            Spacer(Modifier.height(24.dp))

            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color(0xFFE8F5F2)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Pet Avatar",
                        tint = MintSage,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = activePet.name,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldDark
            )

            Text(
                text = "${activePet.breed} ${activePet.type}",
                fontSize = 16.sp,
                color = Color.Gray
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

            Spacer(Modifier.height(20.dp))

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
                        "Vaccinations: ${activePet.vaccinationStatus}. Health checkup score: ${activePet.healthScore}/100.",
                        color = Color(0xFF4A5568),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF267D6D))
            ) {
                Text("Back to Dashboard", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 15.sp)
        Text(text = value, fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 15.sp)
    }
}
