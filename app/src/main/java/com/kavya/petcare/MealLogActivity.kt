package com.kavya.petcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.theme.PetCareTheme

class MealLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                MealLogScreen(
                    viewModel = viewModel,
                    onMealLogged = { finish() }
                )
            }
        }
    }
}

@Composable
fun MealLogScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onMealLogged: () -> Unit
) {
    var mealInput by remember { mutableStateOf("") }
    var portionSize by remember { mutableFloatStateOf(1.0f) }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Log Meal") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Record Today's Meal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = mealInput,
                onValueChange = { mealInput = it },
                label = { Text("Food Item (e.g. Timothy Hay & Greens)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            val calculatedCalories = (portionSize * 150).toInt()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Portion Size: ${String.format("%.1f", portionSize)} Cups (~$calculatedCalories kcal)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = portionSize,
                    onValueChange = { portionSize = it },
                    valueRange = 0.5f..3.0f,
                    steps = 5
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (mealInput.isNotBlank()) {
                        isSubmitting = true
                        viewModel.addMeal(
                            mealName = mealInput,
                            calories = calculatedCalories,
                            time = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date()),
                            onDone = {
                                isSubmitting = false
                                onMealLogged()
                            }
                        )
                    }
                },
                enabled = !isSubmitting && mealInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Log Meal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
