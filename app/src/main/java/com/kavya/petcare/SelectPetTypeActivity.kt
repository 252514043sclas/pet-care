package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.theme.PetCareTheme

class SelectPetTypeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SelectPetTypeScreen(
                        onBack = {
                            startActivity(Intent(this@SelectPetTypeActivity, SignUpActivity::class.java))
                            finish()
                        },
                        onContinue = {
                            startActivity(Intent(this@SelectPetTypeActivity, AddPetProfileActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectPetTypeScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var selectedType by remember { mutableStateOf("Dog") }
    val petTypes = listOf(
        "Dog", "Cat", "Rabbit", "Bird", "Hamster", "Other"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8F3))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF184D43)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "What type of pet do you have?",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF184D43)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Select your pet to customize their care plan",
            color = Color.Gray,
            fontSize = 15.sp
        )

        Spacer(Modifier.height(28.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(petTypes) { type ->
                val isSelected = selectedType == type
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clickable { selectedType = type }
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) Color(0xFF267D6D) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFE8F5F2) else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = type,
                            tint = if (isSelected) Color(0xFF184D43) else Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            type,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isSelected) Color(0xFF184D43) else Color.Black
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF267D6D))
        ) {
            Text("Continue")
        }
    }
}
