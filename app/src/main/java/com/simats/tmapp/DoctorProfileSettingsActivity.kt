package com.simats.tmapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.imageview.ShapeableImageView
import androidx.activity.viewModels

class DoctorProfileSettingsActivity : AppCompatActivity() {
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private var ivAvatar: ShapeableImageView? = null
    private var tvName: TextView? = null
    private var tvEmail: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_profile_settings)
        
        sessionManager = SessionManager.getInstance(this)
        
        // Role Verification
        val currentRole = sessionManager.getUserRole().lowercase()
        if (!currentRole.contains("doctor")) {
            val intent = Intent(this, sessionManager.getDashboardActivity())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        ivAvatar = findViewById(R.id.ivAvatar)
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)

        updateProfileUI()

        val llEditProfile = findViewById<LinearLayout>(R.id.llEditProfile)
        val llNotifications = findViewById<LinearLayout>(R.id.llNotifications)
        val llPrivacy = findViewById<LinearLayout>(R.id.llPrivacy)
        val llAboutApp = findViewById<LinearLayout>(R.id.llAboutApp)
        val llLogout = findViewById<LinearLayout>(R.id.llLogout)

        llEditProfile?.setOnClickListener {
            startActivity(Intent(this, EditDoctorProfileActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.llPayments).setOnClickListener {
            startActivity(Intent(this, PaymentsActivity::class.java))
        }

        llNotifications?.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            intent.putExtra("userRole", "doctor")
            startActivity(intent)
        }

        llPrivacy?.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }

        llAboutApp?.setOnClickListener {
            startActivity(Intent(this, AboutAppActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.llHelpSupport).setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.llEmergencyHelp).setOnClickListener {
            startActivity(Intent(this, EmergencyHelpActivity::class.java))
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

        llLogout?.setOnClickListener {
            showLogoutDialog()
        }
        
        fetchDoctorProfile()
    }

    private fun fetchDoctorProfile() {
        val userId = sessionManager.getUserId()
        viewModel.fetchProfile(userId, "doctor")
        viewModel.profile.observe(this) { profile ->
            if (profile != null) {
                sessionManager.saveUserName(profile.name)
                sessionManager.saveUserEmail(profile.email)
                sessionManager.saveUserDetails(profile.age, profile.gender, profile.phone, profile.address)
                updateProfileUI()
            }
        }
    }

    private fun updateProfileUI() {
        tvName?.text = sessionManager.getUserName()
        tvEmail?.text = sessionManager.getUserEmail()

        sessionManager.getUserAvatar()?.let { avatarUri ->
            if (avatarUri.isNotEmpty()) {
                try {
                    val uri = Uri.parse(avatarUri)
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
        updateProfileUI()
        val userId = sessionManager.getUserId()
        viewModel.fetchProfile(userId, "doctor")
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
            SocketManager.getInstance(this@DoctorProfileSettingsActivity).disconnect()
            sessionManager.logout()
            val intent = Intent(this, AuthWelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        dialog.show()
    }
}
