package com.kavya.petcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
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

class SafeVsToxicFoodsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                SafeVsToxicFoodsScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun SafeVsToxicFoodsScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onBack: () -> Unit
) {
    val foodsFromApi by viewModel.safeToxicFoods.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Safe", "Toxic"

    val fallbackFoods = listOf(
        Pair("Carrots", "Safe & crunchy snack beneficial for dental health (Safe)"),
        Pair("Apples (No Seeds)", "Great source of vitamins A & C; remove cores (Safe)"),
        Pair("Chocolate", "Toxic! Contains theobromine, causing cardiac distress (Toxic)"),
        Pair("Grapes & Raisins", "Toxic! Can trigger acute kidney failure even in small doses (Toxic)"),
        Pair("Onions & Garlic", "Toxic! Damages red blood cells, causing anemia (Toxic)"),
        Pair("Plain Cooked Chicken", "Safe lean protein source; avoid seasoning and cooked bones (Safe)"),
        Pair("Macadamia Nuts", "Toxic! Causes muscle tremors, hypothermia, and weakness (Toxic)")
    )

    Scaffold(
        topBar = {
            PetModernTopBar(
                title = "Safe vs Toxic Foods",
                subtitle = "Veterinary dietary guide",
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
            // Filter Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Safe Only", "Toxic Only").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        onClick = { selectedFilter = filter },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) EmeraldPrimary else WarmPearl,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldPrimary else BorderSubtle
                        )
                    ) {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (foodsFromApi.isNotEmpty()) {
                    val filteredApiList = foodsFromApi.filter {
                        val isSafe = it.isSafe == 1
                        when (selectedFilter) {
                            "Safe Only" -> isSafe
                            "Toxic Only" -> !isSafe
                            else -> true
                        }
                    }

                    items(filteredApiList) { foodItem ->
                        val isSafe = foodItem.isSafe == 1
                        FoodCardItem(
                            foodName = foodItem.foodName,
                            description = foodItem.description,
                            isSafe = isSafe
                        )
                    }
                } else {
                    val filteredFallback = fallbackFoods.filter { (_, desc) ->
                        val isSafe = desc.contains("(Safe)")
                        when (selectedFilter) {
                            "Safe Only" -> isSafe
                            "Toxic Only" -> !isSafe
                            else -> true
                        }
                    }

                    items(filteredFallback) { (food, desc) ->
                        val isSafe = desc.contains("(Safe)")
                        val cleanDesc = desc.replace(" (Safe)", "").replace(" (Toxic)", "")
                        FoodCardItem(
                            foodName = food,
                            description = cleanDesc,
                            isSafe = isSafe
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodCardItem(
    foodName: String,
    description: String,
    isSafe: Boolean
) {
    val badgeBg = if (isSafe) MintSoft else AlertRose.copy(alpha = 0.12f)
    val badgeColor = if (isSafe) EmeraldPrimary else AlertRose

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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSafe) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    PetBadge(
                        text = if (isSafe) "SAFE" else "TOXIC",
                        containerColor = badgeBg,
                        contentColor = badgeColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
