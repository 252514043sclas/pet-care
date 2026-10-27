package com.kavya.petcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kavya.petcare.ui.theme.PetCareTheme

class SafeVsToxicFoodsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val foods = listOf(
                    Pair("✅ Carrots", "Safe & nutritious snack for teeth"),
                    Pair("✅ Apples (No Seeds)", "Great source of vitamins A & C"),
                    Pair("❌ Chocolate", "Toxic! Contains harmful theobromine"),
                    Pair("❌ Grapes & Raisins", "Toxic! Can cause acute kidney failure"),
                    Pair("❌ Onions & Garlic", "Toxic! Damages red blood cells"),
                    Pair("✅ Plain Cooked Chicken", "Safe lean protein source"),
                    Pair("❌ Macadamia Nuts", "Toxic! Causes neurological weakness")
                )

                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Safe vs Toxic Foods") })
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Food Safety Guide",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Check before feeding your pet human foods",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(foods) { (food, desc) ->
                                val isSafe = food.startsWith("✅")
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSafe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    ),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = food,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
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
}
