package com.uspresident.speeches.data

import com.uspresident.speeches.data.local.TranslationCacheDao
import com.uspresident.speeches.data.local.TranslationCacheEntity
import com.uspresident.speeches.translation.BonusApiResult
import com.uspresident.speeches.translation.CloudTranslationApi
import com.uspresident.speeches.translation.TranslationApiResult
import com.uspresident.speeches.util.TextHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class TranslationResult {
    data class Success(
        val translatedText: String,
        val fromLocalCache: Boolean,
        val quotaUsed: Int,
        val quotaLimit: Int,
    ) : TranslationResult()

    data class QuotaExceeded(
        val quotaUsed: Int,
        val quotaLimit: Int,
    ) : TranslationResult()

    data class Error(val message: String) : TranslationResult()
}

sealed class BonusGrantResult {
    data class Success(val quotaUsed: Int, val quotaLimit: Int) : BonusGrantResult()
    data class RewardLimitReached(val quotaUsed: Int, val quotaLimit: Int) : BonusGrantResult()
    data class Error(val message: String) : BonusGrantResult()
}

class TranslationRepository(
    private val cacheDao: TranslationCacheDao,
    private val api: CloudTranslationApi,
    private val quotaStore: TranslationQuotaStore,
    private val clientIdProvider: ClientIdProvider,
) {
    suspend fun translate(text: String): TranslationResult = withContext(Dispatchers.IO) {
        val sourceHash = TextHash.sha256(text)

        cacheDao.getByHash(sourceHash)?.let { cached ->
            return@withContext TranslationResult.Success(
                translatedText = cached.translatedText,
                fromLocalCache = true,
                quotaUsed = quotaStore.getQuotaUsed(),
                quotaLimit = quotaStore.getQuotaLimit(),
            )
        }

        when (val result = api.translate(text, clientIdProvider.getClientId())) {
            is TranslationApiResult.Success -> {
                cacheDao.insert(
                    TranslationCacheEntity(
                        sourceHash = sourceHash,
                        sourceText = text,
                        translatedText = result.translatedText,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
                quotaStore.saveQuota(result.quotaUsed, result.quotaLimit)
                TranslationResult.Success(
                    translatedText = result.translatedText,
                    fromLocalCache = false,
                    quotaUsed = result.quotaUsed,
                    quotaLimit = result.quotaLimit,
                )
            }

            is TranslationApiResult.QuotaExceeded -> {
                quotaStore.saveQuota(result.quotaUsed, result.quotaLimit)
                TranslationResult.QuotaExceeded(
                    quotaUsed = result.quotaUsed,
                    quotaLimit = result.quotaLimit,
                )
            }

            is TranslationApiResult.Error -> TranslationResult.Error(result.message)
        }
    }

    suspend fun grantRewardBonus(): BonusGrantResult = withContext(Dispatchers.IO) {
        when (val result = api.grantBonus(clientIdProvider.getClientId())) {
            is BonusApiResult.Success -> {
                quotaStore.saveQuota(result.quotaUsed, result.quotaLimit)
                BonusGrantResult.Success(
                    quotaUsed = result.quotaUsed,
                    quotaLimit = result.quotaLimit,
                )
            }

            is BonusApiResult.RewardLimitReached -> {
                quotaStore.saveQuota(result.quotaUsed, result.quotaLimit)
                BonusGrantResult.RewardLimitReached(
                    quotaUsed = result.quotaUsed,
                    quotaLimit = result.quotaLimit,
                )
            }

            is BonusApiResult.Error -> BonusGrantResult.Error(result.message)
        }
    }

    fun getCachedQuotaUsed(): Int = quotaStore.getQuotaUsed()

    fun getCachedQuotaLimit(): Int = quotaStore.getQuotaLimit()
}
