package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.EmeraldDark
import com.kavya.petcare.ui.MintSage
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
                        onContinue = { selectedType ->
                            val intent = Intent(this@SelectPetTypeActivity, AddPetProfileActivity::class.java).apply {
                                putExtra("EXTRA_PET_TYPE", selectedType)
                            }
                            startActivity(intent)
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
    onContinue: (String) -> Unit
) {
    var selectedType by remember { mutableStateOf("Dog") }
    val petTypes = listOf(
        Triple("Dog", "Loyal companion", Icons.Default.Pets),
        Triple("Cat", "Independent & playful", Icons.Default.Favorite),
        Triple("Rabbit", "Gentle hopper", Icons.Default.Star),
        Triple("Bird", "Cheerful singer", Icons.Default.Notifications),
        Triple("Hamster", "Curious explorer", Icons.Default.Add),
        Triple("Other", "Special friend", Icons.Default.Person)
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
                    tint = EmeraldDark
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "What type of pet do you have?",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldDark
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
            items(petTypes) { (type, subtitle, icon) ->
                val isSelected = selectedType == type
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.03f else 1.0f,
                    animationSpec = spring(stiffness = 400f),
                    label = "scale"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .scale(scale)
                        .clickable { selectedType = type }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MintSage else Color(0xFFE2E8E5),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFE8F5F2) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MintSage.copy(alpha = 0.2f) else Color(0xFFF0F4F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = type,
                                tint = if (isSelected) EmeraldDark else Color.Gray,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            type,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = EmeraldDark
                        )
                        Text(
                            subtitle,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onContinue(selectedType) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MintSage),
            shape = RoundedCornerShape(27.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
