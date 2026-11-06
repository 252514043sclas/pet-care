package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.R
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme

class SelectPetTypeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialType = intent.getStringExtra("EXTRA_PET_TYPE") ?: "Dog"
        setContent {
            PetCareTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = SoftBackground) {
                    SelectPetTypeScreen(
                        initialType = initialType,
                        onBack = {
                            if (isTaskRoot) {
                                startActivity(Intent(this@SelectPetTypeActivity, SignUpActivity::class.java))
                            }
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
    initialType: String = "Dog",
    onBack: () -> Unit,
    onContinue: (String) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }

    val petTypes = listOf(
        Triple("Dog", "Loyal companion", R.drawable.img_pet_dog),
        Triple("Cat", "Playful & loving", R.drawable.img_pet_cat),
        Triple("Rabbit", "Gentle hopper", R.drawable.img_pet_rabbit),
        Triple("Bird", "Cheerful singer", R.drawable.img_pet_bird),
        Triple("Hamster", "Curious explorer", R.drawable.img_pet_hamster),
        Triple("Other", "Special friend", R.drawable.img_pet_other)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        PetModernTopBar(
            title = "",
            onBack = onBack
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "What type of pet do you have?",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = EmeraldDark,
            lineHeight = 32.sp
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Choose your animal type to customize their care plan",
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(petTypes) { (type, subtitle, imageRes) ->
                val isSelected = selectedType.equals(type, ignoreCase = true)
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.02f else 1.0f,
                    animationSpec = spring(stiffness = 400f),
                    label = "scale"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                        .scale(scale)
                        .shadow(
                            elevation = if (isSelected) 8.dp else 2.dp,
                            shape = RoundedCornerShape(22.dp),
                            ambientColor = if (isSelected) MintSage.copy(alpha = 0.35f) else Color(0x10000000)
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { selectedType = type },
                    shape = RoundedCornerShape(22.dp),
                    color = if (isSelected) Color(0xFFF0FAF7) else Color.White,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MintSage else BorderSubtle
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isSelected) {
                            Surface(
                                shape = CircleShape,
                                color = MintSage,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(22.dp)
                                    .align(Alignment.TopEnd)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Circular Real Animal Photo Container
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MintSage.copy(alpha = 0.18f) else Color(0xFFF1F5F3))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MintSage else Color(0xFFE2E8F0),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = imageRes),
                                    contentDescription = type,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = type,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = EmeraldDark
                            )

                            Spacer(Modifier.height(2.dp))

                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        PetGradientButton(
            text = "Continue",
            onClick = { onContinue(selectedType) }
        )

        Spacer(Modifier.height(8.dp))
    }
}
