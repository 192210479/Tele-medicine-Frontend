package com.simats.Tmapp

import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    fun applyTheme(sessionManager: SessionManager) {
        // Dark mode has been permanently removed as per user request.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    fun forceLightMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    fun toggleTheme(sessionManager: SessionManager, enabled: Boolean) {
        sessionManager.saveDarkMode(enabled)
        applyTheme(sessionManager)
    }
}
