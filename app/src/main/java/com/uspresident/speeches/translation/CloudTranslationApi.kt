package com.uspresident.speeches.translation

import com.uspresident.speeches.data.TranslationQuotaStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class TranslationApiResult {
    data class Success(
        val translatedText: String,
        val fromCache: Boolean,
        val quotaUsed: Int,
        val quotaLimit: Int,
    ) : TranslationApiResult()

    data class QuotaExceeded(
        val quotaUsed: Int,
        val quotaLimit: Int,
    ) : TranslationApiResult()

    data class Error(val message: String) : TranslationApiResult()
}

sealed class BonusApiResult {
    data class Success(
        val bonusGranted: Int,
        val quotaUsed: Int,
        val quotaLimit: Int,
    ) : BonusApiResult()

    data class RewardLimitReached(
        val quotaUsed: Int,
        val quotaLimit: Int,
    ) : BonusApiResult()

    data class Error(val message: String) : BonusApiResult()
}

class CloudTranslationApi(
    private val translateUrl: String,
    private val grantBonusUrl: String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun isConfigured(): Boolean {
        return translateUrl.isNotBlank() && grantBonusUrl.isNotBlank()
    }

    fun translate(text: String, clientId: String): TranslationApiResult {
        if (!isConfigured()) {
            return TranslationApiResult.Error("Translation service is not configured.")
        }

        val payload = JSONObject()
            .put("text", text)
            .put("clientId", clientId)
            .toString()

        val request = Request.Builder()
            .url(translateUrl)
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.code == 429) {
                    val json = JSONObject(body)
                    return TranslationApiResult.QuotaExceeded(
                        quotaUsed = json.optInt("quotaUsed", TranslationQuotaStore.DEFAULT_LIMIT),
                        quotaLimit = json.optInt("quotaLimit", TranslationQuotaStore.DEFAULT_LIMIT),
                    )
                }

                if (!response.isSuccessful) {
                    val message = runCatching {
                        JSONObject(body).optString("error", "Translation failed.")
                    }.getOrDefault("Translation failed.")
                    return TranslationApiResult.Error(message)
                }

                val json = JSONObject(body)
                TranslationApiResult.Success(
                    translatedText = json.getString("translatedText"),
                    fromCache = json.optBoolean("fromCache", false),
                    quotaUsed = json.optInt("quotaUsed", 0),
                    quotaLimit = json.optInt("quotaLimit", TranslationQuotaStore.DEFAULT_LIMIT),
                )
            }
        } catch (error: Exception) {
            TranslationApiResult.Error(error.message ?: "Network error")
        }
    }

    fun grantBonus(clientId: String): BonusApiResult {
        if (!isConfigured()) {
            return BonusApiResult.Error("Translation service is not configured.")
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
                    return BonusApiResult.RewardLimitReached(
                        quotaUsed = json.optInt("quotaUsed", 0),
                        quotaLimit = json.optInt("quotaLimit", TranslationQuotaStore.DEFAULT_LIMIT),
                    )
                }

                if (!response.isSuccessful) {
                    val message = runCatching {
                        JSONObject(body).optString("error", "Bonus request failed.")
                    }.getOrDefault("Bonus request failed.")
                    return BonusApiResult.Error(message)
                }

                val json = JSONObject(body)
                BonusApiResult.Success(
                    bonusGranted = json.optInt("bonusGranted", 0),
                    quotaUsed = json.optInt("quotaUsed", 0),
                    quotaLimit = json.optInt("quotaLimit", TranslationQuotaStore.DEFAULT_LIMIT),
                )
            }
        } catch (error: Exception) {
            BonusApiResult.Error(error.message ?: "Network error")
        }
    }
}
