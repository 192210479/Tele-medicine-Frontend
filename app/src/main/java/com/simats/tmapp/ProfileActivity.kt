package com.simats.tmapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.activity.viewModels
import android.view.View

class ProfileActivity : AppCompatActivity() {
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private var tvUserName: TextView? = null
    private var tvUserEmail: TextView? = null
    private var ivAvatar: ImageView? = null
    private var tvUserRole: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        sessionManager = SessionManager.getInstance(this)

        // Role Verification
        val currentRole = sessionManager.getUserRole().lowercase()
        if (!currentRole.contains("patient")) {
            val intent = Intent(this, sessionManager.getDashboardActivity())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        
        // Find the MaterialCardView and then find the ImageView inside it
        val flAvatar = findViewById<View>(R.id.flAvatar)
        ivAvatar = flAvatar?.findViewById(R.id.ivAvatar)

        tvUserRole = findViewById(R.id.tvUserRole)

        val ivBack = findViewById<ImageView?>(R.id.ivBack)
        val llLogout = findViewById<LinearLayout>(R.id.llLogout)
        
        val llEditProfile = findViewById<LinearLayout>(R.id.llEditProfile)
        val llPayments = findViewById<LinearLayout>(R.id.llPayments)
        val llNotifications = findViewById<LinearLayout>(R.id.llNotifications)
        val llPrivacy = findViewById<LinearLayout>(R.id.llPrivacy)
        val llAboutApp = findViewById<LinearLayout>(R.id.llAboutApp)

        ivBack?.setOnClickListener {
            finish()
        }

        llEditProfile?.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        llPayments?.setOnClickListener {
            startActivity(Intent(this, PaymentsActivity::class.java))
        }

        llNotifications?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        llPrivacy?.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }

        llAboutApp?.setOnClickListener {
            startActivity(Intent(this, AboutAppActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.llHelpSupport)?.setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }
        
        findViewById<LinearLayout>(R.id.llEmergencyHelp)?.setOnClickListener {
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
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_logout)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            
            val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancel)
            val btnLogoutConfirm = dialog.findViewById<MaterialButton>(R.id.btnLogoutConfirm)
            val ivClose = dialog.findViewById<ImageView>(R.id.ivClose)
            
            btnCancel.setOnClickListener { dialog.dismiss() }
            ivClose.setOnClickListener { dialog.dismiss() }
            
            btnLogoutConfirm.setOnClickListener {
                dialog.dismiss()
                SocketManager.getInstance(this@ProfileActivity).disconnect()
                sessionManager.logout()
                val intent = Intent(this, AuthWelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            dialog.show()
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
        viewModel.fetchProfile(sessionManager.getUserId(), sessionManager.getUserRole()?.lowercase() ?: "patient")
    }

    private fun syncUI() {
        tvUserName?.text = sessionManager.getUserName()
        tvUserEmail?.text = sessionManager.getUserEmail()
        tvUserRole?.text = sessionManager.getUserRole()
        
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
        syncUI()
        viewModel.fetchProfile(sessionManager.getUserId(), sessionManager.getUserRole()?.lowercase() ?: "patient")
    }
}
