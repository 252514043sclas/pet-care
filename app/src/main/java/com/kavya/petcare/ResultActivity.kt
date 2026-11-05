package com.kavya.petcare

import android.content.Intent
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
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

class ResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                ResultScreen(
                    viewModel = viewModel,
                    onViewDetails = {
                        startActivity(Intent(this@ResultActivity, DiseaseDetailActivity::class.java))
                    },
                    onBackHome = {
                        startActivity(Intent(this@ResultActivity, HomeDashboardActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun ResultScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onViewDetails: () -> Unit,
    onBackHome: () -> Unit
) {
    val scanResult by viewModel.diagnosticResult.collectAsState()

    val conditionTitle = scanResult?.diagnosisTitle ?: "Minor Skin Allergy"
    val matchPercentage = scanResult?.matchPercentage ?: 88
    val severity = scanResult?.severity ?: "Mild"
    val summary = scanResult?.summary ?: "Based on symptom checklist and photo scan, your pet shows indicators of allergic dermatitis, commonly triggered by environmental factors or food sensitivity."

    Scaffold(
        topBar = {
            PetModernTopBar(
                title = "Analysis Results",
                subtitle = "Diagnostic assessment",
                onBack = onBackHome
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
            // Result Overview Card
            PetModernCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = WarmPearl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MintSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        PetBadge(
                            text = "$matchPercentage% Confidence",
                            containerColor = MintSoft,
                            contentColor = EmeraldPrimary
                        )
                    }

                    Column {
                        Text(
                            text = conditionTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Severity Level:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            PetBadge(
                                text = severity,
                                containerColor = if (severity.equals("mild", true)) MintSoft else AmberAccent.copy(alpha = 0.15f),
                                contentColor = if (severity.equals("mild", true)) EmeraldPrimary else AmberAccent
                            )
                        }
                    }

                    HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }

            // Veterinary notice
            PetModernCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = SoftBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "This AI assessment is an informational aid and does not substitute for licensed veterinary diagnosis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            PetGradientButton(
                text = "View Detailed Disease Info",
                onClick = onViewDetails
            )

            PetOutlinedButton(
                text = "Back to Dashboard",
                onClick = onBackHome
            )
        }
    }
}
