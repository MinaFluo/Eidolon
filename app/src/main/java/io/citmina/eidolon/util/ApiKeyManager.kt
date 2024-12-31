package io.citmina.eidolon.util

import android.content.Context

class ApiKeyManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun getStoredApiKey(): String? = sharedPreferences.getString("api_key", null)

    fun storeApiKey(apiKey: String) {
        sharedPreferences.edit().putString("api_key", apiKey).apply()
    }
} 