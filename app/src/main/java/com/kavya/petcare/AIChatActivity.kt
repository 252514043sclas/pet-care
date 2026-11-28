package com.kavya.petcare

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kavya.petcare.data.models.ChatConversationDto
import com.kavya.petcare.data.models.ChatMessageDto
import com.kavya.petcare.data.models.PetDto
import com.kavya.petcare.data.models.SearchSourceDto
import com.kavya.petcare.data.models.VisionMetadataDto
import com.kavya.petcare.data.network.NetworkConfig
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.components.PetAvatar
import com.kavya.petcare.ui.components.PetImageLoader
import com.kavya.petcare.ui.theme.PetCareTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AIChatActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                val viewModel: AIChatViewModel = viewModel()
                AIChatScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    viewModel: AIChatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by viewModel.messages.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val activeConversation by viewModel.activeConversation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedImageBitmap by viewModel.selectedImageBitmap.collectAsState()
    val activePet by viewModel.activePet.collectAsState()
    val userPets by viewModel.userPets.collectAsState()
    val systemHealth by viewModel.systemHealth.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showPetPickerDialog by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<ChatConversationDto?>(null) }

    // Re-check active pet on resume in case user switched pet in another screen
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadActivePet()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.selectImage(bitmap)
        }
    }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                viewModel.selectImage(bitmap)
            } catch (e: Exception) {
                // handle image load error
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadActivePet()
        viewModel.loadConversations()
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Clear Chat Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Current Chat?", fontWeight = FontWeight.Bold, color = EmeraldDark) },
            text = { Text("This will permanently remove all messages and analyzed images from this session.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRose),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearConfirmDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = EmeraldDark)
                }
            },
            containerColor = WarmPearl,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Specific Conversation Confirmation Dialog
    if (conversationToDelete != null) {
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("Delete Conversation?", fontWeight = FontWeight.Bold, color = EmeraldDark) },
            text = { Text("Are you sure you want to delete '${conversationToDelete?.title}'? This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        val id = conversationToDelete?.id
                        conversationToDelete = null
                        if (id != null) {
                            viewModel.deleteConversation(id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRose),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { conversationToDelete = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = EmeraldDark)
                }
            },
            containerColor = WarmPearl,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Switch Pet Dialog (when user has multiple pets)
    if (showPetPickerDialog && userPets.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showPetPickerDialog = false },
            title = { Text("Select Pet for AI Care", fontWeight = FontWeight.Bold, color = EmeraldDark) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    userPets.forEach { petItem ->
                        val isCurrent = activePet?.id == petItem.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectPet(petItem)
                                    showPetPickerDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) MintSoft else SoftBackground,
                            border = BorderStroke(1.dp, if (isCurrent) MintSage else BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MintSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!petItem.imageUrl.isNullOrBlank()) {
                                        ChatNetworkImage(imageUrl = petItem.imageUrl, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Icon(Icons.Default.Pets, contentDescription = null, tint = MintSage, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(petItem.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmeraldDark)
                                    Text("${petItem.species} • ${petItem.breed} • ${petItem.ageYears}", fontSize = 11.5.sp, color = TextSecondary)
                                }
                                if (isCurrent) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = MintSage, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPetPickerDialog = false }) {
                    Text("Close", color = EmeraldDark)
                }
            },
            containerColor = WarmPearl,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Chat History BottomSheet
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = WarmPearl,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Chat History", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                    }

                    // Start New Chat Button
                    Button(
                        onClick = {
                            viewModel.startNewChat()
                            showHistorySheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintSage),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Chat", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (conversations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No chat history yet", fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            Text("Your past conversations will appear here.", fontSize = 12.sp, color = TextMuted)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(conversations, key = { it.id }) { conv ->
                            val isSelected = activeConversation?.id == conv.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectConversation(conv)
                                        showHistorySheet = false
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MintSoft else SoftBackground,
                                border = BorderStroke(1.dp, if (isSelected) MintSage else BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = conv.title.ifBlank { "Pet Care Chat" },
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = EmeraldDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!conv.lastMessage.isNullOrBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = conv.lastMessage,
                                                fontSize = 11.5.sp,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = "${conv.messageCount} messages • ${conv.updatedAt?.take(16) ?: conv.createdAt?.take(16) ?: ""}",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }

                                    IconButton(
                                        onClick = { conversationToDelete = conv }
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertRose, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = WarmPearl,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EmeraldDark)
                        }

                        Spacer(Modifier.width(2.dp))

                        // Pet avatar
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MintSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            if (activePet != null) {
                                PetAvatar(
                                    imageUrl = activePet?.imageUrl,
                                    species = activePet?.species ?: "Dog",
                                    petName = activePet?.name ?: "Pet",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.Pets, contentDescription = null, tint = MintSage)
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PetCare AI",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                                Spacer(Modifier.width(5.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (systemHealth?.aiService?.modelLoaded == true) Color(0xFF10B981) else Color(0xFFF59E0B))
                                )
                            }
                            val sessionSubtitle = activeConversation?.title ?: "New Chat • Qwen3-VL 2B"
                            Text(
                                text = sessionSubtitle,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // New Chat Button
                        IconButton(
                            onClick = { viewModel.startNewChat() }
                        ) {
                            Icon(Icons.Default.AddComment, contentDescription = "New Chat", tint = EmeraldDark)
                        }

                        // History Drawer Button
                        IconButton(
                            onClick = {
                                viewModel.loadConversations()
                                showHistorySheet = true
                            }
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Chat History", tint = EmeraldDark)
                        }

                        // Clear current messages
                        IconButton(onClick = { showClearConfirmDialog = true }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Session", tint = TextMuted)
                        }
                    }
                }
            }
        },
        containerColor = SoftBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Active Connected Pet Profile Banner
            ActivePetHeader(
                pet = activePet,
                hasMultiplePets = userPets.size > 1,
                onSwitchPet = { showPetPickerDialog = true }
            )

            // Error banner if any
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = AlertRoseSoft,
                    border = BorderStroke(1.dp, AlertRose.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRose, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "Operation failed",
                                fontSize = 12.sp,
                                color = AlertRose,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = { viewModel.retryLast() }) {
                            Text("Retry", color = AlertRose, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Chat Messages List or Clean Welcome Empty View (No Suggested Questions)
            if (messages.isEmpty()) {
                WelcomeEmptyView(
                    pet = activePet,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(msg = msg, onOpenUrl = { url ->
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(browserIntent)
                            } catch (e: Exception) {
                                // ignore url open error
                            }
                        })
                    }

                    if (isLoading) {
                        item {
                            AITypingIndicator(petName = activePet?.name ?: "your pet")
                        }
                    }
                }
            }

            // Image Preview Bar (when an image is selected)
            AnimatedVisibility(visible = selectedImageBitmap != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = WarmPearl,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.5.dp, MintSage)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            selectedImageBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Selected Photo",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Pet Photo Ready for Vision Analysis", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldDark)
                                Text("Tap Send to analyze with Qwen3-VL 2B", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        IconButton(onClick = { viewModel.clearSelectedImage() }, enabled = !isLoading) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = AlertRose)
                        }
                    }
                }
            }

            // Bottom Input Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding(),
                color = WarmPearl,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Camera Action
                    IconButton(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.size(38.dp),
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Take Photo",
                            tint = if (!isLoading) MintSage else TextMuted
                        )
                    }

                    // Gallery Action
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.size(38.dp),
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Pick Image",
                            tint = if (!isLoading) MintSage else TextMuted
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // Text Input Field
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp, max = 110.dp),
                        enabled = !isLoading,
                        placeholder = {
                            Text(
                                text = if (isLoading) "Waiting for AI response..."
                                else if (selectedImageBitmap != null) "Add notes for visual scan..."
                                else "Ask about ${activePet?.name ?: "your pet"} in simple English...",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        },
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintSage,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SoftBackground,
                            unfocusedContainerColor = SoftBackground,
                            disabledContainerColor = SoftBackground.copy(alpha = 0.6f),
                            disabledBorderColor = BorderSubtle.copy(alpha = 0.6f)
                        ),
                        maxLines = 4
                    )

                    Spacer(Modifier.width(6.dp))

                    // Send Button
                    val canSend = (textInput.isNotBlank() || selectedImageBitmap != null) && !isLoading
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(enabled = canSend) {
                                keyboardController?.hide()
                                val prompt = textInput
                                textInput = ""
                                viewModel.sendMessage(prompt)
                            },
                        shape = CircleShape,
                        color = if (canSend) MintSage else BorderSubtle,
                        shadowElevation = if (canSend) 4.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (canSend) Color.White else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeEmptyView(
    pet: PetDto?,
    modifier: Modifier = Modifier
) {
    val petName = pet?.name ?: "your pet"
    Box(
        modifier = modifier
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MintSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MintSage,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Hello! How can I help you take care of $petName today?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldDark,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Ask any questions about ${petName}'s diet, health, symptoms, or care in simple English.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun ChatMessageBubble(
    msg: ChatMessageDto,
    onOpenUrl: (String) -> Unit
) {
    val isUser = msg.sender.equals("user", ignoreCase = true)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MintSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MintSage, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 310.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = if (isUser) EmeraldPrimary else WarmPearl,
                shadowElevation = if (isUser) 2.dp else 3.dp,
                border = if (!isUser) BorderStroke(1.dp, BorderSubtle) else null
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Display uploaded image if present
                    if (!msg.imageUrl.isNullOrBlank()) {
                        val fullImageUrl = if (msg.imageUrl.startsWith("http")) {
                            msg.imageUrl
                        } else {
                            NetworkConfig.DEFAULT_BASE_URL.replace("/api/", "") + msg.imageUrl
                        }
                        ChatNetworkImage(
                            imageUrl = fullImageUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    // Structured Multimodal Vision Card
                    if (msg.analysisMetadata != null) {
                        VisionAnalysisCard(meta = msg.analysisMetadata)
                        Spacer(Modifier.height(8.dp))
                    }

                    // Message text
                    Text(
                        text = msg.message,
                        color = if (isUser) Color.White else TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )

                    // Citing live web search sources if present
                    if (msg.sources.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = BorderSubtle)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Verified Veterinary Sources:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                        Spacer(Modifier.height(6.dp))
                        msg.sources.forEach { source ->
                            SearchSourceChip(source = source, onClick = { onOpenUrl(source.url) })
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = msg.createdAt ?: "",
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun VisionAnalysisCard(meta: VisionMetadataDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SoftBackground,
        border = BorderStroke(1.dp, MintSage.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Qwen3-VL Vision Clinical Triage",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )

                // Severity Tag
                val severity = meta.severity ?: "Moderate"
                val (tagColor, tagBg) = when {
                    severity.contains("Emergency", ignoreCase = true) -> AlertRose to AlertRoseSoft
                    severity.contains("High", ignoreCase = true) -> Color(0xFFEA580C) to Color(0xFFFFEDD5)
                    severity.contains("Low", ignoreCase = true) -> Color(0xFF16A34A) to Color(0xFFDCFCE7)
                    else -> Color(0xFFD97706) to Color(0xFFFEF3C7)
                }

                Surface(shape = RoundedCornerShape(8.dp), color = tagBg) {
                    Text(
                        text = severity.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tagColor
                    )
                }
            }

            if (!meta.petType.isNullOrBlank()) {
                Text("• Pet Type: ${meta.petType}", fontSize = 12.sp, color = TextPrimary)
            }
            if (!meta.observations.isNullOrBlank()) {
                Text("• Observations: ${meta.observations}", fontSize = 12.sp, color = TextPrimary)
            }
            if (!meta.possibleConditions.isNullOrBlank()) {
                Text("• Potential Conditions: ${meta.possibleConditions}", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
            if (!meta.recommendedCare.isNullOrBlank()) {
                Text("• First Aid / Home Comfort: ${meta.recommendedCare}", fontSize = 12.sp, color = TextSecondary)
            }
            if (!meta.vetRecommendation.isNullOrBlank()) {
                Text("• When to visit vet: ${meta.vetRecommendation}", fontSize = 12.sp, color = AlertRose, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SearchSourceChip(
    source: SearchSourceDto,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MintSoft,
        border = BorderStroke(0.8.dp, MintSage.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.title.ifBlank { source.source },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = source.source,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open Source Link",
                tint = MintSage,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun ActivePetHeader(
    pet: PetDto?,
    hasMultiplePets: Boolean = false,
    onSwitchPet: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clickable(enabled = hasMultiplePets) { onSwitchPet() },
        shape = RoundedCornerShape(14.dp),
        color = WarmPearl,
        border = BorderStroke(1.dp, MintSage.copy(alpha = 0.3f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MintSoft),
                contentAlignment = Alignment.Center
            ) {
                if (!pet?.imageUrl.isNullOrBlank()) {
                    ChatNetworkImage(
                        imageUrl = pet!!.imageUrl!!,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Pets,
                        contentDescription = null,
                        tint = MintSage,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pet?.name ?: "Active Pet",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = "● Active Context",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                val details = if (pet != null) {
                    buildString {
                        if (pet.species.isNotBlank()) append(pet.species)
                        if (pet.breed.isNotBlank() && pet.breed != "Mixed") append(" • ${pet.breed}")
                        if (pet.ageYears.isNotBlank()) append(" • ${pet.ageYears}")
                        if (pet.weightKg > 0) append(" • ${pet.weightKg} kg")
                    }
                } else ""
                Text(
                    text = if (details.isNotBlank()) details else "Tailored veterinary intelligence active",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (hasMultiplePets) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MintSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Switch Pet",
                            tint = EmeraldDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "Switch",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AITypingIndicator(petName: String) {
    val phrases = remember(petName) {
        listOf(
            "Thinking...",
            "Processing your request...",
            "Analyzing $petName's health profile...",
            "Consulting veterinary guidelines...",
            "Formulating personalized care advice..."
        )
    }

    var phraseIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2200)
            phraseIndex = (phraseIndex + 1) % phrases.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "thinking_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MintSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MintSage,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer(alpha = alpha)
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = WarmPearl,
            border = BorderStroke(1.dp, MintSage.copy(alpha = 0.35f)),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = MintSage,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                AnimatedContent(
                    targetState = phrases[phraseIndex],
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "status_text"
                ) { text ->
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        color = EmeraldDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ChatNetworkImage(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(imageUrl) {
        mutableStateOf(PetImageLoader.getCachedBitmap(imageUrl.trim()))
    }

    LaunchedEffect(imageUrl) {
        val loaded = PetImageLoader.loadBitmap(context, imageUrl.trim())
        if (loaded != null) {
            bitmap = loaded
        }
    }

    Box(
        modifier = modifier.background(SoftBackground),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Pet Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MintSage,
                strokeWidth = 2.dp
            )
        }
    }
}
