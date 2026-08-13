package com.uspresident.speeches.tts

import com.uspresident.speeches.data.TtsQuotaStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class TtsApiResult {
    data class Success(
        val audioUrl: String,
        val fromCache: Boolean,
        val quotaUsed: Int,
        val quotaLimit: Int,
    ) : TtsApiResult()

    data class QuotaExceeded(
        val quotaUsed: Int,
        val quotaLimit: Int,
    ) : TtsApiResult()

    data class Error(val message: String) : TtsApiResult()
}

sealed class TtsBonusApiResult {
    data class Success(
        val bonusGranted: Int,
        val quotaUsed: Int,
        val quotaLimit: Int,
    ) : TtsBonusApiResult()

    data class RewardLimitReached(
        val quotaUsed: Int,
        val quotaLimit: Int,
    ) : TtsBonusApiResult()

    data class Error(val message: String) : TtsBonusApiResult()
}

class CloudTtsApi(
    private val synthesizeUrl: String,
    private val grantBonusUrl: String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun isConfigured(): Boolean {
        return synthesizeUrl.isNotBlank() && grantBonusUrl.isNotBlank()
    }

    fun synthesize(text: String, clientId: String): TtsApiResult {
        if (!isConfigured()) {
            return TtsApiResult.Error("Speech service is not configured.")
        }

        val payload = JSONObject()
            .put("text", text)
            .put("clientId", clientId)
            .toString()

        val request = Request.Builder()
            .url(synthesizeUrl)
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.code == 429) {
                    val json = JSONObject(body)
                    return TtsApiResult.QuotaExceeded(
                        quotaUsed = json.optInt("quotaUsed", TtsQuotaStore.DEFAULT_LIMIT),
                        quotaLimit = json.optInt("quotaLimit", TtsQuotaStore.DEFAULT_LIMIT),
                    )
                }

                if (!response.isSuccessful) {
                    val message = runCatching {
                        JSONObject(body).optString("error", "Speech synthesis failed.")
                    }.getOrDefault("Speech synthesis failed.")
                    return TtsApiResult.Error(message)
                }

                val json = JSONObject(body)
                TtsApiResult.Success(
                    audioUrl = json.getString("audioUrl"),
                    fromCache = json.optBoolean("fromCache", false),
                    quotaUsed = json.optInt("quotaUsed", 0),
                    quotaLimit = json.optInt("quotaLimit", TtsQuotaStore.DEFAULT_LIMIT),
                )
            }
        } catch (error: Exception) {
            TtsApiResult.Error(error.message ?: "Network error")
        }
    }

    fun grantBonus(clientId: String): TtsBonusApiResult {
        if (!isConfigured()) {
            return TtsBonusApiResult.Error("Speech service is not configured.")
        }

        val payload = JSONObject()
            .put("clientId", clientId)
            .toString()

        val request = Request.Builder()
            .url(grantBonusUrl)
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.code == 429) {
                    val json = JSONObject(body)
                    return TtsBonusApiResult.RewardLimitReached(
                        quotaUsed = json.optInt("quotaUsed", 0),
                        quotaLimit = json.optInt("quotaLimit", TtsQuotaStore.DEFAULT_LIMIT),
                    )
                }

                if (!response.isSuccessful) {
                    val message = runCatching {
                        JSONObject(body).optString("error", "Bonus request failed.")
                    }.getOrDefault("Bonus request failed.")
                    return TtsBonusApiResult.Error(message)
                }

                val json = JSONObject(body)
                TtsBonusApiResult.Success(
                    bonusGranted = json.optInt("bonusGranted", 0),
                    quotaUsed = json.optInt("quotaUsed", 0),
                    quotaLimit = json.optInt("quotaLimit", TtsQuotaStore.DEFAULT_LIMIT),
                )
            }
        } catch (error: Exception) {
            TtsBonusApiResult.Error(error.message ?: "Network error")
        }
    }
}
