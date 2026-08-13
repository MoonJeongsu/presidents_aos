package com.uspresident.speeches.data

import android.content.Context

class TranslationQuotaStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveQuota(used: Int, limit: Int) {
        prefs.edit()
            .putInt(KEY_QUOTA_USED, used)
            .putInt(KEY_QUOTA_LIMIT, limit)
            .apply()
    }

    fun getQuotaUsed(): Int = prefs.getInt(KEY_QUOTA_USED, 0)

    fun getQuotaLimit(): Int = prefs.getInt(KEY_QUOTA_LIMIT, DEFAULT_LIMIT)

    companion object {
        private const val PREFS_NAME = "presidential_speeches_prefs"
        private const val KEY_QUOTA_USED = "translation_quota_used"
        private const val KEY_QUOTA_LIMIT = "translation_quota_limit"
        const val DEFAULT_LIMIT = 40
    }
}
