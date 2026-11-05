package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme

class SymptomSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                SymptomSelectionScreen(
                    viewModel = viewModel,
                    onAnalyze = {
                        startActivity(Intent(this@SymptomSelectionActivity, AnalyzingActivity::class.java))
                        finish()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun SymptomSelectionScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onAnalyze: () -> Unit,
    onBack: () -> Unit = {}
) {
    val symptomsFromApi by viewModel.symptomsList.collectAsState()
    val fallbackSymptoms = listOf(
        "Itching / Excessive Scratching",
        "Vomiting or Upset Stomach",
        "Lethargy / Lack of Energy",
        "Loss of Appetite",
        "Coughing or Sneezing",
        "Ear Discharge / Head Shaking",
        "Skin Redness or Bumps"
    )

    val symptoms = if (symptomsFromApi.isNotEmpty()) symptomsFromApi else fallbackSymptoms
    val selectedSymptoms = remember { mutableStateMapOf<String, Boolean>() }
    var isSubmitting by remember { mutableStateOf(false) }

    val selectedCount = selectedSymptoms.values.count { it }

    Scaffold(
        topBar = {
            PetModernTopBar(
                title = "Symptom Checker",
                subtitle = "Select observable signs",
                onBack = onBack
            )
        },
        containerColor = SoftBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Observable Symptoms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Select all signs your pet is showing",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                PetBadge(
                    text = "$selectedCount selected",
                    containerColor = if (selectedCount > 0) MintSoft else SoftBackground,
                    contentColor = if (selectedCount > 0) EmeraldPrimary else TextSecondary
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(symptoms) { symptom ->
                    val isChecked = selectedSymptoms[symptom] ?: false

                    PetModernCard(
                        onClick = {
                            selectedSymptoms[symptom] = !isChecked
                        },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = if (isChecked) MintSoft.copy(alpha = 0.6f) else WarmPearl,
                        borderColor = if (isChecked) MintSage else BorderSubtle
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = symptom,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                color = if (isChecked) EmeraldDark else TextPrimary,
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (isChecked) EmeraldPrimary else SoftBackground)
                                    .then(
                                        if (!isChecked) Modifier.background(Color.Transparent)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Transparent)
                                            .padding(2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PetGradientButton(
                text = "Analyze Symptoms",
                icon = Icons.Default.Search,
                enabled = !isSubmitting,
                isLoading = isSubmitting,
                onClick = {
                    val chosen = selectedSymptoms.filter { it.value }.keys.toList()
                    isSubmitting = true
                    viewModel.runDiagnosticScan(chosen) {
                        isSubmitting = false
                        onAnalyze()
                    }
                }
            )
        }
    }
}
