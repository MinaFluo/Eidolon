package io.citmina.proxychat.util

import android.content.Context

class ApiKeyManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun getStoredApiKey(): String? = sharedPreferences.getString("api_key", null)

    fun storeApiKey(apiKey: String) {
        sharedPreferences.edit().putString("api_key", apiKey).apply()
    }

    fun getStoredBaseUrl(): String? = sharedPreferences.getString("base_url", null)

    fun storeBaseUrl(baseUrl: String) {
        sharedPreferences.edit().putString("base_url", baseUrl).apply()
    }
} 