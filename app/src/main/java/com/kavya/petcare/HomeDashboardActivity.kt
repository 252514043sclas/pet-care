package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.EmeraldDark
import com.kavya.petcare.ui.MintSage
import com.kavya.petcare.ui.theme.PetCareTheme
import kotlinx.coroutines.launch

class HomeDashboardActivity : ComponentActivity() {

    private lateinit var viewModel: PetViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = PetViewModel.getShared(this)
        setContent {
            PetCareTheme {
                MainSlidingDashboardScreen(
                    viewModel = viewModel,
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
                    onAddPet = { startActivity(Intent(this, SelectPetTypeActivity::class.java)) },
                    onNavigateProfile = { startActivity(Intent(this, PetDetailProfileActivity::class.java)) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.refreshDashboard()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainSlidingDashboardScreen(
    viewModel: PetViewModel,
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
    onAddPet: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Pet Care?", fontWeight = FontWeight.Bold, color = EmeraldDark) },
            text = { Text("Do you want to exit Pet Care?", fontSize = 15.sp, color = Color(0xFF4A5568)) },
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
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabLabels = listOf("Home", "Health", "Diet", "Care", "Profile")
                        val tabIcons = listOf(
                            Icons.Default.Home,
                            Icons.Default.Favorite,
                            Icons.Default.DateRange,
                            Icons.Default.Notifications,
                            Icons.Default.Person
                        )

                        for (index in 0..4) {
                            val isSelected = pagerState.currentPage == index
                            val label = tabLabels[index]
                            val icon = tabIcons[index]

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                color = if (isSelected) MintSage.copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) EmeraldDark else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    AnimatedVisibility(
                                        visible = isSelected,
                                        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
                                        exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
                                    ) {
                                        Text(
                                            text = label,
                                            color = EmeraldDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
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
                        viewModel = viewModel,
                        onNavigateHealth = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        onNavigateDiet = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                        onNavigateGrooming = { coroutineScope.launch { pagerState.animateScrollToPage(3) } },
                        onNavigateProfile = onNavigateProfile,
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
                        viewModel = viewModel,
                        onAddPet = onAddPet,
                        onNavigateProfile = onNavigateProfile
                    )
                }
            }
        }
    }
}

@Composable
fun HomeTabScreen(
    viewModel: PetViewModel,
    onNavigateHealth: () -> Unit,
    onNavigateDiet: () -> Unit,
    onNavigateGrooming: () -> Unit,
    onNavigateProfile: () -> Unit,
    onAddPet: () -> Unit
) {
    val activePet by viewModel.activePet.collectAsState()
    val reminders by viewModel.reminders.collectAsState()

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
                Text("WELCOME BACK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MintSage, letterSpacing = 1.5.sp)
                Text("Pet Dashboard", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldDark)
            }

            Surface(
                modifier = Modifier
                    .size(52.dp)
                    .clickable { onNavigateProfile() },
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = MintSage, modifier = Modifier.size(26.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateProfile() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(MintSage, EmeraldDark)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Star, contentDescription = "Pet", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(Modifier.width(18.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = Color(0xFFF3EAC2),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "ACTIVE PET",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6B5B16),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(activePet.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${activePet.breed} ${activePet.type} · ${activePet.age}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Details", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

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

        Spacer(Modifier.height(32.dp))

        Text("Today's Schedule", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (reminders.isEmpty()) {
                    Text("No scheduled care activities for today.", color = Color.Gray, fontSize = 14.sp)
                } else {
                    reminders.take(3).forEachIndexed { index, reminder ->
                        if (index > 0) {
                            HorizontalDivider(color = Color(0xFFF0F4F1))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (index % 2 == 0) Icons.Default.DateRange else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MintSage,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(reminder.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = EmeraldDark)
                            Text(reminder.time, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                    }
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MintSage),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.Favorite, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(16.dp))
                Text("AI Camera Scan", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Take a photo of your pet's skin or eyes for instant analysis", color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateSymptomSelection() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.Notifications, contentDescription = "Symptoms", tint = MintSage, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(16.dp))
                Text("Select Symptoms", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                Text("Choose observed symptoms to get preliminary guidance", color = Color.Gray, fontSize = 15.sp)
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
fun ProfileTabScreen(
    viewModel: PetViewModel,
    onAddPet: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    val activePet by viewModel.activePet.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier
                .size(100.dp)
                .clickable { onNavigateProfile() },
            shape = CircleShape,
            color = Color(0xFFE8F5F2),
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = MintSage, modifier = Modifier.size(52.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(activePet.name, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
        Text("${activePet.breed} ${activePet.type} · ${activePet.age}", fontSize = 15.sp, color = Color.Gray)

        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateProfile() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Weight", color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text(activePet.weight, fontWeight = FontWeight.Bold, color = EmeraldDark)
                }
                HorizontalDivider(color = Color(0xFFF0F4F1))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Health Score", color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text("${activePet.healthScore} / 100", fontWeight = FontWeight.Bold, color = MintSage)
                }
                HorizontalDivider(color = Color(0xFFF0F4F1))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Vaccination Status", color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text(activePet.vaccinationStatus, fontWeight = FontWeight.Bold, color = EmeraldDark)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onAddPet,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MintSage),
            shape = RoundedCornerShape(27.dp)
        ) {
            Text("Add Another Pet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun DashboardActionCard(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5F2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = MintSage, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
