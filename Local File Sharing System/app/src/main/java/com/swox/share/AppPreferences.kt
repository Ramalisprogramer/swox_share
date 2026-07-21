package com.swox.share

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("swox_share_prefs", Context.MODE_PRIVATE)

    var themeMode: String
        get() = prefs.getString(KEY_THEME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    fun getTransferHistory(): List<String> {
        return prefs.getStringSet(KEY_HISTORY, emptySet())?.toList()?.sortedDescending() ?: emptyList()
    }

    fun addTransferHistory(entry: String) {
        val current = getTransferHistory().toMutableSet()
        current.add("${System.currentTimeMillis()}|$entry")
        prefs.edit().putStringSet(KEY_HISTORY, current).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun getSecurityLogs(): List<String> {
        return prefs.getStringSet(KEY_SECURITY_LOGS, emptySet())?.toList()?.sortedDescending()
            ?: emptyList()
    }

    fun addSecurityLog(message: String) {
        val current = getSecurityLogs().toMutableSet()
        current.add("${System.currentTimeMillis()}|$message")
        prefs.edit()
            .putStringSet(KEY_SECURITY_LOGS, current.toList().takeLast(100).toSet())
            .apply()
    }

    fun getTransferLogs(): List<String> {
        return prefs.getStringSet(KEY_TRANSFER_LOGS, emptySet())?.toList()?.sortedDescending()
            ?: emptyList()
    }

    fun addTransferLog(message: String) {
        val current = getTransferLogs().toMutableSet()
        current.add("${System.currentTimeMillis()}|$message")
        prefs.edit()
            .putStringSet(KEY_TRANSFER_LOGS, current.toList().takeLast(200).toSet())
            .apply()
    }

    fun resetSettings() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_HISTORY = "transfer_history"
        private const val KEY_SECURITY_LOGS = "security_logs"
        private const val KEY_TRANSFER_LOGS = "transfer_logs"
    }
}
