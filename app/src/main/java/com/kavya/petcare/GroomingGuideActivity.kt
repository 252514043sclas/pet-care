package com.kavya.petcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kavya.petcare.ui.theme.PetCareTheme

class GroomingGuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Grooming Guides") })
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Step-by-Step Care Tutorials",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        GuideCard(
                            title = "Coat Brushing & Detangling",
                            steps = "1. Use a slicker brush to remove loose undercoat.\n2. Gently work through tangles without pulling.\n3. Finish with a bristle brush to distribute natural oils."
                        )

                        GuideCard(
                            title = "Safe Nail Trimming",
                            steps = "1. Hold your pet's paw firmly but gently.\n2. Identify the quick (pink blood vessel inside nail).\n3. Clip only the transparent tip at a 45-degree angle."
                        )

                        GuideCard(
                            title = "Ear Cleaning Routine",
                            steps = "1. Apply vet-approved ear cleaning solution.\n2. Gently massage the base of the ear.\n3. Wipe away debris with a clean cotton ball or pad."
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { finish() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Back")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun GuideCard(title: String, steps: String) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = steps,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
