package com.simats.Tmapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.PatientRegisterRequest
import com.simats.Tmapp.api.RegisterResponse
import com.simats.Tmapp.api.ApiClient
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class CreateAccountDetailsActivity : AppCompatActivity() {

    private lateinit var tilName: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
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
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etConfirmPassword: TextInputEditText
    
    private lateinit var btnRegister: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account_details)

        initializeViews()
        setupAdapters()
        setupListeners()
        setupValidation()
        
        checkFormValidity()
    }

    private fun initializeViews() {
        tilName = findViewById(R.id.tilFullName)
        etName = findViewById(R.id.etName)
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPhone = findViewById(R.id.tilMobile)
        etPhone = findViewById(R.id.etPhone)
        tilDOB = findViewById(R.id.tilDOB)
        etDOB = findViewById(R.id.etDOB)
        tilGender = findViewById(R.id.tilGender)
        actvGender = findViewById(R.id.actvGender)
        tilCity = findViewById(R.id.tilCity)
        etCity = findViewById(R.id.etCity)
        tilState = findViewById(R.id.tilState)
        etState = findViewById(R.id.etState)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        
        btnRegister = findViewById(R.id.btnRegister)
    }

    private fun setupAdapters() {
        val genders = arrayOf("Male", "Female", "Other")
        actvGender.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders))
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        etDOB.setOnClickListener { showDatePicker() }
        btnRegister.setOnClickListener { registerPatient() }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val formattedDate = String.format("%02d/%02d/%04d", day, month + 1, year)
            etDOB.setText(formattedDate)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private var isAutoCorrecting = false

    private fun setupValidation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isAutoCorrecting) return
                if (etName.isFocused && s != null) {
                    val input = s.toString()
                    val corrected = input.split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(java.util.Locale.getDefault()) else char.toString() } }
                    if (input != corrected) {
                        isAutoCorrecting = true
                        val selection = etName.selectionEnd
                        etName.setText(corrected)
                        etName.setSelection(selection.coerceAtMost(corrected.length))
                        isAutoCorrecting = false
                    }
                }
                validateFields()
                checkFormValidity()
            }
        }

        val allEdits = listOf(etName, etEmail, etPhone, etCity, etState, etPassword, etConfirmPassword, etDOB)
        allEdits.forEach { it.addTextChangedListener(watcher) }
        
        actvGender.setOnItemClickListener { _, _, _, _ -> validateFields(); checkFormValidity() }
    }

    private fun validateFields() {
        // Full Name
        val name = etName.text.toString().trim()
        val nameRegex = Regex("^[A-Z][a-z]+(\\s[A-Z][a-z]+)*$")
        if (name.isNotEmpty()) {
            if (name.length < 3) tilName.error = "Minimum 3 characters required"
            else if (!name.matches(nameRegex)) tilName.error = "Name must start with capital letters (e.g., John Doe)"
            else tilName.error = null
        } else tilName.error = null

        // Email
        val email = etEmail.text.toString().trim()
        if (email.isNotEmpty()) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || (email.isNotEmpty() && email[0].isDigit())) {
                tilEmail.error = "Enter a valid email address (should not start with a number)"
            } else tilEmail.error = null
        } else tilEmail.error = null

        // Mobile
        val phone = etPhone.text.toString().trim()
        if (phone.isNotEmpty() && !phone.matches(Regex("^[6-9][0-9]{9}$"))) tilPhone.error = "Mobile number must be 10 digits starting with 6-9"
        else tilPhone.error = null

        // Password & Confirm
        val password = etPassword.text.toString()
        if (password.isNotEmpty()) {
            // Match backend: length >= 8, uppercase, lowercase, number, and special character
            val passRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*()_+=\\-])[A-Za-z\\d!@#\$%^&*()_+=\\-]{8,}\$")
            if (!passRegex.matches(password)) {
                tilPassword.error = "Min 8 chars, must include upper, lower, number, and special char"
            } else tilPassword.error = null
        } else tilPassword.error = null

        val confirm = etConfirmPassword.text.toString()
        if (confirm.isNotEmpty() && confirm != password) tilConfirmPassword.error = "Passwords do not match"
        else tilConfirmPassword.error = null
    }

    private fun checkFormValidity() {
        val requiredFilled = !etName.text.isNullOrEmpty() && !etEmail.text.isNullOrEmpty() &&
                             !etPhone.text.isNullOrEmpty() && !actvGender.text.isNullOrEmpty() &&
                             !etDOB.text.isNullOrEmpty() && !etCity.text.isNullOrEmpty() &&
                             !etState.text.isNullOrEmpty() && !etPassword.text.isNullOrEmpty() &&
                             !etConfirmPassword.text.isNullOrEmpty()

        val noErrors = tilName.error == null && tilEmail.error == null &&
                       tilPhone.error == null && tilPassword.error == null &&
                       tilConfirmPassword.error == null

        btnRegister.isEnabled = requiredFilled && noErrors
    }

    private fun registerPatient() {
        val dob = etDOB.text.toString()
        val age = calculateAge(dob)

        val request = PatientRegisterRequest(
            name = etName.text.toString().trim(),
            email = etEmail.text.toString().trim(),
            password = etPassword.text.toString().trim(),
            age = age,
            gender = actvGender.text.toString(),
            phone = etPhone.text.toString().trim(),
            address = "${etCity.text.toString().trim()}, ${etState.text.toString().trim()}"
        )

        ApiClient.instance.registerPatient(request).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@CreateAccountDetailsActivity, "Registration Successful", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@CreateAccountDetailsActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                } else {
                    val errorMsg = try {
                        val errorJson = response.errorBody()?.string()
                        org.json.JSONObject(errorJson).getString("error")
                    } catch (e: Exception) {
                        "Registration failed. Please try again."
                    }
                    Toast.makeText(this@CreateAccountDetailsActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Toast.makeText(this@CreateAccountDetailsActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateAge(dob: String): Int {
        return try {
            val parts = dob.split("/")
            val day = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val year = parts[2].toInt()
            val dobCal = Calendar.getInstance().apply { set(year, month, day) }
            val now = Calendar.getInstance()
            var age = now.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
            if (now.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) age--
            age
        } catch (e: Exception) { 25 } // Fallback
    }
}
