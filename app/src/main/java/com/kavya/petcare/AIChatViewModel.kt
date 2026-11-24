package com.kavya.petcare

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kavya.petcare.data.models.*
import com.kavya.petcare.data.repository.PetCareRepository
import com.kavya.petcare.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class AIChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PetCareRepository(application)
    private val sessionManager = SessionManager.getInstance(application)

    private val _messages = MutableStateFlow<List<ChatMessageDto>>(emptyList())
    val messages: StateFlow<List<ChatMessageDto>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap.asStateFlow()

    private val _selectedImageBase64 = MutableStateFlow<String?>(null)
    val selectedImageBase64: StateFlow<String?> = _selectedImageBase64.asStateFlow()

    private val _activePet = MutableStateFlow<PetDto?>(null)
    val activePet: StateFlow<PetDto?> = _activePet.asStateFlow()

    private val _systemHealth = MutableStateFlow<SystemHealthDto?>(null)
    val systemHealth: StateFlow<SystemHealthDto?> = _systemHealth.asStateFlow()

    private var lastFailedAction: (() -> Unit)? = null

    init {
        loadActivePet()
        loadHistory()
        checkHealth()
    }

    fun loadActivePet() {
        val pet = sessionManager.getActivePetDetails()
        _activePet.value = pet
    }

    fun checkHealth() {
        viewModelScope.launch {
            repository.getSystemHealth().onSuccess { health ->
                _systemHealth.value = health
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val currentPetId = _activePet.value?.id
            repository.getChatHistory(petId = currentPetId).fold(
                onSuccess = { historyList ->
                    if (historyList.isEmpty()) {
                        // Insert friendly welcome greeting
                        val petName = _activePet.value?.name ?: "your pet"
                        val species = _activePet.value?.species ?: "pet"
                        val welcomeMsg = ChatMessageDto(
                            id = 0,
                            sender = "assistant",
                            message = "Hello! I am your AI Pet Care Assistant powered by local Qwen3-VL 2B. " +
                                    "I can assist with health triage for $petName ($species), common diseases, wound care, " +
                                    "safe nutrition and homemade meal planning. You can also upload photos of any affected skin or coat for visual analysis!",
                            messageType = "text",
                            createdAt = "Just now"
                        )
                        _messages.value = listOf(welcomeMsg)
                    } else {
                        _messages.value = historyList
                    }
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _isLoading.value = false
                    _errorMessage.value = error.localizedMessage
                }
            )
        }
    }

    fun selectImage(bitmap: Bitmap) {
        _selectedImageBitmap.value = bitmap
        val stream = ByteArrayOutputStream()
        // Compress bitmap efficiently (max 768px for optimal vision tokens, 80 quality JPEG)
        val scaled = scaleDownBitmap(bitmap, 768)
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()
        _selectedImageBase64.value = Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun clearSelectedImage() {
        _selectedImageBitmap.value = null
        _selectedImageBase64.value = null
    }

    fun sendMessage(text: String, enableSearch: Boolean = true) {
        val trimmed = text.trim()
        val imageBase64 = _selectedImageBase64.value

        if (imageBase64 != null) {
            sendImageMessage(imageBase64, trimmed)
            return
        }

        if (trimmed.isEmpty()) return

        val currentPetId = _activePet.value?.id

        // Optimistically add user message to UI
        val tempUserMsg = ChatMessageDto(
            id = System.currentTimeMillis().toInt(),
            sender = "user",
            message = trimmed,
            messageType = "text",
            createdAt = "Just now"
        )
        _messages.value = _messages.value + tempUserMsg

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            repository.sendChatMessage(
                message = trimmed,
                petId = currentPetId,
                enableSearch = enableSearch
            ).fold(
                onSuccess = { response ->
                    val assistantMsg = ChatMessageDto(
                        id = response.messageId ?: System.currentTimeMillis().toInt(),
                        sender = "assistant",
                        message = response.message,
                        messageType = if (response.sources.isNotEmpty()) "web_search" else "text",
                        sources = response.sources,
                        createdAt = response.timestamp ?: "Just now"
                    )
                    _messages.value = _messages.value + assistantMsg
                    _isLoading.value = false
                },
                onFailure = { err ->
                    _isLoading.value = false
                    _errorMessage.value = err.localizedMessage
                    lastFailedAction = { sendMessage(trimmed, enableSearch) }
                }
            )
        }
    }

    private fun sendImageMessage(imageBase64: String, prompt: String) {
        val currentPetId = _activePet.value?.id
        val displayPrompt = if (prompt.isNotBlank()) prompt else "Uploaded pet photo for health assessment."

        // Optimistically add message
        val tempUserMsg = ChatMessageDto(
            id = System.currentTimeMillis().toInt(),
            sender = "user",
            message = displayPrompt,
            messageType = "image_upload",
            createdAt = "Just now"
        )
        _messages.value = _messages.value + tempUserMsg

        clearSelectedImage()
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            repository.sendChatImage(
                imageBase64 = imageBase64,
                prompt = prompt,
                petId = currentPetId
            ).fold(
                onSuccess = { response ->
                    val assistantMsg = ChatMessageDto(
                        id = response.messageId ?: System.currentTimeMillis().toInt(),
                        sender = "assistant",
                        message = response.message,
                        imageUrl = response.imageUrl,
                        messageType = "image_analysis",
                        analysisMetadata = response.metadata,
                        createdAt = response.timestamp ?: "Just now"
                    )
                    _messages.value = _messages.value + assistantMsg
                    _isLoading.value = false
                },
                onFailure = { err ->
                    _isLoading.value = false
                    _errorMessage.value = err.localizedMessage
                    lastFailedAction = { sendImageMessage(imageBase64, prompt) }
                }
            )
        }
    }

    fun askNutrition(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val currentPetId = _activePet.value?.id
        val tempUserMsg = ChatMessageDto(
            id = System.currentTimeMillis().toInt(),
            sender = "user",
            message = trimmed,
            messageType = "nutrition",
            createdAt = "Just now"
        )
        _messages.value = _messages.value + tempUserMsg

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            repository.getNutritionGuidance(
                query = trimmed,
                petId = currentPetId
            ).fold(
                onSuccess = { response ->
                    val assistantMsg = ChatMessageDto(
                        id = response.messageId ?: System.currentTimeMillis().toInt(),
                        sender = "assistant",
                        message = response.message,
                        messageType = "nutrition",
                        createdAt = response.timestamp ?: "Just now"
                    )
                    _messages.value = _messages.value + assistantMsg
                    _isLoading.value = false
                },
                onFailure = { err ->
                    _isLoading.value = false
                    _errorMessage.value = err.localizedMessage
                    lastFailedAction = { askNutrition(trimmed) }
                }
            )
        }
    }

    fun clearHistory() {
        val currentPetId = _activePet.value?.id
        viewModelScope.launch {
            _isLoading.value = true
            repository.clearChatHistory(petId = currentPetId).fold(
                onSuccess = {
                    _messages.value = emptyList()
                    _isLoading.value = false
                    loadHistory()
                },
                onFailure = { err ->
                    _isLoading.value = false
                    _errorMessage.value = err.localizedMessage
                }
            )
        }
    }

    fun retryLast() {
        _errorMessage.value = null
        lastFailedAction?.invoke()
    }

    private fun scaleDownBitmap(realImage: Bitmap, maxImageSize: Int): Bitmap {
        val ratio = Math.min(
            maxImageSize.toFloat() / realImage.width,
            maxImageSize.toFloat() / realImage.height
        )
        if (ratio >= 1.0f) return realImage
        val width = Math.round(ratio * realImage.width)
        val height = Math.round(ratio * realImage.height)
        return Bitmap.createScaledBitmap(realImage, width, height, true)
    }
}
