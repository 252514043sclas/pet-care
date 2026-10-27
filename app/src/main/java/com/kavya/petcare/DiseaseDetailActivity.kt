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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.theme.PetCareTheme

class DiseaseDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFBF9F1) // Warm cream background matching screenshot
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        // Top bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF1E3D3B)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "CLOVER · RABBIT",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF2E6F65),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Condition guide",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3D3B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Pill badge
                        Surface(
                            color = Color(0xFFF3EAC2),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Example only · Vet exam needed",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF6B5B16),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Title
                        Text(
                            text = "Mite-related irritation",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3D3B)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Description
                        Text(
                            text = "Mites can cause flaky skin, itching or patchy fur in rabbits. Other problems can look similar, so this is a possible explanation—not a diagnosis.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF4A5568),
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Cause Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "What can cause it?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3D3B)
                                )
                                Text(
                                    text = "Contact with affected animals or contaminated bedding may spread mites. Only your vet can confirm the cause.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF4A5568),
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // While you arrange a vet visit
                        Text(
                            text = "While you arrange a vet visit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3D3B)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Step 1
                        StepItem(
                            stepNumber = "1",
                            text = "Keep Clover’s bedding clean and dry. Avoid new scented products."
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Step 2
                        StepItem(
                            stepNumber = "2",
                            text = "Record changes in skin, appetite and droppings for your vet."
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Step 3
                        StepItem(
                            stepNumber = "3",
                            text = "Do not apply dog or cat flea products, creams or home remedies."
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // When to see a vet
                        Text(
                            text = "When to see a vet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B263E)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Alert Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFF7EBE8),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = "Warning",
                                    tint = Color(0xFFB8324E),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Book a rabbit-savvy vet exam for skin changes. Seek urgent care for reduced eating, fewer droppings, marked lethargy or painful, broken skin.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF8B263E),
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Bottom Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedButton(
                                onClick = { /* Save Report */ },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E3D3B))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Save",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Report", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = { /* Find Vet */ },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E564F))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Find Vet",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Find Vet", fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Not a replacement for a vet.",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF718096),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StepItem(stepNumber: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFD3EBE7)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color(0xFF1E564F),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF4A5568),
            lineHeight = 20.sp
        )
    }
}
