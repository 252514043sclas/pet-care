package com.kavya.petcare.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kavya.petcare.R
import com.kavya.petcare.data.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object PetImageLoader {
    private const val TAG = "PetImageLoader"

    // 50-item in-memory LRU cache
    private val memoryCache = object : LruCache<String, Bitmap>(50) {}

    fun cacheBitmap(key: String, bitmap: Bitmap) {
        if (key.isBlank()) return
        memoryCache.put(key.trim(), bitmap)
    }

    fun cacheBitmap(context: Context, key: String, bitmap: Bitmap) {
        if (key.isBlank()) return
        val cleanKey = key.trim()
        memoryCache.put(cleanKey, bitmap)
        saveToPersistentStorage(context, cleanKey, bitmap)
    }

    fun getCachedBitmap(key: String): Bitmap? {
        if (key.isBlank()) return null
        return memoryCache.get(key.trim())
    }

    fun clearMemoryCache() {
        memoryCache.evictAll()
    }

    private fun getStorageKey(key: String): String {
        return try {
            MessageDigest.getInstance("MD5")
                .digest(key.trim().toByteArray())
                .joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            key.hashCode().toString()
        }
    }

    fun saveToPersistentStorage(context: Context, key: String, bitmap: Bitmap) {
        try {
            val dir = File(context.filesDir, "persistent_pet_images")
            if (!dir.exists()) dir.mkdirs()

            val fileName = "${getStorageKey(key)}.jpg"
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error saving bitmap to persistent storage: ${e.message}")
        }
    }

    suspend fun loadBitmap(context: Context, urlOrPath: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (urlOrPath.isNullOrBlank()) return@withContext null

        val cacheKey = urlOrPath.trim()

        // 1. In-memory cache check
        memoryCache.get(cacheKey)?.let { return@withContext it }

        // 2. Base64 data URI check
        if (cacheKey.startsWith("data:image/")) {
            try {
                val data = cacheKey.substringAfter(",")
                val bytes = Base64.decode(data, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    memoryCache.put(cacheKey, bmp)
                    saveToPersistentStorage(context, cacheKey, bmp)
                    return@withContext bmp
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding base64 image", e)
            }
        }

        // 3. Persistent disk storage check in internal filesDir
        try {
            val persistentDir = File(context.filesDir, "persistent_pet_images")
            val pFile = File(persistentDir, "${getStorageKey(cacheKey)}.jpg")
            if (pFile.exists() && pFile.length() > 0) {
                val bmp = BitmapFactory.decodeFile(pFile.absolutePath)
                if (bmp != null) {
                    memoryCache.put(cacheKey, bmp)
                    return@withContext bmp
                }
            }
        } catch (_: Exception) {}

        // 4. Resolve complete HTTP URL
        val fullUrl = resolveFullUrl(context, cacheKey)

        // Also check if fullUrl was stored on persistent disk
        try {
            val persistentDir = File(context.filesDir, "persistent_pet_images")
            val pFile = File(persistentDir, "${getStorageKey(fullUrl)}.jpg")
            if (pFile.exists() && pFile.length() > 0) {
                val bmp = BitmapFactory.decodeFile(pFile.absolutePath)
                if (bmp != null) {
                    memoryCache.put(cacheKey, bmp)
                    return@withContext bmp
                }
            }
        } catch (_: Exception) {}

        // 5. App cacheDir check
        val diskCacheDir = File(context.cacheDir, "pet_images")
        if (!diskCacheDir.exists()) diskCacheDir.mkdirs()

        val diskFile = File(diskCacheDir, "${getStorageKey(fullUrl)}.cache")
        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val bmp = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bmp != null) {
                    memoryCache.put(cacheKey, bmp)
                    saveToPersistentStorage(context, cacheKey, bmp)
                    return@withContext bmp
                }
            } catch (_: Exception) {}
        }

        // 6. Download over HTTP via OkHttp
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("Accept", "image/*")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        try {
                            diskFile.writeBytes(bytes)
                        } catch (_: Exception) {}
                        memoryCache.put(cacheKey, bmp)
                        saveToPersistentStorage(context, cacheKey, bmp)
                        saveToPersistentStorage(context, fullUrl, bmp)
                        return@withContext bmp
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed downloading image from $fullUrl: ${e.message}")
        }

        null
    }

    fun resolveFullUrl(context: Context, rawPath: String): String {
        if (rawPath.startsWith("http://") || rawPath.startsWith("https://")) {
            return rawPath
        }
        val baseUrl = NetworkConfig.getCurrentBaseUrl(context)
        val root = if (baseUrl.contains("/api/")) {
            baseUrl.substringBefore("/api/") + "/"
        } else if (baseUrl.contains("/api")) {
            baseUrl.substringBefore("/api") + "/"
        } else {
            baseUrl
        }
        return root + rawPath.removePrefix("/")
    }
}

/**
 * Returns the proper animal vector drawable based on the species or breed string.
 * Supports rich synonyms so Dogs, Cats, Rabbits, Birds, Hamsters, Fish and Other
 * always receive their tailored visual icon.
 */
fun getPetDrawableRes(species: String?): Int {
    val clean = species?.lowercase()?.trim() ?: ""
    return when {
        clean.contains("dog") || clean.contains("pup") || clean.contains("canine") ||
                clean.contains("retriever") || clean.contains("shepherd") || clean.contains("hound") ||
                clean.contains("husky") || clean.contains("poodle") || clean.contains("bulldog") ||
                clean.contains("terrier") || clean.contains("corgi") || clean.contains("labrador") ||
                clean.contains("chihuahua") || clean.contains("beagle") || clean.contains("boxer") ||
                clean.contains("rottweiler") -> R.drawable.img_pet_dog

        clean.contains("cat") || clean.contains("kitten") || clean.contains("kitty") ||
                clean.contains("feline") || clean.contains("persian") || clean.contains("siamese") ||
                clean.contains("shorthair") || clean.contains("meow") || clean.contains("bengal") ||
                clean.contains("ragdoll") -> R.drawable.img_pet_cat

        clean.contains("rabbit") || clean.contains("bunny") || clean.contains("lop") ||
                clean.contains("hare") || clean.contains("cottontail") -> R.drawable.img_pet_rabbit

        clean.contains("bird") || clean.contains("parrot") || clean.contains("canary") ||
                clean.contains("finch") || clean.contains("cockatiel") || clean.contains("parakeet") ||
                clean.contains("avian") || clean.contains("budgie") || clean.contains("cockatoo") ||
                clean.contains("lovebird") -> R.drawable.img_pet_bird

        clean.contains("hamster") || clean.contains("guinea") || clean.contains("cavy") ||
                clean.contains("gerbil") || clean.contains("mouse") || clean.contains("rat") ||
                clean.contains("rodent") || clean.contains("chinchilla") -> R.drawable.img_pet_hamster

        clean.contains("fish") || clean.contains("goldfish") || clean.contains("betta") ||
                clean.contains("aquarium") || clean.contains("guppy") || clean.contains("tetra") ||
                clean.contains("aquatic") || clean.contains("koi") || clean.contains("cichlid") -> R.drawable.ic_pet_fish

        else -> R.drawable.img_pet_other
    }
}

/**
 * Returns the matching subtle background tint for each species badge.
 */
fun getPetBadgeColor(species: String?): Color {
    val clean = species?.lowercase()?.trim() ?: ""
    return when {
        clean.contains("dog") || clean.contains("pup") -> Color(0xFFE8F5E9)
        clean.contains("cat") -> Color(0xFFFFF3E0)
        clean.contains("rabbit") -> Color(0xFFF3E5F5)
        clean.contains("bird") -> Color(0xFFE1F5FE)
        clean.contains("hamster") -> Color(0xFFFFF8E1)
        clean.contains("fish") -> Color(0xFFE0F7FA)
        else -> Color(0xFFE8F5F2)
    }
}

@Composable
fun PetAvatar(
    imageUrl: String?,
    species: String?,
    modifier: Modifier = Modifier,
    petName: String? = "Pet",
    shape: Shape = CircleShape,
    backgroundColor: Color? = null,
    iconSizeRatio: Float = 0.65f,
    shadowElevation: Dp = 0.dp
) {
    val context = LocalContext.current
    var bitmap by remember(imageUrl, species) {
        mutableStateOf(if (!imageUrl.isNullOrBlank()) PetImageLoader.getCachedBitmap(imageUrl.trim()) else null)
    }

    LaunchedEffect(imageUrl, species) {
        if (!imageUrl.isNullOrBlank()) {
            val loaded = PetImageLoader.loadBitmap(context, imageUrl.trim())
            if (loaded != null) {
                bitmap = loaded
            }
        } else {
            bitmap = null
        }
    }

    val bgColor = backgroundColor ?: getPetBadgeColor(species)

    Surface(
        modifier = modifier.clip(shape),
        shape = shape,
        color = bgColor,
        shadowElevation = shadowElevation
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = petName ?: "Pet Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                val drawableRes = getPetDrawableRes(species)
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = petName ?: "Pet Icon",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun UserAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    userName: String? = "User",
    shape: Shape = CircleShape,
    backgroundColor: Color = Color(0xFFE8F5F2),
    shadowElevation: Dp = 0.dp
) {
    val context = LocalContext.current
    var bitmap by remember(avatarUrl) {
        mutableStateOf(if (!avatarUrl.isNullOrBlank()) PetImageLoader.getCachedBitmap(avatarUrl.trim()) else null)
    }

    LaunchedEffect(avatarUrl) {
        if (!avatarUrl.isNullOrBlank()) {
            val loaded = PetImageLoader.loadBitmap(context, avatarUrl.trim())
            if (loaded != null) {
                bitmap = loaded
            }
        } else {
            bitmap = null
        }
    }

    Surface(
        modifier = modifier.clip(shape),
        shape = shape,
        color = backgroundColor,
        shadowElevation = shadowElevation
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = userName ?: "User Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_user_avatar),
                    contentDescription = userName ?: "User Icon",
                    modifier = Modifier.fillMaxSize(0.6f),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
