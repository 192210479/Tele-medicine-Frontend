package com.simats.Tmapp

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.UpdateProfileRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var tilName: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPhone: TextInputLayout
    private lateinit var etPhone: TextInputEditText
    private lateinit var tilDOB: TextInputLayout
    private lateinit var etDOB: TextInputEditText
    private lateinit var tilGender: TextInputLayout
    private lateinit var actvGender: AutoCompleteTextView
    private lateinit var tilCity: TextInputLayout
    private lateinit var etCity: TextInputEditText
    private lateinit var tilState: TextInputLayout
    private lateinit var etState: TextInputEditText
    
    private lateinit var btnSave: MaterialButton
    private lateinit var ivAvatar: ImageView
    private lateinit var sessionManager: SessionManager
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let {
                    try {
                        contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) {
                    }

                    selectedImageUri = it
                    ivAvatar.setImageURI(it)

                    Toast.makeText(this, "Photo selected", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager.getInstance(this)
        initializeViews()
        setupAdapters()
        setupListeners()
        setupValidation()
        
        preFillFromSession()
        fetchProfile()
        observeViewModel()
        
        checkFormValidity()
    }

    private fun initializeViews() {
        tilName = findViewById(R.id.tilFullName)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        tilPhone = findViewById(R.id.tilPhone)
        etPhone = findViewById(R.id.etPhone)
        tilDOB = findViewById(R.id.tilDOB)
        etDOB = findViewById(R.id.etDOB)
        tilGender = findViewById(R.id.tilGender)
        actvGender = findViewById(R.id.actvGender)
        tilCity = findViewById(R.id.tilCity)
        etCity = findViewById(R.id.etCity)
        tilState = findViewById(R.id.tilState)
        etState = findViewById(R.id.etState)
        
        btnSave = findViewById(R.id.btnSaveChanges)
        ivAvatar = findViewById(R.id.ivAvatar)
    }

    private fun setupAdapters() {
        val genders = arrayOf("Male", "Female", "Other")
        actvGender.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders))
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        findViewById<View>(R.id.flAvatarEdit).setOnClickListener { openImagePicker() }
        etDOB.setOnClickListener { showDatePicker() }
        btnSave.setOnClickListener { updateProfile() }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year)
            etDOB.setText(formattedDate)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun setupValidation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateFields()
                checkFormValidity()
            }
        }

        val edits = listOf(etName, etPhone, etCity, etState, etDOB)
        edits.forEach { it.addTextChangedListener(watcher) }
        
        actvGender.setOnItemClickListener { _, _, _, _ -> validateFields(); checkFormValidity() }
    }

    private fun validateFields() {
        val name = etName.text.toString().trim()
        if (name.isNotEmpty()) {
            if (!name.matches(Regex("^[a-zA-Z\\s]+$"))) tilName.error = "Name should contain only alphabets and spaces"
            else if (name.length < 3) tilName.error = "Name should be at least 3 characters"
            else if (name == name.uppercase()) tilName.error = "Name should not be all-uppercase"
            else {
                val words = name.split(" ")
                if (words.any { it.isNotEmpty() && !it[0].isUpperCase() }) tilName.error = "First letter of each word must be capitalized"
                else tilName.error = null
            }
        } else tilName.error = null

        val phone = etPhone.text.toString().trim()
        if (phone.isNotEmpty() && !phone.matches(Regex("^[6-9][0-9]{9}$"))) tilPhone.error = "Mobile number must be 10 digits starting with 6-9"
        else tilPhone.error = null
    }

    private fun checkFormValidity() {
        val requiredFilled = !etName.text.isNullOrEmpty()
        val noErrors = tilName.error == null && tilPhone.error == null
        btnSave.isEnabled = requiredFilled && noErrors
    }

    private fun preFillFromSession() {
        etName.setText(sessionManager.getUserName())
        etEmail.setText(sessionManager.getUserEmail())
        etPhone.setText(sessionManager.getUserPhone())
        
        val address = sessionManager.getUserAddress() ?: ""
        if (address.contains(",")) {
            val parts = address.split(",")
            etCity.setText(parts.getOrNull(0)?.trim() ?: "")
            etState.setText(parts.getOrNull(1)?.trim() ?: "")
        } else {
            etCity.setText(address)
        }
        
        actvGender.setText(sessionManager.getUserGender(), false)
        
        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        val avatarVersion = sessionManager.getAvatarVersion()
        val role = sessionManager.getUserRole()?.lowercase() ?: "patient"
        val photoUrl = "$baseUrl/api/profile/image/${sessionManager.getUserId()}?role=$role&v=$avatarVersion"

        AvatarUtils.loadAvatar(
            ivAvatar,
            photoUrl,
            sessionManager.getUserName() ?: "User"
        )
    }

    private fun fetchProfile() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole()?.lowercase() ?: "patient"
        if (userId != -1) viewModel.fetchProfile(userId, role)
    }

    private fun observeViewModel() {
        viewModel.profile.observe(this) { profile ->
            if (profile != null) {
                etName.setText(profile.name)
                etEmail.setText(profile.email)
                etPhone.setText(profile.phone)
                
                val address = profile.address ?: ""
                if (address.contains(",")) {
                    val parts = address.split(",")
                    etCity.setText(parts.getOrNull(0)?.trim() ?: "")
                    etState.setText(parts.getOrNull(1)?.trim() ?: "")
                } else {
                    etCity.setText(address)
                }
                
                profile.gender?.let { actvGender.setText(it, false) }
                
                sessionManager.saveUserName(profile.name)
                sessionManager.saveUserDetails(profile.age, profile.gender, profile.phone, profile.address)
            }
        }

        viewModel.updateStatus.observe(this) { (success, error) ->
            if (success) {
                if (selectedImageUri != null) {
                    uploadProfileImage(selectedImageUri!!)
                } else {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()

                    sessionManager.saveUserName(etName.text.toString().trim())
                    sessionManager.saveUserDetails(
                        calculateAge(etDOB.text.toString()),
                        actvGender.text.toString(),
                        etPhone.text.toString().trim(),
                        "${etCity.text.toString().trim()}, ${etState.text.toString().trim()}"
                            .trim()
                            .removePrefix(",")
                            .removeSuffix(",")
                    )

                    btnSave.isEnabled = true
                    btnSave.text = "Save Changes"
                    setResult(RESULT_OK)
                    finish()
                }
            } else {
                btnSave.isEnabled = true
                btnSave.text = "Save Changes"
                Toast.makeText(this, error ?: "Update failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private fun updateProfile() {
        val dob = etDOB.text.toString()
        val age = if (dob.isNotEmpty()) calculateAge(dob) else sessionManager.getUserAge() ?: 25

        val request = UpdateProfileRequest(
            userId = sessionManager.getUserId(),
            role = sessionManager.getUserRole() ?: "patient",
            name = etName.text.toString().trim(),
            age = age,
            gender = actvGender.text.toString(),
            phone = etPhone.text.toString().trim(),
            address = "${etCity.text.toString().trim()}, ${etState.text.toString().trim()}"
                .trim()
                .removePrefix(",")
                .removeSuffix(",")
        )

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        viewModel.updateProfile(request)
    }

    private fun calculateAge(dob: String): Int {
        return try {
            val parts = dob.split("/")
            val year = parts[2].toInt()
            Calendar.getInstance().get(Calendar.YEAR) - year
        } catch (_: Exception) { 25 }
    }
    private fun uploadProfileImage(uri: Uri) {
        try {
            val file = uriToFile(uri)
            if (file == null || !file.exists()) {
                btnSave.isEnabled = true
                btnSave.text = "Save Changes"
                Toast.makeText(this, "Failed to prepare image", Toast.LENGTH_SHORT).show()
                return
            }

            val imagePart = MultipartBody.Part.createFormData(
                "image",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )

            val userIdBody = sessionManager.getUserId().toString()
                .toRequestBody("text/plain".toMediaTypeOrNull())

            val roleBody = (sessionManager.getUserRole() ?: "patient")
                .lowercase()
                .toRequestBody("text/plain".toMediaTypeOrNull())

            ApiClient.instance.uploadProfileImage(
                userIdBody,
                roleBody,
                imagePart
            ).enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    btnSave.isEnabled = true
                    btnSave.text = "Save Changes"

                    if (response.isSuccessful) {
                        sessionManager.bumpAvatarVersion()
                        com.bumptech.glide.Glide.get(this@EditProfileActivity).clearMemory()
                        Thread {
                            com.bumptech.glide.Glide.get(applicationContext).clearDiskCache()
                        }.start()

                        Toast.makeText(
                            this@EditProfileActivity,
                            "Profile updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        sessionManager.saveUserName(etName.text.toString().trim())
                        sessionManager.saveUserDetails(
                            calculateAge(etDOB.text.toString()),
                            actvGender.text.toString(),
                            etPhone.text.toString().trim(),
                            "${etCity.text.toString().trim()}, ${etState.text.toString().trim()}"
                                .trim()
                                .removePrefix(",")
                                .removeSuffix(",")
                        )

                        selectedImageUri = null
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(
                            this@EditProfileActivity,
                            "Profile text updated, but image upload failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    btnSave.isEnabled = true
                    btnSave.text = "Save Changes"

                    Toast.makeText(
                        this@EditProfileActivity,
                        "Profile text updated, but image upload failed: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        } catch (e: Exception) {
            btnSave.isEnabled = true
            btnSave.text = "Save Changes"
            Toast.makeText(this, "Image upload error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val fileName = getFileName(uri) ?: "profile_${System.currentTimeMillis()}.jpg"
            val tempFile = File(cacheDir, fileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
        return name
    }
}
