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
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme

class GroomingGuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                Scaffold(
                    topBar = {
                        PetModernTopBar(
                            title = "Grooming Guides",
                            subtitle = "Step-by-step care tutorials",
                            onBack = { finish() }
                        )
                    },
                    containerColor = SoftBackground
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PetSectionHeader(title = "Essential Care Routines")

                        ModernGuideCard(
                            title = "Coat Brushing & Detangling",
                            icon = Icons.Default.Brush,
                            accentColor = EmeraldPrimary,
                            steps = listOf(
                                "Use a gentle slicker brush to remove loose undercoat without scratching the skin.",
                                "Work through tangles starting at the hair tips, gradually moving towards the roots.",
                                "Finish with a soft bristle brush to distribute healthy natural skin oils evenly."
                            )
                        )

                        ModernGuideCard(
                            title = "Safe Nail Trimming",
                            icon = Icons.Default.ContentCut,
                            accentColor = AmberAccent,
                            steps = listOf(
                                "Hold paw firmly but gently in a well-lit room with your pet in a relaxed posture.",
                                "Identify the pink quick (blood vessel) and stay at least 2mm away from it.",
                                "Clip small slices at a 45-degree angle. Keep styptic powder on hand for emergencies."
                            )
                        )

                        ModernGuideCard(
                            title = "Ear Cleaning Routine",
                            icon = Icons.Default.Hearing,
                            accentColor = SkyBlue,
                            steps = listOf(
                                "Apply a few drops of vet-approved ear cleaning solution into the ear canal.",
                                "Gently massage the base of the ear for 20-30 seconds to dissolve built-up wax.",
                                "Wipe away loose debris with clean cotton pads. Never insert cotton swabs inside."
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        PetGradientButton(
                            text = "Back to Grooming Home",
                            onClick = { finish() }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ModernGuideCard(
        title: String,
        icon: ImageVector,
        accentColor: Color,
        steps: List<String>
    ) {
        PetModernCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = WarmPearl
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                steps.forEachIndexed { index, stepText ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MintSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                        Text(
                            text = stepText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
