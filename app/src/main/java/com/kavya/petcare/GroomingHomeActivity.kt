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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
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

class GroomingHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                Scaffold(
                    topBar = {
                        PetModernTopBar(
                            title = "Grooming & Care",
                            subtitle = "Schedules, reminders & guides",
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
                        PetModernCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MintSoft
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Care & Hygiene Hub",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDark
                                    )
                                    PetBadge(
                                        text = "Active Schedule",
                                        containerColor = WarmPearl,
                                        contentColor = EmeraldPrimary
                                    )
                                }
                                Text(
                                    text = "Maintain coat health, clean ears, trim nails, and track recurring hygiene routines.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        PetSectionHeader(title = "Care Management")

                        GroomingNavCard(
                            title = "Add Care Reminder",
                            subtitle = "Set scheduled push alerts for baths, dental care, and nail trims",
                            icon = Icons.Default.Alarm,
                            accentColor = EmeraldPrimary,
                            onClick = {
                                startActivity(Intent(this@GroomingHomeActivity, AddReminderActivity::class.java))
                            }
                        )

                        GroomingNavCard(
                            title = "Grooming Calendar",
                            subtitle = "View upcoming hygiene appointments and care milestones",
                            icon = Icons.Default.CalendarToday,
                            accentColor = SkyBlue,
                            onClick = {
                                startActivity(Intent(this@GroomingHomeActivity, GroomingCalendarActivity::class.java))
                            }
                        )

                        GroomingNavCard(
                            title = "Grooming Tutorials",
                            subtitle = "Step-by-step veterinary guides for fur, teeth, ears & claws",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            accentColor = AmberAccent,
                            onClick = {
                                startActivity(Intent(this@GroomingHomeActivity, GroomingGuideActivity::class.java))
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun GroomingNavCard(
        title: String,
        subtitle: String,
        icon: ImageVector,
        accentColor: Color,
        onClick: () -> Unit
    ) {
        PetModernCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            containerColor = WarmPearl
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
