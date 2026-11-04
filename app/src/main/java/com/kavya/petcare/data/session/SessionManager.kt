package com.kavya.petcare.data.session

import android.content.Context
import android.content.SharedPreferences

class SessionManager private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "petcare_session_prefs"
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_AVATAR_URL = "key_user_avatar_url"
        private const val KEY_ACTIVE_PET_ID = "key_active_pet_id"
        private const val KEY_ACTIVE_PET_NAME = "key_active_pet_name"
        private const val KEY_ACTIVE_PET_TYPE = "key_active_pet_type"
        private const val KEY_ACTIVE_PET_BREED = "key_active_pet_breed"
        private const val KEY_ACTIVE_PET_AGE = "key_active_pet_age"
        private const val KEY_ACTIVE_PET_WEIGHT = "key_active_pet_weight"
        private const val KEY_ACTIVE_PET_IMAGE_URL = "key_active_pet_image_url"
        private const val KEY_ACTIVE_PET_GENDER = "key_active_pet_gender"
        private const val KEY_ACTIVE_PET_MICROCHIP = "key_active_pet_microchip"
        private const val KEY_ACTIVE_PET_HEALTH_SCORE = "key_active_pet_health_score"
        private const val KEY_ACTIVE_PET_VACCINATION = "key_active_pet_vaccination"
        private const val KEY_BASE_URL = "key_base_url"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context).also { INSTANCE = it }
            }
        }
    }

    fun saveAuthSession(token: String, userId: Int, userName: String, email: String, activePetId: Int? = null) {
        prefs.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putInt(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_EMAIL, email)
            if (activePetId != null) {
                putInt(KEY_ACTIVE_PET_ID, activePetId)
            }
            putBoolean(KEY_ONBOARDING_COMPLETED, true)
            apply()
        }
    }

    fun updateUserInfo(userId: Int? = null, userName: String? = null, email: String? = null, avatarUrl: String? = null) {
        prefs.edit().apply {
            if (userId != null && userId > 0) putInt(KEY_USER_ID, userId)
            if (!userName.isNullOrBlank()) putString(KEY_USER_NAME, userName)
            if (!email.isNullOrBlank()) putString(KEY_USER_EMAIL, email)
            if (avatarUrl != null) putString(KEY_USER_AVATAR_URL, avatarUrl)
            apply()
        }
    }

    fun getUserAvatarUrl(): String? = prefs.getString(KEY_USER_AVATAR_URL, null)

    fun setUserAvatarUrl(url: String?) {
        prefs.edit().putString(KEY_USER_AVATAR_URL, url).apply()
    }

    fun saveActivePetDetails(
        id: Int,
        name: String,
        type: String,
        breed: String,
        age: String,
        weight: String,
        imageUrl: String?,
        gender: String = "Female",
        microchipId: String? = null,
        healthScore: Int = 95,
        vaccinationStatus: String = "Up to Date"
    ) {
        prefs.edit().apply {
            putInt(KEY_ACTIVE_PET_ID, id)
            putString(KEY_ACTIVE_PET_NAME, name)
            putString(KEY_ACTIVE_PET_TYPE, type)
            putString(KEY_ACTIVE_PET_BREED, breed)
            putString(KEY_ACTIVE_PET_AGE, age)
            putString(KEY_ACTIVE_PET_WEIGHT, weight)
            putString(KEY_ACTIVE_PET_IMAGE_URL, imageUrl)
            putString(KEY_ACTIVE_PET_GENDER, gender)
            putString(KEY_ACTIVE_PET_MICROCHIP, microchipId)
            putInt(KEY_ACTIVE_PET_HEALTH_SCORE, healthScore)
            putString(KEY_ACTIVE_PET_VACCINATION, vaccinationStatus)
            apply()
        }
    }

    fun getSavedActivePetName(): String? = prefs.getString(KEY_ACTIVE_PET_NAME, null)
    fun getSavedActivePetType(): String? = prefs.getString(KEY_ACTIVE_PET_TYPE, null)
    fun getSavedActivePetBreed(): String? = prefs.getString(KEY_ACTIVE_PET_BREED, null)
    fun getSavedActivePetAge(): String? = prefs.getString(KEY_ACTIVE_PET_AGE, null)
    fun getSavedActivePetWeight(): String? = prefs.getString(KEY_ACTIVE_PET_WEIGHT, null)
    fun getSavedActivePetImageUrl(): String? = prefs.getString(KEY_ACTIVE_PET_IMAGE_URL, null)
    fun getSavedActivePetGender(): String? = prefs.getString(KEY_ACTIVE_PET_GENDER, null)
    fun getSavedActivePetMicrochip(): String? = prefs.getString(KEY_ACTIVE_PET_MICROCHIP, null)
    fun getSavedActivePetHealthScore(): Int = prefs.getInt(KEY_ACTIVE_PET_HEALTH_SCORE, 95)
    fun getSavedActivePetVaccination(): String = prefs.getString(KEY_ACTIVE_PET_VACCINATION, "Up to Date") ?: "Up to Date"

    fun getToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "User") ?: "User"

    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""

    fun setActivePetId(petId: Int) {
        prefs.edit().putInt(KEY_ACTIVE_PET_ID, petId).apply()
    }

    fun getActivePetId(): Int = prefs.getInt(KEY_ACTIVE_PET_ID, 1)

    fun saveBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url).apply()
    }

    fun getBaseUrl(): String? = prefs.getString(KEY_BASE_URL, null)

    fun isOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_AVATAR_URL)
            remove(KEY_ACTIVE_PET_ID)
            remove(KEY_ACTIVE_PET_NAME)
            remove(KEY_ACTIVE_PET_TYPE)
            remove(KEY_ACTIVE_PET_BREED)
            remove(KEY_ACTIVE_PET_AGE)
            remove(KEY_ACTIVE_PET_WEIGHT)
            remove(KEY_ACTIVE_PET_IMAGE_URL)
            remove(KEY_ACTIVE_PET_GENDER)
            remove(KEY_ACTIVE_PET_MICROCHIP)
            remove(KEY_ACTIVE_PET_HEALTH_SCORE)
            remove(KEY_ACTIVE_PET_VACCINATION)
            apply()
        }
    }
}

