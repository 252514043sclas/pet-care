package com.kavya.petcare.data.network

import android.content.Context
import android.os.Build
import android.util.Log
import com.kavya.petcare.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkConfig {
    private const val TAG = "PetCareNetwork"

    /**
     * TARGET ENVIRONMENTS:
     * - EMULATOR_XAMPP: "http://10.0.2.2/petcare/public/api/" (XAMPP Apache through QEMU emulator loopback)
     * - PHYSICAL_DEVICE_XAMPP: "http://172.23.176.206/petcare/public/api/" (XAMPP Apache via LAN IP)
     */
    const val EMULATOR_BASE_URL = "http://10.0.2.2/petcare/public/api/"
    const val LAN_BASE_URL = "http://172.23.176.206/petcare/public/api/"
    const val DEFAULT_BASE_URL = EMULATOR_BASE_URL

    /**
     * Detects whether the app is running on an Android Emulator or a Physical Device.
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    /**
     * Automatically resolves the reachable host URL:
     * - If running in an Emulator -> http://10.0.2.2/petcare/public/api/
     * - If running on a Physical Phone -> http://172.23.176.206/petcare/public/api/
     */
    fun getAutoDetectedBaseUrl(): String {
        val detected = if (isEmulator()) EMULATOR_BASE_URL else LAN_BASE_URL
        Log.d(TAG, "isEmulator=${isEmulator()}, autoDetectedBaseUrl=$detected")
        return detected
    }

    @Volatile
    private var apiService: PetCareApiService? = null

    fun getApiService(context: Context): PetCareApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildRetrofit(context).create(PetCareApiService::class.java).also {
                apiService = it
            }
        }
    }

    fun getCurrentBaseUrl(context: Context): String {
        val sessionManager = SessionManager.getInstance(context)
        return sessionManager.getBaseUrl() ?: getAutoDetectedBaseUrl()
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val sessionManager = SessionManager.getInstance(context)
        val baseUrl = sessionManager.getBaseUrl() ?: getAutoDetectedBaseUrl()
        Log.i(TAG, "Building Retrofit with Base URL: $baseUrl")

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")

            val token = sessionManager.getToken()
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            chain.proceed(requestBuilder.build())
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun setCustomBaseUrl(context: Context, newUrl: String) {
        val sessionManager = SessionManager.getInstance(context)
        sessionManager.saveBaseUrl(newUrl)
        resetClient()
    }

    fun resetClient() {
        synchronized(this) {
            apiService = null
        }
    }
}
