package com.uspresident.speeches.data

import android.content.Context
import com.uspresident.speeches.tts.CloudTtsApi
import com.uspresident.speeches.tts.TtsApiResult
import com.uspresident.speeches.tts.TtsBonusApiResult
import com.uspresident.speeches.util.TextHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

sealed class TtsResult {
    data class Success(
        val audioFile: File,
        val fromLocalCache: Boolean,
        val quotaUsed: Int,
        val quotaLimit: Int,
    ) : TtsResult()

    data class QuotaExceeded(
        val quotaUsed: Int,
        val quotaLimit: Int,
    ) : TtsResult()

    data class Error(val message: String) : TtsResult()
}

sealed class TtsBonusGrantResult {
    data class Success(val quotaUsed: Int, val quotaLimit: Int) : TtsBonusGrantResult()
    data class RewardLimitReached(val quotaUsed: Int, val quotaLimit: Int) : TtsBonusGrantResult()
    data class Error(val message: String) : TtsBonusGrantResult()
}

class TtsRepository(
    context: Context,
    private val api: CloudTtsApi,
    private val quotaStore: TtsQuotaStore,
    private val clientIdProvider: ClientIdProvider,
) {
    private val cacheDir = File(context.cacheDir, "tts_audio").apply { mkdirs() }

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun synthesize(text: String): TtsResult = withContext(Dispatchers.IO) {
        val sourceHash = TextHash.sha256(text)
        val localFile = File(cacheDir, "$sourceHash.mp3")

        if (localFile.exists() && localFile.length() > 0L) {
            return@withContext TtsResult.Success(
                audioFile = localFile,
                fromLocalCache = true,
                quotaUsed = quotaStore.getQuotaUsed(),
                quotaLimit = quotaStore.getQuotaLimit(),
            )
        }

        when (val result = api.synthesize(text, clientIdProvider.getClientId())) {
            is TtsApiResult.Success -> {
                downloadAudio(result.audioUrl, localFile)
                    .onFailure { error ->
                        return@withContext TtsResult.Error(error.message ?: "Audio download failed.")
                    }
                quotaStore.saveQuota(result.quotaUsed, result.quotaLimit)
                TtsResult.Success(
                    audioFile = localFile,
                    fromLocalCache = false,
                    quotaUsed = result.quotaUsed,
                    quotaLimit = result.quotaLimit,
                )
            }

            is TtsApiResult.QuotaExceeded -> {
                quotaStore.saveQuota(result.quotaUsed, result.quotaLimit)
                TtsResult.QuotaExceeded(
                    quotaUsed = result.quotaUsed,
                    quotaLimit = result.quotaLimit,
                )
            }

            is TtsApiResult.Error -> TtsResult.Error(result.message)
        }
    }

    suspend fun grantRewardBonus(): TtsBonusGrantResult = withContext(Dispatchers.IO) {
        when (val result = api.grantBonus(clientIdProvider.getClientId())) {
            is TtsBonusApiResult.Success -> {
                quotaStore.saveQuota(result.quotaUsed, result.quotaLimit)
                TtsBonusGrantResult.Success(
                    quotaUsed = result.quotaUsed,
                    quotaLimit = result.quotaLimit,
                )
            }

            is TtsBonusApiResult.RewardLimitReached -> {
                quotaStore.saveQuota(result.quotaUsed, result.quotaLimit)
                TtsBonusGrantResult.RewardLimitReached(
                    quotaUsed = result.quotaUsed,
                    quotaLimit = result.quotaLimit,
                )
            }

            is TtsBonusApiResult.Error -> TtsBonusGrantResult.Error(result.message)
        }
    }

    fun getCachedQuotaUsed(): Int = quotaStore.getQuotaUsed()

    fun getCachedQuotaLimit(): Int = quotaStore.getQuotaLimit()

    fun refreshDailyQuotaIfNeeded(): Boolean = quotaStore.refreshIfNewUtcDay()

    private fun downloadAudio(url: String, destination: File): Result<Unit> {
        val request = Request.Builder().url(url).get().build()
        return try {
            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IllegalStateException("Audio download failed (${response.code})."))
                }
                val bytes = response.body?.bytes()
                    ?: return Result.failure(IllegalStateException("Audio download returned empty body."))
                if (bytes.isEmpty()) {
                    return Result.failure(IllegalStateException("Audio download returned empty body."))
                }
                destination.writeBytes(bytes)
                Result.success(Unit)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
