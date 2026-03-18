package com.simats.tmapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import androidx.activity.viewModels
import com.simats.tmapp.api.UpdateProfileRequest

class EditDoctorProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var ivEditAvatar: ShapeableImageView
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etSpecialization: EditText
    private val viewModel: ProfileViewModel by viewModels()

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            selectedImageUri = data?.data
            ivEditAvatar.setImageURI(selectedImageUri)
            // Save the URI locally in SessionManager for persistence across sessions.
            sessionManager.saveUserAvatar(selectedImageUri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_doctor_profile)

        sessionManager = SessionManager.getInstance(this)

        ivEditAvatar = findViewById(R.id.ivEditAvatar)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etSpecialization = findViewById(R.id.etSpecialization)

        // Load existing data from SessionManager
        etFullName.setText(sessionManager.getUserName())
        etEmail.setText(sessionManager.getUserEmail())
        etPhone.setText(sessionManager.getUserPhone())
        etSpecialization.setText(sessionManager.getUserSpecialization())

        sessionManager.getUserAvatar()?.let {
            if (it.isNotEmpty()) ivEditAvatar.setImageURI(Uri.parse(it))
        }

        // Fetch exact latest info
        viewModel.fetchProfile(sessionManager.getUserId(), "doctor")
        viewModel.profile.observe(this) { profile ->
            if (profile != null) {
                etFullName.setText(profile.name)
                etEmail.setText(profile.email)
                profile.phone?.let { etPhone.setText(it) }
                profile.specialization?.let { etSpecialization.setText(it) }
                
                sessionManager.saveUserName(profile.name)
                sessionManager.saveUserEmail(profile.email)
                sessionManager.saveUserDetails(profile.age, profile.gender, profile.phone, profile.address)
                sessionManager.saveDoctorDetails(
                    profile.specialization,
                    profile.experienceYears,
                    profile.fee,
                    profile.languages,
                    profile.bio
                )
            }
        }

        viewModel.updateStatus.observe(this) { (success, error) ->
            if (success) {
                val name = etFullName.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val specialization = etSpecialization.text.toString().trim()
                
                sessionManager.saveUserName(name)
                sessionManager.saveUserEmail(email)
                sessionManager.saveUserDetails(sessionManager.getUserAge(), sessionManager.getUserGender(), phone, sessionManager.getUserAddress())
                sessionManager.saveDoctorDetails(
                    specialization,
                    sessionManager.getUserExperience(),
                    sessionManager.getUserFee(),
                    sessionManager.getUserLanguages(),
                    sessionManager.getUserBio()
                )

                Toast.makeText(this@EditDoctorProfileActivity, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@EditDoctorProfileActivity, error ?: "Update Failed", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            finish()
        }

        findViewById<FrameLayout>(R.id.flChangePhoto).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        findViewById<MaterialButton>(R.id.btnSaveChanges).setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun saveProfileChanges() {
        val name = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val specialization = etSpecialization.text.toString().trim()

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = sessionManager.getUserId()
        val updateRequest = UpdateProfileRequest(
            userId = userId,
            role = "doctor",
            name = name,
            phone = phone,
            specialization = specialization
            // The backend update_profile handles these fields for the "doctor" role.
        )
        viewModel.updateProfile(updateRequest)
    }
}
