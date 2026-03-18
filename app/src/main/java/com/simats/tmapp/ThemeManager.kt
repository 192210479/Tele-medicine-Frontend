package com.simats.tmapp

import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    fun applyTheme(sessionManager: SessionManager) {
        val isDark = sessionManager.isDarkMode()
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun forceLightMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    fun toggleTheme(sessionManager: SessionManager, enabled: Boolean) {
        sessionManager.saveDarkMode(enabled)
        applyTheme(sessionManager)
    }
}
