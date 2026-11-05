package com.kavya.petcare

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme

class DiseaseDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                DiseaseDetailScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun DiseaseDetailScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activePet by viewModel.activePet.collectAsState()
    val conditionGuide by viewModel.conditionDetail.collectAsState()

    val conditionName = conditionGuide?.conditionName ?: "Mite-related irritation"
    val causes = conditionGuide?.causes ?: "Contact with affected animals or contaminated bedding may spread mites. Only your vet can confirm the cause."
    val description = conditionGuide?.description ?: "Mites can cause flaky skin, itching or patchy fur in rabbits. Other problems can look similar, so this is a possible explanation—not a diagnosis."
    val vetWarning = conditionGuide?.vetWarning ?: "Book a rabbit-savvy vet exam for skin changes. Seek urgent care for reduced eating, fewer droppings, marked lethargy or painful, broken skin."
    val steps = conditionGuide?.interimSteps ?: listOf(
        "Keep ${activePet.name}’s bedding clean and dry. Avoid new scented products.",
        "Record changes in skin, appetite and droppings for your vet.",
        "Do not apply dog or cat flea products, creams or home remedies."
    )

    Scaffold(
        topBar = {
            PetModernTopBar(
                title = "Condition Guide",
                subtitle = "${activePet.name} • ${activePet.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }}",
                onBack = onBack
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
            PetBadge(
                text = "Educational Reference • Veterinary Exam Required",
                containerColor = AmberAccent.copy(alpha = 0.15f),
                contentColor = AmberAccent
            )

            Text(
                text = conditionName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                lineHeight = 24.sp
            )

            // Causes Card
            PetModernCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = WarmPearl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Potential Causes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = causes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            PetSectionHeader(title = "While Arranging a Vet Visit")

            steps.forEachIndexed { index, stepText ->
                StepCard(stepNumber = "${index + 1}", text = stepText)
            }

            PetSectionHeader(title = "When to See a Vet Immediately")

            // Warning Card
            PetModernCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = AlertRose.copy(alpha = 0.08f),
                borderColor = AlertRose.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = "Warning",
                        tint = AlertRose,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = vetWarning,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AlertRose,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PetOutlinedButton(
                    text = "Save Report",
                    icon = Icons.Default.Bookmark,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.saveReport("Condition Report: $conditionName") {
                            Toast.makeText(context, "Diagnostic report saved to history!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                PetGradientButton(
                    text = "Find Vet",
                    icon = Icons.Default.LocationOn,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        Toast.makeText(context, "Searching for nearby emergency vet clinics...", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Text(
                text = "This guide is for informational triage purposes and does not replace professional veterinary care.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StepCard(stepNumber: String, text: String) {
    PetModernCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = WarmPearl
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MintSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                lineHeight = 20.sp
            )
        }
    }
}
