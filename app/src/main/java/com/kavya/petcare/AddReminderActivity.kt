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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme

class AddReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                AddReminderScreen(
                    viewModel = viewModel,
                    onReminderSaved = { finish() },
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun AddReminderScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onReminderSaved: () -> Unit,
    onBack: () -> Unit = {}
) {
    var reminderTitle by remember { mutableStateOf("") }
    var reminderDate by remember { mutableStateOf("") }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PetModernTopBar(
                title = "New Care Reminder",
                subtitle = "Set scheduled reminder",
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
            PetModernCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = WarmPearl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Reminder Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    PetTextField(
                        value = reminderTitle,
                        onValueChange = { reminderTitle = it },
                        label = "Activity / Task",
                        placeholder = "e.g., Bath, Nail Trim, or Coat Brushing",
                        leadingIcon = Icons.Default.Alarm
                    )

                    PetTextField(
                        value = reminderDate,
                        onValueChange = { reminderDate = it },
                        label = "Scheduled Time / Date",
                        placeholder = "e.g., Tomorrow, 5:30 PM",
                        leadingIcon = Icons.Default.Schedule
                    )
                }
            }

            // Notification toggle card
            PetModernCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = WarmPearl
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MintSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Push Notification",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Alert device when due",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldPrimary,
                            checkedTrackColor = MintSage.copy(alpha = 0.35f),
                            uncheckedThumbColor = WarmPearl,
                            uncheckedTrackColor = BorderSubtle
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PetGradientButton(
                text = "Save Reminder",
                enabled = !isSubmitting && reminderTitle.isNotBlank() && reminderDate.isNotBlank(),
                isLoading = isSubmitting,
                onClick = {
                    if (reminderTitle.isNotBlank() && reminderDate.isNotBlank()) {
                        isSubmitting = true
                        viewModel.addReminder(
                            title = reminderTitle,
                            time = reminderDate,
                            onDone = {
                                isSubmitting = false
                                onReminderSaved()
                            }
                        )
                    }
                }
            )
        }
    }
}
