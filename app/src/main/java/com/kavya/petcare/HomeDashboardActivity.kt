package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
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
import com.kavya.petcare.ui.EmeraldDark
import com.kavya.petcare.ui.MintSage
import com.kavya.petcare.ui.theme.PetCareTheme
import kotlinx.coroutines.launch

class HomeDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                MainSlidingDashboardScreen(
                    onExitApp = { finish() },
                    onNavigatePhotoScan = { startActivity(Intent(this, PhotoScanActivity::class.java)) },
                    onNavigateSymptomSelection = { startActivity(Intent(this, SymptomSelectionActivity::class.java)) },
                    onNavigateDietPlan = { startActivity(Intent(this, DietPlanActivity::class.java)) },
                    onNavigateMealLog = { startActivity(Intent(this, MealLogActivity::class.java)) },
                    onNavigateSafeFoods = { startActivity(Intent(this, SafeVsToxicFoodsActivity::class.java)) },
                    onNavigateWeightTracker = { startActivity(Intent(this, WeightTrackerActivity::class.java)) },
                    onNavigateAddReminder = { startActivity(Intent(this, AddReminderActivity::class.java)) },
                    onNavigateCalendar = { startActivity(Intent(this, GroomingCalendarActivity::class.java)) },
                    onNavigateGuides = { startActivity(Intent(this, GroomingGuideActivity::class.java)) },
                    onAddPet = { startActivity(Intent(this, SelectPetTypeActivity::class.java)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainSlidingDashboardScreen(
    onExitApp: () -> Unit,
    onNavigatePhotoScan: () -> Unit,
    onNavigateSymptomSelection: () -> Unit,
    onNavigateDietPlan: () -> Unit,
    onNavigateMealLog: () -> Unit,
    onNavigateSafeFoods: () -> Unit,
    onNavigateWeightTracker: () -> Unit,
    onNavigateAddReminder: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateGuides: () -> Unit,
    onAddPet: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    var showExitDialog by remember { mutableStateOf(false) }

    // Custom Back Press Logic
    BackHandler(enabled = true) {
        if (pagerState.currentPage != 0) {
            // Return to Home tab if on any other tab
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        } else {
            // On Home tab: Show exit confirmation dialog
            showExitDialog = true
        }
    }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    "Exit Pet Care?",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            },
            text = {
                Text(
                    "Do you want to exit Pet Care?",
                    fontSize = 15.sp,
                    color = Color(0xFF4A5568)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        onExitApp()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE63946)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Exit", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitDialog = false },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Stay", color = EmeraldDark, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF6F8F3)
    ) {
        Scaffold(
            containerColor = Color(0xFFF6F8F3),
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    val tabLabels = listOf("Home", "Health", "Diet", "Care", "Profile")
                    val tabIcons = listOf(Icons.Default.Home, Icons.Default.Favorite, Icons.Default.DateRange, Icons.Default.Notifications, Icons.Default.Person)

                    for (index in 0..4) {
                        val isSelected = pagerState.currentPage == index
                        val label = tabLabels[index]
                        val icon = tabIcons[index]
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = { Icon(icon, contentDescription = label, tint = if (isSelected) MintSage else Color.Gray) },
                            label = { Text(label, color = if (isSelected) MintSage else Color.Gray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { page ->
                when (page) {
                    0 -> HomeTabScreen(
                        onNavigateHealth = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        onNavigateDiet = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                        onNavigateGrooming = { coroutineScope.launch { pagerState.animateScrollToPage(3) } },
                        onNavigateProfile = { coroutineScope.launch { pagerState.animateScrollToPage(4) } },
                        onAddPet = onAddPet
                    )
                    1 -> HealthTabScreen(
                        onNavigatePhotoScan = onNavigatePhotoScan,
                        onNavigateSymptomSelection = onNavigateSymptomSelection
                    )
                    2 -> DietTabScreen(
                        onNavigateDietPlan = onNavigateDietPlan,
                        onNavigateMealLog = onNavigateMealLog,
                        onNavigateSafeFoods = onNavigateSafeFoods,
                        onNavigateWeightTracker = onNavigateWeightTracker
                    )
                    3 -> CareTabScreen(
                        onNavigateAddReminder = onNavigateAddReminder,
                        onNavigateCalendar = onNavigateCalendar,
                        onNavigateGuides = onNavigateGuides
                    )
                    4 -> ProfileTabScreen(
                        onAddPet = onAddPet
                    )
                }
            }
        }
    }
}

@Composable
fun HomeTabScreen(
    onNavigateHealth: () -> Unit,
    onNavigateDiet: () -> Unit,
    onNavigateGrooming: () -> Unit,
    onNavigateProfile: () -> Unit,
    onAddPet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Welcome Back", fontSize = 14.sp, color = Color.Gray)
                Text("Pet Dashboard", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
            }

            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onNavigateProfile() },
                shape = CircleShape,
                color = Color(0xFFE8F5F2)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = MintSage)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateProfile() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldDark)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Star, contentDescription = "Pet", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = Color(0xFFF3EAC2),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ACTIVE PET",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B5B16),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Clover", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Holland Lop Rabbit · 1.2 yrs", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                }

                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Details", tint = Color.White)
            }
        }

        Spacer(Modifier.height(28.dp))

        Text("Quick Care Actions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardActionCard(
                title = "AI Health Scan",
                subtitle = "Check symptoms",
                icon = Icons.Default.Favorite,
                modifier = Modifier.weight(1f),
                onClick = onNavigateHealth
            )
            DashboardActionCard(
                title = "Diet & Meals",
                subtitle = "Calorie tracker",
                icon = Icons.Default.DateRange,
                modifier = Modifier.weight(1f),
                onClick = onNavigateDiet
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardActionCard(
                title = "Grooming & Care",
                subtitle = "Reminders & guides",
                icon = Icons.Default.Notifications,
                modifier = Modifier.weight(1f),
                onClick = onNavigateGrooming
            )
            DashboardActionCard(
                title = "Add New Pet",
                subtitle = "Create profile",
                icon = Icons.Default.Add,
                modifier = Modifier.weight(1f),
                onClick = onAddPet
            )
        }

        Spacer(Modifier.height(28.dp))

        Text("Today's Schedule", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MintSage, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Morning Timothy Hay", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), color = EmeraldDark)
                    Text("8:00 AM", fontSize = 13.sp, color = Color.Gray)
                }
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MintSage, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Fur Brushing & Nail Check", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), color = EmeraldDark)
                    Text("5:30 PM", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun HealthTabScreen(onNavigatePhotoScan: () -> Unit, onNavigateSymptomSelection: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Health & Diagnostics", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
        Spacer(Modifier.height(8.dp))
        Text("AI Symptom Checker & Medical Scan", color = Color.Gray)
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigatePhotoScan() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MintSage)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Icon(Icons.Default.Favorite, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text("AI Camera Scan", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Take a photo of your pet's skin or eyes for instant analysis", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateSymptomSelection() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Icon(Icons.Default.Notifications, contentDescription = "Symptoms", tint = MintSage, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text("Select Symptoms", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                Text("Choose observed symptoms to get preliminary guidance", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun DietTabScreen(
    onNavigateDietPlan: () -> Unit,
    onNavigateMealLog: () -> Unit,
    onNavigateSafeFoods: () -> Unit,
    onNavigateWeightTracker: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Diet & Nutrition", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
        Spacer(Modifier.height(8.dp))
        Text("Calorie Tracker & Food Safety", color = Color.Gray)
        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardActionCard(
                title = "Meal Schedule",
                subtitle = "View daily plan",
                icon = Icons.Default.DateRange,
                modifier = Modifier.weight(1f),
                onClick = onNavigateDietPlan
            )
            DashboardActionCard(
                title = "Log Meal",
                subtitle = "Add food intake",
                icon = Icons.Default.Add,
                modifier = Modifier.weight(1f),
                onClick = onNavigateMealLog
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardActionCard(
                title = "Safe vs Toxic",
                subtitle = "Food reference",
                icon = Icons.Default.Star,
                modifier = Modifier.weight(1f),
                onClick = onNavigateSafeFoods
            )
            DashboardActionCard(
                title = "Weight Tracker",
                subtitle = "Monitor growth",
                icon = Icons.Default.Person,
                modifier = Modifier.weight(1f),
                onClick = onNavigateWeightTracker
            )
        }
    }
}

@Composable
fun CareTabScreen(
    onNavigateAddReminder: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateGuides: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Grooming & Care", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
        Spacer(Modifier.height(8.dp))
        Text("Schedules, Reminders & Guides", color = Color.Gray)
        Spacer(Modifier.height(24.dp))

        DashboardActionCard(
            title = "Add Reminder",
            subtitle = "Schedule grooming, baths & checkups",
            icon = Icons.Default.Add,
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateAddReminder
        )

        Spacer(Modifier.height(16.dp))

        DashboardActionCard(
            title = "Care Calendar",
            subtitle = "View monthly reminders",
            icon = Icons.Default.DateRange,
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateCalendar
        )

        Spacer(Modifier.height(16.dp))

        DashboardActionCard(
            title = "Grooming Guides",
            subtitle = "Tutorials and step-by-step care tips",
            icon = Icons.Default.Notifications,
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateGuides
        )
    }
}

@Composable
fun ProfileTabScreen(onAddPet: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.size(90.dp),
            shape = CircleShape,
            color = Color(0xFFE8F5F2)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = MintSage, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Clover", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
        Text("Holland Lop Rabbit · 1.2 yrs", fontSize = 15.sp, color = Color.Gray)

        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Weight", color = Color.Gray)
                    Text("1.8 kg", fontWeight = FontWeight.Bold, color = EmeraldDark)
                }
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Health Score", color = Color.Gray)
                    Text("95 / 100", fontWeight = FontWeight.Bold, color = MintSage)
                }
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Vaccination Status", color = Color.Gray)
                    Text("Up to Date", fontWeight = FontWeight.Bold, color = EmeraldDark)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onAddPet,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MintSage),
            shape = RoundedCornerShape(26.dp)
        ) {
            Text("Add Another Pet")
        }
    }
}

@Composable
fun DashboardActionCard(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5F2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = MintSage, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
