package com.simats.tmapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import androidx.activity.viewModels
import com.simats.tmapp.api.ProfileResponse
import com.simats.tmapp.api.UpdateProfileRequest

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAge: EditText
    private lateinit var tvGender: TextView
    private lateinit var llGender: LinearLayout
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnSave: MaterialButton
    private lateinit var ivAvatar: ImageView
    private lateinit var sessionManager: SessionManager
    private val viewModel: ProfileViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            imageUri?.let {
                // To persist access to the URI after reboot or in other activities
                try {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {}
                
                ivAvatar.setImageURI(it)
                sessionManager.saveUserAvatar(it.toString())
                Toast.makeText(this, "Photo updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager.getInstance(this)

        // Initialize Views
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etAge = findViewById(R.id.etAge)
        tvGender = findViewById(R.id.tvGender)
        llGender = findViewById(R.id.llGender)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        btnSave = findViewById(R.id.btnSave)
        ivAvatar = findViewById(R.id.ivAvatar)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener { finish() }

        // Avatar Click - Allow clicking the FrameLayout or the ImageView
        findViewById<android.view.View>(R.id.cvAvatarEdit)?.setOnClickListener {
            openImagePicker()
        }
        ivAvatar.setOnClickListener {
            openImagePicker()
        }

        // Set existing avatar if any
        syncAvatar()

        // Gender Selection
        llGender.setOnClickListener {
            showGenderDialog()
        }

        // Pre-fill with session data first (for immediate responsiveness)
        etFullName.setText(sessionManager.getUserName())
        etEmail.setText(sessionManager.getUserEmail())
        etAge.setText(sessionManager.getUserAge()?.toString() ?: "")
        tvGender.text = sessionManager.getUserGender()
        etPhone.setText(sessionManager.getUserPhone())
        etAddress.setText(sessionManager.getUserAddress())

        // Email field is read-only as per policy
        etEmail.isEnabled = false
        etEmail.isFocusable = false
        etEmail.alpha = 0.7f

        // Fetch current profile data from backend to ensure synchronization
        fetchProfile()

        // Observers
        viewModel.profile.observe(this) { profile ->
            if (profile != null) {
                etFullName.setText(profile.name)
                etEmail.setText(profile.email)
                profile.age?.let { etAge.setText(it.toString()) }
                profile.gender?.let { tvGender.text = it }
                profile.phone?.let { etPhone.setText(it) }
                profile.address?.let { etAddress.setText(it) }

                sessionManager.saveUserName(profile.name)
                sessionManager.saveUserDetails(profile.age, profile.gender, profile.phone, profile.address)
            }
        }

        viewModel.updateStatus.observe(this) { (success, error) ->
            if (success) {
                val name = etFullName.text.toString().trim()
                val ageStr = etAge.text.toString().trim()
                val age = if (ageStr.isNotEmpty()) ageStr.toInt() else null
                sessionManager.saveUserName(name)
                sessionManager.saveUserDetails(age, tvGender.text.toString(), etPhone.text.toString().trim(), etAddress.text.toString().trim())
                
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                viewModel.fetchProfile(sessionManager.getUserId(), sessionManager.getUserRole() ?: "patient")
            } else {
                Toast.makeText(this, error ?: "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }

        btnSave.setOnClickListener { updateProfile() }

        // Navigation (Dynamic based on role)
        val userRole = sessionManager.getUserRole()?.lowercase() ?: "patient"

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val destination = when (userRole) {
                "doctor" -> DoctorDashboardActivity::class.java
                "admin" -> AdminDashboardActivity::class.java
                else -> PatientDashboardActivity::class.java
            }
            val intent = Intent(this, destination)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
        
        findViewById<LinearLayout>(R.id.navBook).setOnClickListener {
            if (userRole == "patient") {
                startActivity(Intent(this, SelectDoctorActivity::class.java))
            } else {
                Toast.makeText(this, "Only patients can book appointments", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<LinearLayout>(R.id.navHistory).setOnClickListener {
            startActivity(Intent(this, ConsultationHistoryActivity::class.java))
        }
        
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            finish()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun syncAvatar() {
        sessionManager.getUserAvatar()?.let {
            try {
                ivAvatar.setImageURI(Uri.parse(it))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showGenderDialog() {
        val genders = arrayOf("Male", "Female", "Other")
        AlertDialog.Builder(this)
            .setTitle("Select Gender")
            .setItems(genders) { _, which ->
                tvGender.text = genders[which]
            }
            .show()
    }

    private fun fetchProfile() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole()?.lowercase() ?: "patient"
        if (userId != -1) {
            viewModel.fetchProfile(userId, role)
        }
    }

    private fun updateProfile() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole()?.lowercase() ?: "patient"

        val name = etFullName.text.toString().trim()
        val ageStr = etAge.text.toString().trim()
        val age = if (ageStr.isNotEmpty()) ageStr.toInt() else null
        val gender = tvGender.text.toString()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()

        if (name.isEmpty()) {
            etFullName.error = "Full Name is required"
            return
        }

        val updateRequest = UpdateProfileRequest(
            userId = userId,
            role = role,
            name = name,
            age = age,
            gender = gender,
            phone = phone,
            address = address
        )

        viewModel.updateProfile(updateRequest)
    }
}
