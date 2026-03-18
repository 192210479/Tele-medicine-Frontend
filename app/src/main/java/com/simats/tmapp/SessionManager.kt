package com.simats.tmapp

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("TMAppSession", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_AVATAR = "user_avatar_uri"
        private const val KEY_UNREAD_NOTIFICATIONS = "unread_notifications"
        private const val KEY_ONBOARDING_SEEN = "onboarding_seen"
        private const val KEY_THEME = "telehealth_theme"
        private const val KEY_ROOM_CONNECTED = "user_room_connected"
        
        private const val KEY_USER_AGE = "user_age"
        private const val KEY_USER_GENDER = "user_gender"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_ADDRESS = "user_address"
        private const val KEY_USER_SPECIALIZATION = "user_specialization"
        private const val KEY_USER_BIO = "user_bio"
        private const val KEY_USER_EXPERIENCE = "user_experience"
        private const val KEY_USER_FEE = "user_fee"
        private const val KEY_USER_LANGUAGES = "user_languages"
        
        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context).also { instance = it }
            }
        }
    }

    fun saveUserRole(role: String?) {
        prefs.edit().putString(KEY_USER_ROLE, role ?: "Patient").apply()
    }

    fun getUserRole(): String {
        return prefs.getString(KEY_USER_ROLE, "Patient") ?: "Patient"
    }

    fun saveUserEmail(email: String?) {
        prefs.edit().putString(KEY_USER_EMAIL, email ?: "").apply()
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun saveUserName(name: String?) {
        prefs.edit().putString(KEY_USER_NAME, name ?: "").apply()
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "") ?: ""
    }

    fun saveUserId(id: Int) {
        prefs.edit().putInt(KEY_USER_ID, id).apply()
    }

    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun setOnboardingSeen(seen: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, seen).apply()
    }

    fun hasSeenOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_SEEN, false)
    }

    fun saveUserAvatar(uri: String?) {
        prefs.edit().putString(KEY_USER_AVATAR, uri).apply()
    }

    fun getUserAvatar(): String? {
        return prefs.getString(KEY_USER_AVATAR, null)
    }

    fun saveUserDetails(age: Int?, gender: String?, phone: String?, address: String?) {
        prefs.edit().apply {
            putInt(KEY_USER_AGE, age ?: -1)
            putString(KEY_USER_GENDER, gender)
            putString(KEY_USER_PHONE, phone)
            putString(KEY_USER_ADDRESS, address)
            apply()
        }
    }

    fun saveDoctorDetails(specialization: String?, experience: Int?, fee: Double?, languages: String?, bio: String?) {
        prefs.edit().apply {
            putString(KEY_USER_SPECIALIZATION, specialization)
            putInt(KEY_USER_EXPERIENCE, experience ?: 0)
            putFloat(KEY_USER_FEE, (fee ?: 0.0).toFloat())
            putString(KEY_USER_LANGUAGES, languages)
            putString(KEY_USER_BIO, bio)
            apply()
        }
    }

    fun getUserSpecialization(): String? = prefs.getString(KEY_USER_SPECIALIZATION, "")
    fun getUserBio(): String? = prefs.getString(KEY_USER_BIO, "")
    fun getUserExperience(): Int = prefs.getInt(KEY_USER_EXPERIENCE, 0)
    fun getUserFee(): Double = prefs.getFloat(KEY_USER_FEE, 0.0f).toDouble()
    fun getUserLanguages(): String? = prefs.getString(KEY_USER_LANGUAGES, "")

    fun getUserAge(): Int? {
        val age = prefs.getInt(KEY_USER_AGE, -1)
        return if (age == -1) null else age
    }

    fun getUserGender(): String? = prefs.getString(KEY_USER_GENDER, "Male")
    fun getUserPhone(): String? = prefs.getString(KEY_USER_PHONE, "")
    fun getUserAddress(): String? = prefs.getString(KEY_USER_ADDRESS, "")

    fun hasUnreadNotifications(): Boolean {
        return prefs.getBoolean(KEY_UNREAD_NOTIFICATIONS, true)
    }

    fun markNotificationsRead() {
        prefs.edit().putBoolean(KEY_UNREAD_NOTIFICATIONS, false).apply()
    }

    fun setHasUnreadNotifications(hasUnread: Boolean) {
        prefs.edit().putBoolean(KEY_UNREAD_NOTIFICATIONS, hasUnread).apply()
    }

    fun isRoomConnected(): Boolean {
        return prefs.getBoolean(KEY_ROOM_CONNECTED, false)
    }

    fun setRoomConnected(connected: Boolean) {
        prefs.edit().putBoolean(KEY_ROOM_CONNECTED, connected).apply()
    }

    fun saveDarkMode(isEnabled: Boolean) {
        val theme = if (isEnabled) "dark" else "light"
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun isDarkMode(): Boolean {
        return prefs.getString(KEY_THEME, "light") == "dark"
    }

    fun saveBiometricEnabled(role: String, isEnabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled_$role", isEnabled).apply()
    }

    fun isBiometricEnabled(role: String): Boolean {
        return prefs.getBoolean("biometric_enabled_$role", true)
    }

    fun getDashboardActivity(): Class<*> {
        val role = getUserRole().lowercase()
        return when {
            role.contains("doctor") -> DoctorDashboardActivity::class.java
            role.contains("admin") -> AdminDashboardActivity::class.java
            else -> PatientDashboardActivity::class.java
        }
    }

    fun logout() {
        val onboardingSeen = hasSeenOnboarding()
        
        // Dark mode is now stored as "dark_mode_$userId".
        // To preserve it properly on logout without knowing all userIds, we could iterate via prefs.all
        // But the simplest fix without breaking user expectations is to just preserve the current modes mapping if needed, or simply let clear() wipe them since it's a device logout.
        // Actually, we should selectively clear to prevent losing dark mode settings for other users on this device.
        val editor = prefs.edit()
        
        // Remove known session keys instead of clearing everything
        editor.remove("IS_LOGIN")
        editor.remove(KEY_USER_ID)
        editor.remove(KEY_USER_NAME)
        editor.remove(KEY_USER_EMAIL)
        editor.remove(KEY_USER_ROLE)
        editor.remove(KEY_USER_AVATAR)
        editor.remove(KEY_USER_ADDRESS)
        editor.apply()
    }
}
