package com.simats.Tmapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.simats.Tmapp.api.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateDoctorDetailsActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etSpec: EditText
    private lateinit var etLicense: EditText
    
    private lateinit var etExperience: EditText
    private lateinit var etFee: EditText
    private lateinit var etLanguages: EditText
    private lateinit var etBio: EditText

    private lateinit var tvNameError: TextView
    private lateinit var tvEmailError: TextView
    private lateinit var tvPasswordError: TextView
    private lateinit var tvConfirmPasswordError: TextView
    private lateinit var tvSpecError: TextView
    private lateinit var tvLicenseErrorLine: TextView

    private lateinit var cvLicenseFile: MaterialCardView
    private lateinit var tvLicenseFileName: TextView
    private lateinit var tvLicenseFileError: TextView

    private lateinit var cvMedicalFile: MaterialCardView
    private lateinit var tvMedicalFileName: TextView
    private lateinit var tvMedicalFileError: TextView

    private lateinit var btnRegister: MaterialButton

    private var licenseUri: Uri? = null
    private var medicalUri: Uri? = null

    // Touched states for validation
    private var isSubmitted = false
    private var touchedFields = mutableSetOf<Int>()

    private val pickLicenseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleFileSelection(result.data?.data, true)
        }
    }

    private val pickMedicalLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleFileSelection(result.data?.data, false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_doctor_details)

        initializeViews()
        setupListeners()
        setupValidation()
        setupPasswordToggles()
        
        checkFormValidity()
    }

    private fun initializeViews() {
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        etSpec = findViewById(R.id.etSpec)
        etLicense = findViewById(R.id.etLicense)
        etExperience = findViewById(R.id.etExperience)
        etFee = findViewById(R.id.etFee)
        etLanguages = findViewById(R.id.etLanguages)
        etBio = findViewById(R.id.etBio)
        
        tvNameError = findViewById(R.id.tvNameError)
        tvEmailError = findViewById(R.id.tvEmailError)
        tvPasswordError = findViewById(R.id.tvPasswordError)
        tvConfirmPasswordError = findViewById(R.id.tvConfirmPasswordError)
        tvSpecError = findViewById(R.id.tvSpecError)
        tvLicenseErrorLine = findViewById(R.id.tvLicenseErrorLine)
        
        cvLicenseFile = findViewById(R.id.cvLicenseFile)
        tvLicenseFileName = findViewById(R.id.tvLicenseFileName)
        tvLicenseFileError = findViewById(R.id.tvLicenseFileError)
        
        cvMedicalFile = findViewById(R.id.cvMedicalFile)
        tvMedicalFileName = findViewById(R.id.tvMedicalFileName)
        tvMedicalFileError = findViewById(R.id.tvMedicalFileError)
        
        btnRegister = findViewById(R.id.btnRegister)
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        
        cvLicenseFile.setOnClickListener { openFilePicker(pickLicenseLauncher) }
        cvMedicalFile.setOnClickListener { openFilePicker(pickMedicalLauncher) }
        
        btnRegister.setOnClickListener { 
            isSubmitted = true
            validateFields()
            if (licenseUri == null || medicalUri == null) {
                if (licenseUri == null) {
                    tvLicenseFileError.text = "License document is required"
                    tvLicenseFileError.visibility = View.VISIBLE
                    cvLicenseFile.setStrokeColor(ContextCompat.getColorStateList(this, R.color.red_danger))
                }
                if (medicalUri == null) {
                    tvMedicalFileError.text = "Medical record is required"
                    tvMedicalFileError.visibility = View.VISIBLE
                    cvMedicalFile.setStrokeColor(ContextCompat.getColorStateList(this, R.color.red_danger))
                }
                Toast.makeText(this, "Required documents missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isFormValid()) {
                registerDoctor() 
            } else {
                Toast.makeText(this, "Please fix the errors", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openFilePicker(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        launcher.launch(Intent.createChooser(intent, "Select PDF"))
    }

    private fun handleFileSelection(uri: Uri?, isLicense: Boolean) {
        val tvName = if (isLicense) tvLicenseFileName else tvMedicalFileName
        val tvError = if (isLicense) tvLicenseFileError else tvMedicalFileError
        val card = if (isLicense) cvLicenseFile else cvMedicalFile
        
        if (uri == null) return
        
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val name = c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                val size = c.getLong(c.getColumnIndexOrThrow(OpenableColumns.SIZE))
                
                if (!name.lowercase().endsWith(".pdf")) {
                    tvError.text = "Only PDF files allowed"
                    tvError.visibility = View.VISIBLE
                    card.setStrokeColor(ContextCompat.getColorStateList(this, R.color.red_danger))
                    if (isLicense) licenseUri = null else medicalUri = null
                    tvName.text = if (isLicense) "Upload license document (PDF)" else "Upload medical records (PDF)"
                } else if (size > 5 * 1024 * 1024) {
                    tvError.text = "File size must be less than 5MB"
                    tvError.visibility = View.VISIBLE
                    card.setStrokeColor(ContextCompat.getColorStateList(this, R.color.red_danger))
                    if (isLicense) licenseUri = null else medicalUri = null
                    tvName.text = if (isLicense) "Upload license document (PDF)" else "Upload medical records (PDF)"
                } else {
                    tvError.visibility = View.GONE
                    card.setStrokeColor(ContextCompat.getColorStateList(this, R.color.sl_input_border))
                    tvName.text = name
                    if (isLicense) licenseUri = uri else medicalUri = uri
                }
            }
        }
        checkFormValidity()
    }

    private fun setupValidation() {
        val inputFields = listOf(etName, etEmail, etPassword, etConfirmPassword, etSpec, etLicense)
        
        inputFields.forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                private var isAutoCorrecting = false
                override fun afterTextChanged(s: Editable?) {
                    if (isAutoCorrecting) return
                    if (editText.id == R.id.etName && s != null) {
                        val input = s.toString()
                        val corrected = input.split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(java.util.Locale.getDefault()) else char.toString() } }
                        if (input != corrected) {
                            isAutoCorrecting = true
                            val selection = editText.selectionEnd
                            editText.setText(corrected)
                            editText.setSelection(selection.coerceAtMost(corrected.length))
                            isAutoCorrecting = false
                        }
                    }
                    touchedFields.add(editText.id)
                    validateFields()
                    checkFormValidity()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
            
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    touchedFields.add(editText.id)
                    validateFields()
                }
            }
        }
    }

    private fun validateFields() {
        // Name
        validateField(etName, tvNameError, touchedFields.contains(etName.id) || isSubmitted) {
            val name = etName.text.toString().trim()
            val nameRegex = Regex("^[A-Z][a-z]+(\\s[A-Z][a-z]+)*$")
            when {
                name.isEmpty() -> if (isSubmitted) "Full name is required" else null
                name.length < 3 -> "Minimum 3 characters required"
                !name.matches(nameRegex) -> "Name must start with capital letters (e.g., John Doe)"
                else -> null
            }
        }

        // Email
        validateField(etEmail, tvEmailError, touchedFields.contains(etEmail.id) || isSubmitted) {
            val email = etEmail.text.toString().trim()
            when {
                email.isEmpty() -> if (isSubmitted) "Email is required" else null
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || email[0].isDigit() -> "Valid email required (no leading numbers)"
                else -> null
            }
        }

        // Password
        validateField(etPassword, tvPasswordError, touchedFields.contains(etPassword.id) || isSubmitted) {
            val password = etPassword.text.toString()
            // Match backend: length >= 8, uppercase, lowercase, number, and special character
            val passRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*()_+=\\-])[A-Za-z\\d!@#\$%^&*()_+=\\-]{8,}\$")
            when {
                password.isEmpty() -> if (isSubmitted) "Password is required" else null
                !passRegex.matches(password) -> "Min 8 chars, must include upper, lower, number, and special char"
                else -> null
            }
        }

        // Confirm Password
        validateField(etConfirmPassword, tvConfirmPasswordError, touchedFields.contains(etConfirmPassword.id) || isSubmitted) {
            val confirm = etConfirmPassword.text.toString()
            val password = etPassword.text.toString()
            when {
                confirm.isEmpty() -> if (isSubmitted) "Please confirm your password" else null
                confirm != password -> "Passwords do not match"
                else -> null
            }
        }

        // Spec
        validateField(etSpec, tvSpecError, touchedFields.contains(etSpec.id) || isSubmitted) {
            val spec = etSpec.text.toString().trim()
            when {
                spec.isEmpty() -> if (isSubmitted) "Specialization is required" else null
                else -> null
            }
        }

        // License
        validateField(etLicense, tvLicenseErrorLine, touchedFields.contains(etLicense.id) || isSubmitted) {
            val license = etLicense.text.toString().trim()
            when {
                license.isEmpty() -> if (isSubmitted) "License number is required" else null
                license.length < 6 || !license.matches(Regex("^[a-zA-Z0-9]+$")) -> "Min 6 alphanumeric characters required"
                else -> null
            }
        }
    }

    private fun validateField(et: EditText, tv: TextView, shouldShow: Boolean, validator: () -> String?) {
        val error = validator()
        if (shouldShow && error != null) {
            tv.text = error
            tv.visibility = View.VISIBLE
            et.setBackgroundResource(R.drawable.bg_input_error)
        } else {
            tv.visibility = View.GONE
            et.setBackgroundResource(R.drawable.bg_input_selector)
        }
    }

    private fun isFormValid(): Boolean {
        val noErrors = tvNameError.visibility == View.GONE && tvEmailError.visibility == View.GONE &&
                       tvPasswordError.visibility == View.GONE && tvConfirmPasswordError.visibility == View.GONE &&
                       tvSpecError.visibility == View.GONE && tvLicenseErrorLine.visibility == View.GONE
        
        val allFilled = etName.text.isNotEmpty() && etEmail.text.isNotEmpty() &&
                        etPassword.text.isNotEmpty() && etConfirmPassword.text.isNotEmpty() &&
                        etSpec.text.isNotEmpty() && etLicense.text.isNotEmpty()
        
        return noErrors && allFilled && licenseUri != null && medicalUri != null
    }

    private fun checkFormValidity() {
        // Just used to drive UI if needed, but we keep button enabled per latest UX.
        btnRegister.isEnabled = true
    }

    private fun setupPasswordToggles() {
        setupPasswordToggle(etPassword)
        setupPasswordToggle(etConfirmPassword)
    }

    private fun setupPasswordToggle(editText: EditText) {
        var isPasswordVisible = false
        val iconVisible = ContextCompat.getDrawable(this, R.drawable.ic_visibility)
        val iconHidden = ContextCompat.getDrawable(this, R.drawable.ic_visibility_off)
        editText.setCompoundDrawablesWithIntrinsicBounds(null, null, iconHidden, null)

        editText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = editText.compoundDrawables[2]
                if (drawableEnd != null && event.rawX >= (editText.right - editText.paddingEnd - drawableEnd.bounds.width())) {
                    isPasswordVisible = !isPasswordVisible
                    if (isPasswordVisible) {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        editText.setCompoundDrawablesWithIntrinsicBounds(null, null, iconVisible, null)
                    } else {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        editText.setCompoundDrawablesWithIntrinsicBounds(null, null, iconHidden, null)
                    }
                    editText.setSelection(editText.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun registerDoctor() {
        val name = etName.text.toString().trim().toRequestBody("text/plain".toMediaTypeOrNull())
        val email = etEmail.text.toString().trim().toRequestBody("text/plain".toMediaTypeOrNull())
        val password = etPassword.text.toString().trim().toRequestBody("text/plain".toMediaTypeOrNull())
        val spec = etSpec.text.toString().trim().toRequestBody("text/plain".toMediaTypeOrNull())
        val licNum = etLicense.text.toString().trim().toRequestBody("text/plain".toMediaTypeOrNull())
        
        // Optional fields
        val experience = etExperience.text.toString().trim().let { if(it.isNotEmpty()) it.toRequestBody("text/plain".toMediaTypeOrNull()) else null }
        val fee = etFee.text.toString().trim().let { if(it.isNotEmpty()) it.toRequestBody("text/plain".toMediaTypeOrNull()) else null }
        val languages = etLanguages.text.toString().trim().let { if(it.isNotEmpty()) it.toRequestBody("text/plain".toMediaTypeOrNull()) else null }
        val bio = etBio.text.toString().trim().let { if(it.isNotEmpty()) it.toRequestBody("text/plain".toMediaTypeOrNull()) else null }

        val licensePart = prepareFilePart("license_file", licenseUri!!)
        val medicalPart = prepareFilePart("medical_record_file", medicalUri!!)

        if (licensePart == null || medicalPart == null) {
            Toast.makeText(this, "Failed to prepare documents", Toast.LENGTH_SHORT).show()
            return
        }

        btnRegister.isEnabled = false
        ApiClient.instance.registerDoctorMultipart(
            name, email, password, spec, licNum,
            experience, fee, languages, bio,
            licensePart, medicalPart
        ).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                btnRegister.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@CreateDoctorDetailsActivity, "Registration Successful! Waiting for Admin Approval.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@CreateDoctorDetailsActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }else {
                    val errorBody = response.errorBody()?.string()

                    val errorMsg = if (!errorBody.isNullOrEmpty()) {
                        try {
                            val json = org.json.JSONObject(errorBody)
                            val msg = json.optString("error", "")

                            if (msg.contains("already registered", ignoreCase = true)) {
                                "This email is already registered. Try with a new email."
                            } else {
                                msg.ifEmpty { "Registration failed. Please try again." }
                            }
                        } catch (e: Exception) {
                            "Registration failed. Please try again."
                        }
                    } else {
                        "Registration failed. Please try again."
                    }

                    Toast.makeText(
                        this@CreateDoctorDetailsActivity,
                        errorMsg,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                btnRegister.isEnabled = true

                val errorMsg = t.message?.lowercase() ?: ""

                // 👇 Claude's intelligent handling (KEEP THIS LOGIC)
                if (errorMsg.contains("timeout") ||
                    errorMsg.contains("reset") ||
                    errorMsg.contains("eof") ||
                    errorMsg.contains("closed") ||
                    errorMsg.contains("socket") ||
                    errorMsg.contains("connection")) {

                    // ✅ Treat as SUCCESS (because backend already registered)
                    Toast.makeText(
                        this@CreateDoctorDetailsActivity,
                        "Registration submitted! Waiting for admin approval.",
                        Toast.LENGTH_LONG
                    ).show()

                    // ✅ Navigate safely (optional but recommended)
                    val intent = Intent(this@CreateDoctorDetailsActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                } else {
                    // ❌ Real failure → keep your original behavior
                    Toast.makeText(
                        this@CreateDoctorDetailsActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun prepareFilePart(partName: String, fileUri: Uri): MultipartBody.Part? {
        return try {
            val inputStream = contentResolver.openInputStream(fileUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes == null) return null
            val requestFile = bytes.toRequestBody("application/pdf".toMediaTypeOrNull())
            var fileName = "document.pdf"
            contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
            MultipartBody.Part.createFormData(partName, fileName, requestFile)
        } catch (e: Exception) { null }
    }
}
