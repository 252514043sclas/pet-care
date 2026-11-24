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
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kavya.petcare.data.models.ChatMessageDto
import com.kavya.petcare.data.models.SearchSourceDto
import com.kavya.petcare.data.models.VisionMetadataDto
import com.kavya.petcare.data.network.NetworkConfig
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.components.PetAvatar
import com.kavya.petcare.ui.components.PetImageLoader
import com.kavya.petcare.ui.theme.PetCareTheme
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
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedImageBitmap by viewModel.selectedImageBitmap.collectAsState()
    val activePet by viewModel.activePet.collectAsState()
    val systemHealth by viewModel.systemHealth.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

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

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Chat History?", fontWeight = FontWeight.Bold, color = EmeraldDark) },
            text = { Text("This will permanently remove all messages and analyzed images from your device and database.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRose),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
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
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = Color.White,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EmeraldDark)
                        }

                        Spacer(Modifier.width(4.dp))

                        // Pet avatar
                        Box(
                            modifier = Modifier
                                .size(42.dp)
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

                        Spacer(Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PetCare AI Assistant",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (systemHealth?.aiService?.modelLoaded == true) Color(0xFF10B981) else Color(0xFFF59E0B))
                                )
                            }
                            val modelLabel = if (systemHealth?.aiService?.modelLoaded == true) "Qwen3-VL 2B (Local CPU)" else "Qwen3-VL 2B • Ready"
                            Text(
                                text = modelLabel,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showClearConfirmDialog = true }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear History", tint = TextMuted)
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

            // Chat Messages List
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
                        AITypingIndicator()
                    }
                }
            }

            // Quick Prompt Suggestion Chips (when no input)
            if (textInput.isBlank() && selectedImageBitmap == null) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val promptChips = listOf(
                        "Common dog diseases" to "What are the common diseases in dogs and their prevention?",
                        "Cat wound care" to "How can I care for a small superficial wound or cut on my cat?",
                        "Rabbit carrot safety" to "Can rabbits eat carrots? What is a safe portion?",
                        "Healthy meal plan" to "Can you provide a healthy homemade meal plan for my pet?",
                        "Foods toxic to cats" to "What human foods are toxic to cats and should be avoided?"
                    )
                    items(promptChips) { (chipTitle, promptText) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, MintSage.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                viewModel.sendMessage(promptText)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MintSage, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(chipTitle, fontSize = 12.sp, color = EmeraldDark, fontWeight = FontWeight.Medium)
                            }
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
                    color = Color.White,
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
                        IconButton(onClick = { viewModel.clearSelectedImage() }) {
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
                color = Color.White,
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
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo", tint = MintSage)
                    }

                    // Gallery Action
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Pick Image", tint = MintSage)
                    }

                    Spacer(Modifier.width(4.dp))

                    // Text Input Field
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp, max = 110.dp),
                        placeholder = {
                            Text(
                                if (selectedImageBitmap != null) "Add notes for visual scan..." else "Ask symptoms, care, nutrition...",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        },
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintSage,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SoftBackground,
                            unfocusedContainerColor = SoftBackground
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
                color = if (isUser) EmeraldPrimary else Color.White,
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
fun AITypingIndicator() {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MintSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MintSage, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BorderSubtle),
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
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Qwen3-VL 2B is analyzing...",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
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
