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
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object PetImageLoader {
    private const val TAG = "PetImageLoader"

    // 50-item in-memory LRU cache
    private val memoryCache = object : LruCache<String, Bitmap>(50) {}

    fun cacheBitmap(key: String, bitmap: Bitmap) {
        memoryCache.put(key, bitmap)
    }

    fun getCachedBitmap(key: String): Bitmap? {
        return memoryCache.get(key)
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
                    return@withContext bmp
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding base64 image", e)
            }
        }

        // 3. Resolve complete HTTP URL
        val fullUrl = resolveFullUrl(context, cacheKey)

        // 4. Disk cache check
        val diskCacheDir = File(context.cacheDir, "pet_images")
        if (!diskCacheDir.exists()) diskCacheDir.mkdirs()

        val md5Key = MessageDigest.getInstance("MD5")
            .digest(fullUrl.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val diskFile = File(diskCacheDir, "$md5Key.cache")

        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val bmp = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bmp != null) {
                    memoryCache.put(cacheKey, bmp)
                    return@withContext bmp
                }
            } catch (_: Exception) {}
        }

        // 5. Download over HTTP via OkHttp
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

fun getPetDrawableRes(species: String?): Int {
    val clean = species?.lowercase()?.trim() ?: ""
    return when {
        clean.contains("dog") || clean.contains("pup") || clean.contains("canine") || clean.contains("retriever") || clean.contains("shepherd") || clean.contains("hound") -> R.drawable.ic_pet_dog
        clean.contains("cat") || clean.contains("kitten") || clean.contains("feline") || clean.contains("persian") || clean.contains("siamese") -> R.drawable.ic_pet_cat
        clean.contains("rabbit") || clean.contains("bunny") || clean.contains("lop") || clean.contains("hare") -> R.drawable.ic_pet_rabbit
        clean.contains("bird") || clean.contains("parrot") || clean.contains("canary") || clean.contains("finch") || clean.contains("cockatiel") -> R.drawable.ic_pet_bird
        clean.contains("hamster") || clean.contains("guinea") || clean.contains("cavy") || clean.contains("gerbil") || clean.contains("mouse") || clean.contains("rat") -> R.drawable.ic_pet_hamster
        clean.contains("fish") || clean.contains("goldfish") || clean.contains("betta") || clean.contains("aquarium") -> R.drawable.ic_pet_fish
        else -> R.drawable.ic_pet_default
    }
}

fun getPetBadgeColor(species: String?): Color {
    val clean = species?.lowercase()?.trim() ?: ""
    return when {
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
    var bitmap by remember(imageUrl) {
        mutableStateOf(if (!imageUrl.isNullOrBlank()) PetImageLoader.getCachedBitmap(imageUrl.trim()) else null)
    }

    LaunchedEffect(imageUrl) {
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
                    modifier = Modifier.fillMaxSize(iconSizeRatio),
                    contentScale = ContentScale.Fit
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

