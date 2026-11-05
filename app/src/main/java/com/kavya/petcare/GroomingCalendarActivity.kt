package com.kavya.petcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
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

class GroomingCalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                GroomingCalendarScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun GroomingCalendarScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onBack: () -> Unit
) {
    val activePet by viewModel.activePet.collectAsState()
    val tasksFromApi by viewModel.calendarTasks.collectAsState()

    val fallbackSchedule = listOf(
        Pair("Oct 12, 10:00 AM", "Bath & Shampoo Session"),
        Pair("Oct 19, 2:00 PM", "Nail Trimming & Filing"),
        Pair("Oct 26, 11:00 AM", "Ear Cleaning & Checkup"),
        Pair("Nov 02, 10:00 AM", "Full Grooming & Haircut")
    )

    Scaffold(
        topBar = {
            PetModernTopBar(
                title = "Grooming Calendar",
                subtitle = "Upcoming care for ${activePet.name}",
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
            PetSectionHeader(title = "Scheduled Tasks")

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (tasksFromApi.isNotEmpty()) {
                    items(tasksFromApi) { taskItem ->
                        CalendarTaskCard(dateTime = taskItem.dateTime, task = taskItem.task)
                    }
                } else {
                    items(fallbackSchedule) { (date, task) ->
                        CalendarTaskCard(dateTime = date, task = task)
                    }
                }
            }

            PetGradientButton(
                text = "Back to Grooming Home",
                onClick = onBack
            )
        }
    }
}

@Composable
private fun CalendarTaskCard(dateTime: String, task: String) {
    PetModernCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = WarmPearl
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MintSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateTime,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MintSage
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            PetBadge(
                text = "Scheduled",
                containerColor = SoftBackground,
                contentColor = EmeraldPrimary
            )
        }
    }
}
