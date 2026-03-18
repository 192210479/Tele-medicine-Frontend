package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class AdminProfileActivity : AppCompatActivity() {
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private var ivAvatar: android.widget.ImageView? = null
    private var tvUserName: android.widget.TextView? = null
    private var tvUserEmail: android.widget.TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_profile)
        
        sessionManager = SessionManager.getInstance(this)
        
        ivAvatar = findViewById(R.id.ivAvatar)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        
        // Role Verification
        val currentRole = sessionManager.getUserRole().lowercase()
        if (!currentRole.contains("admin")) {
            val intent = Intent(this, sessionManager.getDashboardActivity())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        val llEditProfile = findViewById<LinearLayout>(R.id.llEditProfile)
        val llPayments = findViewById<LinearLayout>(R.id.llPayments)
        val llNotifications = findViewById<LinearLayout>(R.id.llNotifications)
        val llPrivacy = findViewById<LinearLayout>(R.id.llPrivacy)
        val llEmergencyHelp = findViewById<LinearLayout>(R.id.llEmergencyHelp)
        val llHelpSupport = findViewById<LinearLayout>(R.id.llHelpSupport)
        val llLanguage = findViewById<LinearLayout>(R.id.llLanguage)
        val ivEditAvatar = findViewById<android.view.View>(R.id.ivEditAvatar)
        val llAboutApp = findViewById<LinearLayout>(R.id.llAboutApp)
        val llLogout = findViewById<LinearLayout>(R.id.llLogout)

        llEditProfile?.setOnClickListener {
            try { startActivity(Intent(this, AdminEditProfileActivity::class.java)) } catch (e: Exception) { e.printStackTrace() }
        }

        llPayments?.setOnClickListener {
            try { startActivity(Intent(this, PaymentsActivity::class.java)) } catch (e: Exception) { e.printStackTrace() }
        }

        llNotifications?.setOnClickListener {
            try { startActivity(Intent(this, NotificationsActivity::class.java)) } catch (e: Exception) { e.printStackTrace() }
        }

        llPrivacy?.setOnClickListener {
            try { startActivity(Intent(this, PrivacyActivity::class.java)) } catch (e: Exception) { e.printStackTrace() }
        }

        llEmergencyHelp?.setOnClickListener {
            try { startActivity(Intent(this, EmergencyHelpActivity::class.java)) } catch (e: Exception) { e.printStackTrace() }
        }

        llHelpSupport?.setOnClickListener {
            try { startActivity(Intent(this, HelpSupportActivity::class.java)) } catch (e: Exception) { e.printStackTrace() }
        }

        llAboutApp?.setOnClickListener {
            try { startActivity(Intent(this, AboutAppActivity::class.java)) } catch (e: Exception) { e.printStackTrace() }
        }

        llLanguage?.setOnClickListener {
            android.widget.Toast.makeText(this, "Language Selection Coming Soon", android.widget.Toast.LENGTH_SHORT).show()
        }

        ivEditAvatar?.setOnClickListener {
            try { startActivity(Intent(this, AdminEditProfileActivity::class.java)) } catch (e: Exception) { e.printStackTrace() }
        }

        val swDarkMode = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swDarkMode)
        val ivDarkModeIcon = findViewById<ImageView>(R.id.ivDarkModeIcon)
        
        val isCurrentlyDark = sessionManager.isDarkMode()
        swDarkMode?.isChecked = isCurrentlyDark
        ivDarkModeIcon?.setImageResource(if (isCurrentlyDark) R.drawable.ic_dark_mode else R.drawable.ic_light_mode)
        
        swDarkMode?.setOnCheckedChangeListener { _, isChecked ->
            ivDarkModeIcon?.setImageResource(if (isChecked) R.drawable.ic_dark_mode else R.drawable.ic_light_mode)
            ThemeManager.toggleTheme(sessionManager, isChecked)
        }

        val swBiometric = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swBiometric)
        swBiometric?.isChecked = sessionManager.isBiometricEnabled("Admin")
        swBiometric?.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.saveBiometricEnabled("Admin", isChecked)
        }

        llLogout?.setOnClickListener {
            showLogoutDialog()
        }
        viewModel.profile.observe(this) { profile ->
            if (profile != null) {
                sessionManager.saveUserName(profile.name)
                sessionManager.saveUserEmail(profile.email)
                sessionManager.saveUserDetails(profile.age, profile.gender, profile.phone, profile.address)
                syncUI()
            }
        }
        
        syncUI()
        viewModel.fetchProfile(sessionManager.getUserId(), "admin")
    }

    private fun syncUI() {
        tvUserName?.text = sessionManager.getUserName()
        tvUserEmail?.text = sessionManager.getUserEmail()

        sessionManager.getUserAvatar()?.let { avatarUri ->
            if (avatarUri.isNotEmpty()) {
                try {
                    val uri = android.net.Uri.parse(avatarUri)
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            ivAvatar?.setImageBitmap(bitmap)
                        } else {
                            ivAvatar?.setImageResource(R.drawable.bg_circle_soft_blue)
                        }
                    } ?: run {
                        ivAvatar?.setImageResource(R.drawable.bg_circle_soft_blue)
                    }
                } catch (e: Exception) {
                    ivAvatar?.setImageResource(R.drawable.bg_circle_soft_blue)
                }
            } else {
                ivAvatar?.setImageResource(R.drawable.bg_circle_soft_blue)
            }
        } ?: run {
            ivAvatar?.setImageResource(R.drawable.bg_circle_soft_blue)
        }
    }

    override fun onResume() {
        super.onResume()
        syncUI()
        viewModel.fetchProfile(sessionManager.getUserId(), "admin")
    }

    private fun showLogoutDialog() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_logout)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnLogoutConfirm = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLogoutConfirm)
        val ivClose = dialog.findViewById<android.widget.ImageView>(R.id.ivClose)

        btnCancel.setOnClickListener { dialog.dismiss() }
        ivClose.setOnClickListener { dialog.dismiss() }

        btnLogoutConfirm.setOnClickListener {
            dialog.dismiss()
            SocketManager.getInstance(this@AdminProfileActivity).disconnect()
            sessionManager.logout()
            val intent = Intent(this, AuthWelcomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        dialog.show()
    }
}
