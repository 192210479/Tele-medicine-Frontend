package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.simats.Tmapp.api.ApiClient

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


        llHelpSupport?.setOnClickListener {
            try { startActivity(Intent(this, HelpSupportActivity::class.java)) } catch (e: Exception) { e.printStackTrace() }
        }

        llAboutApp?.setOnClickListener {
            try { startActivity(Intent(this, AboutAppActivity::class.java)) } catch (e: Exception) { e.printStackTrace() }
        }



        ivEditAvatar?.setOnClickListener {
            try { startActivity(Intent(this, AdminEditProfileActivity::class.java)) } catch (e: Exception) { e.printStackTrace() }
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
        val name = sessionManager.getUserName()
        tvUserName?.text = name
        tvUserEmail?.text = sessionManager.getUserEmail()

        ivAvatar?.let {
            val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
            val avatarVersion = sessionManager.getAvatarVersion()
            val photoUrl = "$baseUrl/api/profile/image/${sessionManager.getUserId()}?role=admin&v=$avatarVersion"

            AvatarUtils.loadAvatar(it, photoUrl, name.ifEmpty { "Admin" })
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
            // Reset to light mode so auth screens open unaffected by this account's pref
            ThemeManager.applyTheme(sessionManager)
            val intent = Intent(this, AuthWelcomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        dialog.show()
    }
}
