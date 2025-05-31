package com.example.nebo.config

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object SessionManager {

    private const val AUTH_TOKEN_KEY = "auth_token"
    private const val REFRESH_TOKEN_KEY = "refresh_token"

    private fun getPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun getAuthToken(context: Context): String? {
        return getPrefs(context).getString(AUTH_TOKEN_KEY, null)
    }

    fun refreshToken(context: Context): String {
        // Реализация обновления токена
        return "new_refreshed_token"
    }

    fun saveTokens(context: Context, authToken: String, refreshToken: String) {
        getPrefs(context).edit()
            .putString(AUTH_TOKEN_KEY, authToken)
            .putString(REFRESH_TOKEN_KEY, refreshToken)
            .apply()
    }
}