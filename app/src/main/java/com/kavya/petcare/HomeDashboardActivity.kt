package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.components.PetAvatar
import com.kavya.petcare.ui.components.UserAvatar
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
                    onNavigateDietPlan = { startActivity(Intent(this, DietPlanActivity::class.java)) },
                    onNavigateMealLog = { startActivity(Intent(this, MealLogActivity::class.java)) },
                    onNavigateSafeFoods = { startActivity(Intent(this, SafeVsToxicFoodsActivity::class.java)) },
                    onNavigateWeightTracker = { startActivity(Intent(this, WeightTrackerActivity::class.java)) },
                    onNavigateAddReminder = { startActivity(Intent(this, AddReminderActivity::class.java)) },
                    onNavigateCalendar = { startActivity(Intent(this, GroomingCalendarActivity::class.java)) },
                    onNavigateGuides = { startActivity(Intent(this, GroomingGuideActivity::class.java)) },
                    onNavigateAIChat = { startActivity(Intent(this, AIChatActivity::class.java)) },
                    onAddPet = { startActivity(Intent(this, SelectPetTypeActivity::class.java)) },
                    onNavigateProfile = { startActivity(Intent(this, PetDetailProfileActivity::class.java)) },
                    onNavigateSettings = { startActivity(Intent(this, SettingsActivity::class.java)) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!com.kavya.petcare.data.session.SessionManager.getInstance(this).isLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }
        if (::viewModel.isInitialized) {
            viewModel.refreshDashboard()
        }
    }
}

@Composable
fun MainSlidingDashboardScreen(
    viewModel: PetViewModel,
    onExitApp: () -> Unit,
    onNavigateDietPlan: () -> Unit,
    onNavigateMealLog: () -> Unit,
    onNavigateSafeFoods: () -> Unit,
    onNavigateWeightTracker: () -> Unit,
    onNavigateAddReminder: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateGuides: () -> Unit,
    onNavigateAIChat: () -> Unit,
    onAddPet: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateSettings: () -> Unit
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
            text = { Text("Are you sure you want to close Pet Care?", fontSize = 15.sp, color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        onExitApp()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRose),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Exit", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitDialog = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Stay", color = EmeraldDark, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = WarmPearl,
            shape = RoundedCornerShape(22.dp)
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SoftBackground
    ) {
        Scaffold(
            containerColor = SoftBackground,
            floatingActionButton = {
                if (pagerState.currentPage != 1) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(24.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        icon = {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MintLight)
                        },
                        text = {
                            Text("Ask AI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.End,
            bottomBar = {
                // Floating Island Modern Bottom Navigation Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = Color(0x1A0D3B2E),
                            spotColor = Color(0x2E0D3B2E)
                        ),
                    shape = RoundedCornerShape(32.dp),
                    color = WarmPearl,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabLabels = listOf("Home", "AI Pet Care", "Diet", "Care", "Profile")
                        val tabIcons = listOf(
                            Icons.Default.Home,
                            Icons.Default.AutoAwesome,
                            Icons.Default.Restaurant,
                            Icons.Default.CalendarToday,
                            Icons.Default.Person
                        )

                        for (index in 0..4) {
                            val isSelected = pagerState.currentPage == index
                            val label = tabLabels[index]
                            val icon = tabIcons[index]

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                color = if (isSelected) MintSoft else Color.Transparent,
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) MintSage else TextMuted,
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
                        onNavigateSettings = onNavigateSettings,
                        onAddPet = onAddPet
                    )
                    1 -> {
                        val aiChatViewModel: AIChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        AIChatScreen(
                            viewModel = aiChatViewModel,
                            onBack = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )
                    }
                    2 -> DietTabScreen(
                        viewModel = viewModel,
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
                        onNavigateProfile = onNavigateProfile,
                        onNavigateSettings = onNavigateSettings
                    )
                }
            }
        }
    }
}

// ============================================================================
// Tab 0: Home Dashboard
// ============================================================================
@Composable
fun HomeTabScreen(
    viewModel: PetViewModel,
    onNavigateHealth: () -> Unit,
    onNavigateDiet: () -> Unit,
    onNavigateGrooming: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateSettings: () -> Unit,
    onAddPet: () -> Unit
) {
    val activePet by viewModel.activePet.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val allPets by viewModel.allPets.collectAsState()
    var showSwitchPetDialog by remember { mutableStateOf(false) }

    if (showSwitchPetDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchPetDialog = false },
            title = {
                Text("Select Active Pet", fontWeight = FontWeight.Bold, color = EmeraldDark)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    allPets.forEach { pet ->
                        val isActive = pet.id.toString() == activePet.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.selectActivePet(pet)
                                    showSwitchPetDialog = false
                                },
                            color = if (isActive) Color(0xFFF0FAF7) else Color.White,
                            border = BorderStroke(if (isActive) 1.5.dp else 1.dp, if (isActive) MintSage else BorderSubtle),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    PetAvatar(
                                        imageUrl = pet.imageUrl,
                                        species = pet.species,
                                        petName = pet.name,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(pet.name, fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 15.sp)
                                        Text("${pet.species} • ${pet.breed}", fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                                if (isActive) {
                                    PetBadge(text = "ACTIVE", color = MintSage, textColor = EmeraldDark)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSwitchPetDialog = false
                        onAddPet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintSage),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Pet", fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchPetDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = WarmPearl,
            shape = RoundedCornerShape(22.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Modern Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MintSage)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "WELCOME BACK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MintSage,
                        letterSpacing = 1.2.sp
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Pet Dashboard",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onNavigateSettings() },
                    shape = CircleShape,
                    color = WarmPearl,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = EmeraldDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                val userAvatarUrl by viewModel.userAvatarUrl.collectAsState()
                UserAvatar(
                    avatarUrl = userAvatarUrl,
                    userName = "User",
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onNavigateSettings() }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Hero Active Pet Gradient Card
        if (activePet.hasPet) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = MintSage.copy(alpha = 0.3f),
                        spotColor = EmeraldDark.copy(alpha = 0.45f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .clickable { onNavigateProfile() },
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HeroCardGradient)
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pet Avatar with double glow ring
                        Surface(
                            modifier = Modifier.size(70.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.25f),
                            border = BorderStroke(2.5.dp, Color.White.copy(alpha = 0.8f))
                        ) {
                            PetAvatar(
                                imageUrl = activePet.imageUrl,
                                species = activePet.type,
                                petName = activePet.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE PET",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                if (allPets.size > 1) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { showSwitchPetDialog = true }
                                    ) {
                                        Text(
                                            text = "Switch ▾",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = activePet.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "${activePet.breed} ${activePet.type} • ${activePet.age}",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "View Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = MintSage.copy(alpha = 0.3f),
                        spotColor = EmeraldDark.copy(alpha = 0.45f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .clickable { onAddPet() },
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HeroCardGradient)
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.25f),
                            border = BorderStroke(2.5.dp, Color.White.copy(alpha = 0.8f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Pets,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "GET STARTED",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "No Pet Added Yet",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Tap here to add your first pet profile",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Pet",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // Quick Care Actions Header
        PetSectionHeader(
            title = "Quick Care Actions",
            subtitle = "Daily essentials & diagnostics"
        )

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DashboardActionCard(
                title = "AI Pet Care",
                subtitle = "Ask AI Assistant",
                icon = Icons.Default.AutoAwesome,
                accentColor = MintSage,
                modifier = Modifier.weight(1f),
                onClick = onNavigateHealth
            )
            DashboardActionCard(
                title = "Diet & Meals",
                subtitle = "Calorie tracker",
                icon = Icons.Default.Restaurant,
                accentColor = AmberAccent,
                modifier = Modifier.weight(1f),
                onClick = onNavigateDiet
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DashboardActionCard(
                title = "Grooming & Care",
                subtitle = "Reminders & guides",
                icon = Icons.Default.CalendarToday,
                accentColor = SkyBlue,
                modifier = Modifier.weight(1f),
                onClick = onNavigateGrooming
            )
            DashboardActionCard(
                title = "Add New Pet",
                subtitle = "Create profile",
                icon = Icons.Default.Add,
                accentColor = MintSage,
                modifier = Modifier.weight(1f),
                onClick = onAddPet
            )
        }

        Spacer(Modifier.height(28.dp))

        // Today's Schedule Header
        PetSectionHeader(
            title = "Today's Schedule",
            subtitle = "Active care routine",
            actionText = "Add Task",
            onAction = onNavigateGrooming
        )

        Spacer(Modifier.height(12.dp))

        PetModernCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (reminders.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MintSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Pets, contentDescription = null, tint = MintSage, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("All caught up!", fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 15.sp)
                            Text("No pending reminders for today", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                } else {
                    reminders.take(3).forEachIndexed { index, reminder ->
                        if (index > 0) {
                            HorizontalDivider(color = BorderSubtle)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (index % 2 == 0) MintSoft else AmberSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (index % 2 == 0) Icons.Default.Restaurant else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (index % 2 == 0) MintSage else AmberAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminder.title,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark,
                                    fontSize = 14.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SoftBackground,
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text(
                                    text = reminder.time,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ============================================================================
// Modern Quick Action Card with Accent Tint
// ============================================================================
@Composable
fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    PetModernCard(
        modifier = modifier.height(138.dp),
        onClick = onClick,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================================================
// Tab 2: Diet & Nutrition
// ============================================================================
@Composable
fun DietTabScreen(
    viewModel: PetViewModel,
    onNavigateDietPlan: () -> Unit,
    onNavigateMealLog: () -> Unit,
    onNavigateSafeFoods: () -> Unit,
    onNavigateWeightTracker: () -> Unit
) {
    val dietSummary by viewModel.dietSummary.collectAsState()
    val activePet by viewModel.activePet.collectAsState()
    val targetCalories = dietSummary?.targetCalories ?: 0
    val consumedCalories = dietSummary?.consumedCalories ?: 0
    val progressRatio = if (targetCalories > 0) (consumedCalories.toFloat() / targetCalories.toFloat()).coerceIn(0f, 1f) else 0f
    val percentMet = if (targetCalories > 0) ((consumedCalories.toFloat() / targetCalories.toFloat()) * 100).toInt() else 0
    val hasSchedule = (dietSummary?.hasSchedule == true) || (targetCalories > 0)

    LaunchedEffect(activePet.id) {
        val pId = activePet.id.toIntOrNull()
        viewModel.loadMealSchedules(pId)
        viewModel.loadDietSummary(pId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Diet & Nutrition",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = EmeraldDark
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Calorie tracking & food safety guide",
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(20.dp))

        // Daily Calorie Summary Card
        PetModernCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AmberSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Daily Calorie Target",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark,
                            fontSize = 16.sp
                        )
                    }
                    PetBadge(text = "$percentMet%", color = AmberAccent, textColor = Color(0xFF92400E))
                }
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = AmberAccent,
                    trackColor = AmberSoft
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Consumed: $consumedCalories kcal", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    Text("Target: $targetCalories kcal", fontSize = 13.sp, color = TextMuted)
                }
                if (!hasSchedule) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Set up your feeding schedule to view your daily calorie target.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DashboardActionCard(
                title = "Meal Schedule",
                subtitle = "Daily meal plan",
                icon = Icons.Default.AccessTime,
                accentColor = MintSage,
                modifier = Modifier.weight(1f),
                onClick = onNavigateDietPlan
            )
            DashboardActionCard(
                title = "Log Meal",
                subtitle = "Add food intake",
                icon = Icons.Default.Add,
                accentColor = EmeraldPrimary,
                modifier = Modifier.weight(1f),
                onClick = onNavigateMealLog
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DashboardActionCard(
                title = "Safe vs Toxic",
                subtitle = "Food reference",
                icon = Icons.Default.Shield,
                accentColor = AlertRose,
                modifier = Modifier.weight(1f),
                onClick = onNavigateSafeFoods
            )
            DashboardActionCard(
                title = "Weight Tracker",
                subtitle = "Growth history",
                icon = Icons.Default.MonitorWeight,
                accentColor = SkyBlue,
                modifier = Modifier.weight(1f),
                onClick = onNavigateWeightTracker
            )
        }
    }
}

// ============================================================================
// Tab 3: Care & Grooming
// ============================================================================
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
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Grooming & Routine",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = EmeraldDark
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Schedules, alerts & step-by-step tutorials",
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(24.dp))

        CareActionRow(
            title = "Add Care Reminder",
            subtitle = "Schedule baths, brushings, nail trims & vet visits",
            icon = Icons.Default.AddAlarm,
            accentColor = MintSage,
            onClick = onNavigateAddReminder
        )

        Spacer(Modifier.height(14.dp))

        CareActionRow(
            title = "Care Calendar",
            subtitle = "View monthly reminders and completed tasks",
            icon = Icons.Default.CalendarToday,
            accentColor = SkyBlue,
            onClick = onNavigateCalendar
        )

        Spacer(Modifier.height(14.dp))

        CareActionRow(
            title = "Grooming Guides",
            subtitle = "Step-by-step coat care and hygiene tutorials",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            accentColor = LavenderAccent,
            onClick = onNavigateGuides
        )
    }
}

@Composable
fun CareActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    PetModernCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 16.sp)
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

// ============================================================================
// Tab 4: Profile Tab
// ============================================================================
@Composable
fun ProfileTabScreen(
    viewModel: PetViewModel,
    onAddPet: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val context = LocalContext.current
    val activePet by viewModel.activePet.collectAsState()

    if (!activePet.hasPet) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Surface(
                modifier = Modifier
                    .size(112.dp)
                    .shadow(10.dp, CircleShape, ambientColor = MintSage.copy(alpha = 0.25f)),
                shape = CircleShape,
                color = MintSoft,
                border = BorderStroke(3.dp, MintSage.copy(alpha = 0.35f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        tint = MintSage,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "No pets added yet!",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldDark
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Add your first pet to get started with personalized pet care.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(Modifier.height(36.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PetGradientButton(
                    text = "Add Your First Pet",
                    icon = Icons.Default.Add,
                    onClick = onAddPet
                )

                PetOutlinedButton(
                    text = "App Settings & Account",
                    icon = Icons.Default.Settings,
                    onClick = onNavigateSettings
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .size(106.dp)
                    .shadow(10.dp, CircleShape, ambientColor = MintSage.copy(alpha = 0.3f))
                    .clip(CircleShape)
                    .clickable { onNavigateProfile() },
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(3.dp, MintSage.copy(alpha = 0.3f))
            ) {
                PetAvatar(
                    imageUrl = activePet.imageUrl,
                    species = activePet.type,
                    petName = activePet.name,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = activePet.name,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldDark
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                PetBadge(text = activePet.type.uppercase(), color = MintSage)
                Text("•", color = TextMuted)
                Text(activePet.breed, fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(24.dp))

            // 3-Stat Pill Card
            PetModernCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateProfile() }
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Weight", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(activePet.weight, fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 15.sp)
                    }
                    HorizontalDivider(color = BorderSubtle)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { context.startActivity(Intent(context, HealthScoreDetailActivity::class.java)) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Health Score", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        val scoreText = if (activePet.healthScore > 0) "${activePet.healthScore} / 100" else "No data"
                        Text(scoreText, fontWeight = FontWeight.Bold, color = MintSage, fontSize = 15.sp)
                    }
                    HorizontalDivider(color = BorderSubtle)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { context.startActivity(Intent(context, VaccinationDetailActivity::class.java)) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vaccination Status", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        val vaccineText = if (activePet.vaccinationStatus.isNotBlank()) activePet.vaccinationStatus else "No Records"
                        Text(vaccineText, fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 15.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            PetGradientButton(
                text = "Add Another Pet",
                icon = Icons.Default.Add,
                onClick = onAddPet
            )

            Spacer(Modifier.height(12.dp))

            PetOutlinedButton(
                text = "App Settings & Account",
                icon = Icons.Default.Settings,
                onClick = onNavigateSettings
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}
