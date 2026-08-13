package com.uspresident.speeches.data

import android.content.Context
import java.util.UUID

class ClientIdProvider(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getClientId(): String {
        val existing = prefs.getString(KEY_CLIENT_ID, null)
        if (existing != null) {
            return existing
        }

        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_CLIENT_ID, created).apply()
        return created
    }

    companion object {
        private const val PREFS_NAME = "presidential_speeches_prefs"
        private const val KEY_CLIENT_ID = "translation_client_id"
    }
}
