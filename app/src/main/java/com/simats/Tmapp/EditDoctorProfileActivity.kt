package com.simats.Tmapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import androidx.activity.viewModels
import com.google.android.material.card.MaterialCardView
import com.simats.Tmapp.api.UpdateProfileRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.simats.Tmapp.api.ApiClient
import java.io.File
import okhttp3.RequestBody.Companion.asRequestBody

class EditDoctorProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var ivEditAvatar: ShapeableImageView
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etSpecialization: EditText
    
    private lateinit var cvUpdateLicense: MaterialCardView
    private lateinit var tvUpdateLicenseName: TextView
    private lateinit var cvUpdateMedical: MaterialCardView
    private lateinit var tvUpdateMedicalName: TextView
    
    private val viewModel: ProfileViewModel by viewModels()

    private var selectedLicenseUri: Uri? = null
    private var selectedMedicalUri: Uri? = null
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                ivEditAvatar.setImageURI(uri)
            }
        }
    }

    private val pickLicenseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            handleFileSelection(uri, true)
        }
    }

    private val pickMedicalLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            handleFileSelection(uri, false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_doctor_profile)

        sessionManager = SessionManager.getInstance(this)
        initializeViews()
        setupObservers()
        loadInitialData()
    }

    private fun initializeViews() {
        ivEditAvatar = findViewById(R.id.ivEditAvatar)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etSpecialization = findViewById(R.id.etSpecialization)
        
        cvUpdateLicense = findViewById(R.id.cvUpdateLicense)
        tvUpdateLicenseName = findViewById(R.id.tvUpdateLicenseName)
        cvUpdateMedical = findViewById(R.id.cvUpdateMedical)
        tvUpdateMedicalName = findViewById(R.id.tvUpdateMedicalName)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        findViewById<FrameLayout>(R.id.flChangePhoto).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        cvUpdateLicense.setOnClickListener { openFilePicker(pickLicenseLauncher) }
        cvUpdateMedical.setOnClickListener { openFilePicker(pickMedicalLauncher) }

        findViewById<MaterialButton>(R.id.btnSaveChanges).setOnClickListener {
            saveChanges()
        }
    }

    private fun loadInitialData() {
        etFullName.setText(sessionManager.getUserName())
        etEmail.setText(sessionManager.getUserEmail())
        etPhone.setText(sessionManager.getUserPhone())
        etSpecialization.setText(sessionManager.getUserSpecialization())

        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        val userId = sessionManager.getUserId()
        val avatarVersion = sessionManager.getAvatarVersion()
        val photoUrl = "$baseUrl/api/profile/image/$userId?role=doctor&v=$avatarVersion"

        AvatarUtils.loadAvatar(
            imageView = ivEditAvatar,
            imageUrl = photoUrl,
            name = sessionManager.getUserName().ifEmpty { "Doctor" }
        )

        viewModel.fetchProfile(sessionManager.getUserId(), "doctor")
    }

    private fun setupObservers() {
        viewModel.profile.observe(this) { profile ->
            if (profile != null) {
                etFullName.setText(profile.name)
                etEmail.setText(profile.email)
                etPhone.setText(profile.phone ?: "")
                etSpecialization.setText(profile.specialization ?: "")
                
                sessionManager.saveUserName(profile.name)
                sessionManager.saveUserEmail(profile.email)
                sessionManager.saveUserDetails(profile.age, profile.gender, profile.phone, profile.address)
                sessionManager.saveDoctorDetails(profile.specialization, profile.experienceYears, profile.fee, profile.languages, profile.bio)

                val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
                val avatarVersion = sessionManager.getAvatarVersion()
                val photoUrl = "$baseUrl/api/profile/image/${sessionManager.getUserId()}?role=doctor&v=$avatarVersion"

                AvatarUtils.loadAvatar(
                    imageView = ivEditAvatar,
                    imageUrl = photoUrl,
                    name = profile.name.ifEmpty { "Doctor" }
                )
            }
        }

        viewModel.updateStatus.observe(this) { (success, error) ->
            if (success) {
                if (selectedImageUri != null) {
                    uploadDoctorProfileImage(selectedImageUri!!)
                } else {
                    Toast.makeText(this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                    if (selectedLicenseUri == null && selectedMedicalUri == null) {
                        finish()
                    }
                }
            } else {
                Toast.makeText(this, error ?: "Update Failed", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.documentUpdateStatus.observe(this) { (success, error) ->
            if (success) {
                Toast.makeText(this, "Documents Updated. Status reset to Pending.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, error ?: "Document Update Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openFilePicker(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        launcher.launch(Intent.createChooser(intent, "Select PDF"))
    }

    private fun handleFileSelection(uri: Uri?, isLicense: Boolean) {
        if (uri == null) return
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val name = c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                if (isLicense) {
                    selectedLicenseUri = uri
                    tvUpdateLicenseName.text = name
                } else {
                    selectedMedicalUri = uri
                    tvUpdateMedicalName.text = name
                }
            }
        }
    }

    private fun saveChanges() {
        val name = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val specialization = etSpecialization.text.toString().trim()

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show()
            return
        }

        // Update basic profile
        val updateRequest = UpdateProfileRequest(
            userId = sessionManager.getUserId(),
            role = "doctor",
            name = name,
            phone = etPhone.text.toString().trim(),
            specialization = specialization
        )
        viewModel.updateProfile(updateRequest)

        // Update documents if selected
        if (selectedLicenseUri != null || selectedMedicalUri != null) {
            val userIdBody = sessionManager.getUserId().toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val licensePart = selectedLicenseUri?.let { prepareFilePart("license_file", it) }
            val medicalPart = selectedMedicalUri?.let { prepareFilePart("medical_record_file", it) }
            viewModel.updateDoctorDocuments(userIdBody, licensePart, medicalPart)
        }
    }

    private fun prepareFilePart(partName: String, fileUri: Uri): MultipartBody.Part? {
        return try {
            val inputStream = contentResolver.openInputStream(fileUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes == null) return null
            val requestFile = bytes.toRequestBody("application/pdf".toMediaTypeOrNull())
            var fileName = "document.pdf"
            contentResolver.query(fileUri, null, null, null, null)?.use { it.moveToFirst(); fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) }
            MultipartBody.Part.createFormData(partName, fileName, requestFile)
        } catch (e: Exception) { null }
    }

    private fun uploadDoctorProfileImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val tempFile = File.createTempFile("doctor_profile_", ".jpg", cacheDir)
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
            val userIdBody = sessionManager.getUserId().toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val roleBody = "doctor".toRequestBody("text/plain".toMediaTypeOrNull())

            ApiClient.instance.uploadProfileImage(userIdBody, roleBody, imagePart)
                .enqueue(object : retrofit2.Callback<Map<String, Any>> {
                    override fun onResponse(
                        call: retrofit2.Call<Map<String, Any>>,
                        response: retrofit2.Response<Map<String, Any>>
                    ) {
                        sessionManager.bumpAvatarVersion()
                        com.bumptech.glide.Glide.get(this@EditDoctorProfileActivity).clearMemory()
                        Thread {
                            com.bumptech.glide.Glide.get(applicationContext).clearDiskCache()
                        }.start()

                        selectedImageUri = null

                        Toast.makeText(
                            this@EditDoctorProfileActivity,
                            "Profile Updated Successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                    }

                    override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                        Toast.makeText(this@EditDoctorProfileActivity, "Image upload failed: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })

        } catch (e: Exception) {
            Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
        }
    }
}
