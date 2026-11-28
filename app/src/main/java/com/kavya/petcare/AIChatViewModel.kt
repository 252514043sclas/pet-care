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

    private val _conversations = MutableStateFlow<List<ChatConversationDto>>(emptyList())
    val conversations: StateFlow<List<ChatConversationDto>> = _conversations.asStateFlow()

    private val _activeConversation = MutableStateFlow<ChatConversationDto?>(null)
    val activeConversation: StateFlow<ChatConversationDto?> = _activeConversation.asStateFlow()

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

    private val _userPets = MutableStateFlow<List<PetDto>>(emptyList())
    val userPets: StateFlow<List<PetDto>> = _userPets.asStateFlow()

    private val _systemHealth = MutableStateFlow<SystemHealthDto?>(null)
    val systemHealth: StateFlow<SystemHealthDto?> = _systemHealth.asStateFlow()

    private var lastFailedAction: (() -> Unit)? = null

    init {
        loadActivePet()
        loadConversations()
        checkHealth()
    }

    fun loadActivePet() {
        val pet = sessionManager.getActivePetDetails()
        if (pet != null) {
            _activePet.value = pet
        }
        viewModelScope.launch {
            repository.getPets().onSuccess { petsList ->
                _userPets.value = petsList
                if (petsList.isNotEmpty()) {
                    val activeId = sessionManager.getActivePetId()
                    val matched = if (activeId > 0) {
                        petsList.find { it.id == activeId } ?: petsList.first()
                    } else {
                        petsList.first()
                    }
                    _activePet.value = matched
                    sessionManager.setActivePetId(matched.id)
                    sessionManager.saveActivePetDetails(
                        id = matched.id,
                        name = matched.name,
                        type = matched.species,
                        breed = matched.breed,
                        age = matched.ageYears,
                        weight = matched.weightKg.toString(),
                        imageUrl = matched.imageUrl,
                        gender = matched.gender,
                        healthScore = matched.healthScore,
                        vaccinationStatus = matched.vaccinationStatus
                    )
                }
            }
        }
    }

    fun selectPet(pet: PetDto) {
        _activePet.value = pet
        sessionManager.setActivePetId(pet.id)
        sessionManager.saveActivePetDetails(
            id = pet.id,
            name = pet.name,
            type = pet.species,
            breed = pet.breed,
            age = pet.ageYears,
            weight = pet.weightKg.toString(),
            imageUrl = pet.imageUrl,
            gender = pet.gender,
            healthScore = pet.healthScore,
            vaccinationStatus = pet.vaccinationStatus
        )
    }

    fun checkHealth() {
        viewModelScope.launch {
            repository.getSystemHealth().onSuccess { health ->
                _systemHealth.value = health
            }
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            repository.getChatConversations().fold(
                onSuccess = { list ->
                    _conversations.value = list
                    // If active conversation is set, refresh its info
                    val currentActive = _activeConversation.value
                    if (currentActive != null) {
                        val matched = list.find { it.id == currentActive.id }
                        if (matched != null) {
                            _activeConversation.value = matched
                        }
                    } else if (list.isNotEmpty() && _messages.value.isEmpty()) {
                        // Optionally select the most recent conversation
                        selectConversation(list.first())
                    }
                },
                onFailure = {
                    // Non-blocking fallback
                }
            )
        }
    }

    fun startNewChat() {
        _activeConversation.value = null
        _messages.value = emptyList()
        _errorMessage.value = null
    }

    fun selectConversation(conversation: ChatConversationDto) {
        _activeConversation.value = conversation
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            repository.getChatConversationDetails(conversation.id).fold(
                onSuccess = { detailed ->
                    _activeConversation.value = detailed
                    _messages.value = detailed.messages
                    _isLoading.value = false
                },
                onFailure = { err ->
                    _isLoading.value = false
                    _errorMessage.value = err.localizedMessage
                }
            )
        }
    }

    fun deleteConversation(conversationId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteChatConversation(conversationId).fold(
                onSuccess = {
                    if (_activeConversation.value?.id == conversationId) {
                        startNewChat()
                    }
                    loadConversations()
                    _isLoading.value = false
                },
                onFailure = { err ->
                    _isLoading.value = false
                    _errorMessage.value = err.localizedMessage
                }
            )
        }
    }

    fun selectImage(bitmap: Bitmap) {
        _selectedImageBitmap.value = bitmap
        val stream = ByteArrayOutputStream()
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

        val currentPetId = _activePet.value?.id ?: sessionManager.getActivePetId()
        val currentConvId = _activeConversation.value?.id

        // Optimistically add user message to UI
        val tempUserMsg = ChatMessageDto(
            id = System.currentTimeMillis().toInt(),
            conversationId = currentConvId,
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
                enableSearch = enableSearch,
                conversationId = currentConvId
            ).fold(
                onSuccess = { response ->
                    val assistantMsg = ChatMessageDto(
                        id = response.messageId ?: System.currentTimeMillis().toInt(),
                        conversationId = response.conversationId ?: currentConvId,
                        sender = "assistant",
                        message = response.message,
                        messageType = if (response.sources.isNotEmpty()) "web_search" else "text",
                        sources = response.sources,
                        createdAt = response.timestamp ?: "Just now"
                    )
                    _messages.value = _messages.value + assistantMsg
                    _isLoading.value = false

                    if (_activeConversation.value == null && response.conversationId != null) {
                        _activeConversation.value = ChatConversationDto(
                            id = response.conversationId,
                            title = response.conversationTitle ?: "Pet Consultation"
                        )
                    }
                    loadConversations()
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
        val currentPetId = _activePet.value?.id ?: sessionManager.getActivePetId()
        val currentConvId = _activeConversation.value?.id
        val displayPrompt = if (prompt.isNotBlank()) prompt else "Uploaded pet photo for health assessment."

        val tempUserMsg = ChatMessageDto(
            id = System.currentTimeMillis().toInt(),
            conversationId = currentConvId,
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
                petId = currentPetId,
                conversationId = currentConvId
            ).fold(
                onSuccess = { response ->
                    val assistantMsg = ChatMessageDto(
                        id = response.messageId ?: System.currentTimeMillis().toInt(),
                        conversationId = response.conversationId ?: currentConvId,
                        sender = "assistant",
                        message = response.message,
                        imageUrl = response.imageUrl,
                        messageType = "image_analysis",
                        analysisMetadata = response.metadata,
                        createdAt = response.timestamp ?: "Just now"
                    )
                    _messages.value = _messages.value + assistantMsg
                    _isLoading.value = false

                    if (_activeConversation.value == null && response.conversationId != null) {
                        _activeConversation.value = ChatConversationDto(
                            id = response.conversationId,
                            title = "Photo Health Analysis"
                        )
                    }
                    loadConversations()
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

        val currentPetId = _activePet.value?.id ?: sessionManager.getActivePetId()
        val currentConvId = _activeConversation.value?.id

        val tempUserMsg = ChatMessageDto(
            id = System.currentTimeMillis().toInt(),
            conversationId = currentConvId,
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
                        conversationId = currentConvId,
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
        val activeConv = _activeConversation.value
        if (activeConv != null) {
            deleteConversation(activeConv.id)
        } else {
            val currentPetId = _activePet.value?.id ?: sessionManager.getActivePetId()
            viewModelScope.launch {
                _isLoading.value = true
                repository.clearChatHistory(petId = currentPetId).fold(
                    onSuccess = {
                        startNewChat()
                        _isLoading.value = false
                    },
                    onFailure = { err ->
                        _isLoading.value = false
                        _errorMessage.value = err.localizedMessage
                    }
                )
            }
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
