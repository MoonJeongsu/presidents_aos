package com.uspresident.speeches.data

import android.content.Context

class TranslationQuotaStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveQuota(used: Int, limit: Int) {
        prefs.edit()
            .putInt(KEY_QUOTA_USED, used)
            .putInt(KEY_QUOTA_LIMIT, limit)
            .putString(KEY_DATE, QuotaUtcDate.today())
            .apply()
    }

    fun refreshIfNewUtcDay(): Boolean {
        val today = QuotaUtcDate.today()
        val storedDate = prefs.getString(KEY_DATE, null)
        if (storedDate == today) {
            return false
        }
        prefs.edit()
            .putInt(KEY_QUOTA_USED, 0)
            .putInt(KEY_QUOTA_LIMIT, DEFAULT_LIMIT)
            .putString(KEY_DATE, today)
            .apply()
        return true
    }

    fun getQuotaUsed(): Int {
        refreshIfNewUtcDay()
        return prefs.getInt(KEY_QUOTA_USED, 0)
    }

    fun getQuotaLimit(): Int {
        refreshIfNewUtcDay()
        return prefs.getInt(KEY_QUOTA_LIMIT, DEFAULT_LIMIT)
    }

    companion object {
        private const val PREFS_NAME = "presidential_speeches_prefs"
        private const val KEY_QUOTA_USED = "translation_quota_used"
        private const val KEY_QUOTA_LIMIT = "translation_quota_limit"
        private const val KEY_DATE = "translation_quota_date"
        const val DEFAULT_LIMIT = 40
    }
}
