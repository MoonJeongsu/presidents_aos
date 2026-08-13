package com.uspresident.speeches.data

import android.content.Context
import com.uspresident.speeches.R
import com.uspresident.speeches.util.NetworkUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SpeechBodyFetcher(context: Context) {

    private val appContext = context.applicationContext
    private val bucket = appContext.getString(R.string.firebase_storage_bucket).trim()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val memoryCache = mutableMapOf<String, String>()

    fun isConfigured(): Boolean = bucket.isNotBlank()

    fun fetchBody(speechId: String): Result<String> {
        if (!NetworkUtils.isConnected(appContext)) {
            return Result.failure(IllegalStateException("Network is not available."))
        }

        memoryCache[speechId]?.let { return Result.success(it) }

        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Firebase Storage bucket is not configured."))
        }

        val objectPath = "speeches/$speechId.txt"
        val encodedPath = URLEncoder.encode(objectPath, Charsets.UTF_8.name()).replace("+", "%20")
        val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encodedPath?alt=media"

        val request = Request.Builder().url(url).get().build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(
                        IllegalStateException("Speech download failed (${response.code})."),
                    )
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    return Result.failure(IllegalStateException("Speech text is empty."))
                }
                memoryCache[speechId] = body
                Result.success(body)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
