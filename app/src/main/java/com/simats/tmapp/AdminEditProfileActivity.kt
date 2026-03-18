package com.simats.tmapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.viewModels
import com.simats.tmapp.api.UpdateProfileRequest

class AdminEditProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmailAddress: TextInputEditText
    private lateinit var ivAvatar: ImageView
    private var selectedImageUri: Uri? = null
    private val viewModel: ProfileViewModel by viewModels()

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_edit_profile)
        sessionManager = SessionManager.getInstance(this)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val navDashboard = findViewById<LinearLayout>(R.id.navDashboard)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        etFullName = findViewById(R.id.etFullName)
        etEmailAddress = findViewById(R.id.etEmailAddress)
        ivAvatar = findViewById(R.id.ivAvatar)
        val flAvatarEdit = findViewById<FrameLayout>(R.id.flAvatarEdit)

        // Initial setup from session
        etFullName.setText(sessionManager.getUserName())
        etEmailAddress.setText(sessionManager.getUserEmail())
        
        // Load avatar if exists
        loadAvatar()

        flAvatarEdit.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        viewModel.fetchProfile(sessionManager.getUserId(), "admin")
        viewModel.profile.observe(this) { profile ->
            if (profile != null) {
                etFullName.setText(profile.name)
                etEmailAddress.setText(profile.email)
                sessionManager.saveUserName(profile.name)
                sessionManager.saveUserEmail(profile.email)
            }
        }

        viewModel.updateStatus.observe(this) { (success, error) ->
            if (success) {
                val newName = etFullName.text.toString().trim()
                sessionManager.saveUserName(newName)
                Toast.makeText(this@AdminEditProfileActivity, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@AdminEditProfileActivity, error ?: "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }

        ivBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            saveProfileChanges()
        }

        navDashboard.setOnClickListener {
            val intent = Intent(this, AdminDashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        navProfile.setOnClickListener {
            finish()
        }
    }

    private fun loadAvatar() {
        sessionManager.getUserAvatar()?.let { avatarUri ->
            if (avatarUri.isNotEmpty()) {
                try {
                    val uri = Uri.parse(avatarUri)
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            ivAvatar.setImageBitmap(bitmap)
                        } else {
                            ivAvatar.setImageResource(R.drawable.bg_image_mock)
                        }
                    }
                } catch (e: Exception) {
                    ivAvatar.setImageResource(R.drawable.bg_image_mock)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            try {
                contentResolver.openInputStream(selectedImageUri!!)?.use { inputStream ->
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    ivAvatar.setImageBitmap(bitmap)
                    sessionManager.saveUserAvatar(selectedImageUri.toString())
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveProfileChanges() {
        val newName = etFullName.text.toString().trim()
        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val request = UpdateProfileRequest(
            userId = sessionManager.getUserId(),
            role = sessionManager.getUserRole().lowercase(),
            name = newName
        )
        viewModel.updateProfile(request)
    }
}
