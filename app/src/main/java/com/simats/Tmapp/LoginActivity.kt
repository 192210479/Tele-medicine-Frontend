package com.simats.Tmapp

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.LoginRequest
import com.simats.Tmapp.api.LoginResponse
import com.simats.Tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private var selectedRole = "Patient"
    private lateinit var sessionManager: SessionManager
    private var isLoggingIn = false

    // Validation Flags
    private var isSubmitted = false
    private var isEmailTouched = false
    private var isPasswordTouched = false

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbTerms: CheckBox
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvEmailError: TextView
    private lateinit var tvPasswordError: TextView
    private lateinit var tvTermsError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        ThemeManager.applyTheme(SessionManager.getInstance(this))
        setContentView(R.layout.activity_login)
        sessionManager = SessionManager.getInstance(this)

        initializeViews()
        setupRoleSelection()
        setupValidation()
        setupPasswordToggle()
        setupAuthLinks()
        
        // Initial state: No errors, button enabled
        isSubmitted = false
        isEmailTouched = false
        isPasswordTouched = false
        updateValidationUI()
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        cbTerms = findViewById(R.id.cbTerms)
        btnLogin = findViewById(R.id.btnLogin)
        tvEmailError = findViewById(R.id.tvEmailError)
        tvPasswordError = findViewById(R.id.tvPasswordError)
        tvTermsError = findViewById(R.id.tvTermsError)
        
        // Apply selector for blue focus highlighting
        etEmail.setBackgroundResource(R.drawable.bg_input_selector)
        etPassword.setBackgroundResource(R.drawable.bg_input_selector)
    }

    private fun setupRoleSelection() {
        val cardPatient = findViewById<LinearLayout>(R.id.cardPatient)
        val cardDoctor = findViewById<LinearLayout>(R.id.cardDoctor)
        val cardAdmin = findViewById<LinearLayout>(R.id.cardAdmin)

        updateRoleSelectionUI()

        cardPatient.setOnClickListener { selectedRole = "Patient"; updateRoleSelectionUI() }
        cardDoctor.setOnClickListener { selectedRole = "Doctor"; updateRoleSelectionUI() }
        cardAdmin.setOnClickListener { selectedRole = "Admin"; updateRoleSelectionUI() }
    }

    private fun updateRoleSelectionUI() {
        val colorSelected = ContextCompat.getColor(this, R.color.primary_blue)
        val colorUnselected = ContextCompat.getColor(this, R.color.text_grey)
        
        val cards = listOf(findViewById<LinearLayout>(R.id.cardPatient), findViewById<LinearLayout>(R.id.cardDoctor), findViewById<LinearLayout>(R.id.cardAdmin))
        val tvs = listOf(findViewById<TextView>(R.id.tvPatient), findViewById<TextView>(R.id.tvDoctor), findViewById<TextView>(R.id.tvAdmin))
        val ivs = listOf(findViewById<ImageView>(R.id.ivPatient), findViewById<ImageView>(R.id.ivDoctor), findViewById<ImageView>(R.id.ivAdmin))
        val roles = listOf("Patient", "Doctor", "Admin")

        roles.forEachIndexed { i, role ->
            val isSelected = selectedRole == role
            cards[i].setBackgroundResource(if (isSelected) R.drawable.bg_card_selected else R.drawable.bg_card_unselected)
            tvs[i].setTextColor(if (isSelected) colorSelected else colorUnselected)
            tvs[i].setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            androidx.core.widget.ImageViewCompat.setImageTintList(ivs[i], ColorStateList.valueOf(if (isSelected) colorSelected else colorUnselected))
        }
    }

    private fun setupValidation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateValidationUI()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etEmail.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)
        cbTerms.setOnCheckedChangeListener { _, _ -> updateValidationUI() }

        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                isEmailTouched = true
                updateValidationUI()
            }
        }
        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                isPasswordTouched = true
                updateValidationUI()
            }
        }

        btnLogin.setOnClickListener {
            handleLogin()
        }
    }

    private fun updateValidationUI(): Boolean {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val termsAccepted = cbTerms.isChecked

        val isEmailValid = email.isNotEmpty()
        val isPasswordValid = password.isNotEmpty()
        val isFormValid = isEmailValid && isPasswordValid && termsAccepted

        // Email UI
        if (isEmailTouched || isSubmitted) {
            if (!isEmailValid) {
                tvEmailError.text = "Email address is required"
                tvEmailError.visibility = View.VISIBLE
                etEmail.setBackgroundResource(R.drawable.bg_input_error)
            } else {
                tvEmailError.visibility = View.GONE
                etEmail.setBackgroundResource(R.drawable.bg_input_selector)
            }
        } else {
            tvEmailError.visibility = View.GONE
            etEmail.setBackgroundResource(R.drawable.bg_input_selector)
        }

        // Password UI
        if (isPasswordTouched || isSubmitted) {
            if (!isPasswordValid) {
                tvPasswordError.text = "Password is required"
                tvPasswordError.visibility = View.VISIBLE
                etPassword.setBackgroundResource(R.drawable.bg_input_error)
            } else {
                tvPasswordError.visibility = View.GONE
                etPassword.setBackgroundResource(R.drawable.bg_input_selector)
            }
        } else {
            tvPasswordError.visibility = View.GONE
            etPassword.setBackgroundResource(R.drawable.bg_input_selector)
        }

        // Terms UI - Only show after submission attempt
        if (isSubmitted && !termsAccepted) {
            tvTermsError.text = "You must accept the terms and conditions"
            tvTermsError.visibility = View.VISIBLE
        } else {
            tvTermsError.visibility = View.GONE
        }

        // Button State
        if (!isLoggingIn) {
            btnLogin.isEnabled = true // Button always clickable per UX requirement to show errors on click
            btnLogin.alpha = 1.0f
        }

        return isFormValid
    }

    private fun handleLogin() {
        isSubmitted = true
        val valid = updateValidationUI()
        
        if (!valid) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (isLoggingIn) return
        isLoggingIn = true
        btnLogin.isEnabled = false
        btnLogin.alpha = 0.5f

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        val loginRequest = LoginRequest(
            email = email,
            password = password,
            role = selectedRole.lowercase(),
            deviceName = android.os.Build.MODEL,
            location = "Unknown"
        )

        ApiClient.instance.login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                isLoggingIn = false
                btnLogin.isEnabled = true
                btnLogin.alpha = 1.0f

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null && loginResponse.error == null) {
                        val actualRole = loginResponse.role
                        
                        // Strict Role Validation
                        if (actualRole != null && !actualRole.equals(selectedRole, ignoreCase = true)) {
                            showRoleMismatchError(actualRole)
                            return
                        }

                        val role = actualRole ?: selectedRole
                        sessionManager.saveUserRole(role)
                        sessionManager.saveUserEmail(email)
                        loginResponse.userId?.let { userId -> sessionManager.saveUserId(userId) }
                        
                        SocketManager.getInstance(this@LoginActivity).connect()
                        ThemeManager.applyTheme(sessionManager)

                        val intent = Intent(this@LoginActivity, sessionManager.getDashboardActivity())
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, loginResponse?.error ?: "Login failed", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Invalid credentials"
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                isLoggingIn = false
                btnLogin.isEnabled = true
                btnLogin.alpha = 1.0f
                Toast.makeText(this@LoginActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showRoleMismatchError(actualRole: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Login Failed")
            .setMessage("You are registered as a $actualRole. Please select the correct account type.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                // Highlight the correct role if possible, but mainly reset UI
                isLoggingIn = false
                btnLogin.isEnabled = true
                btnLogin.alpha = 1.0f
            }
            .setCancelable(false)
            .show()
    }

    private fun setupPasswordToggle() {
        var isPasswordVisible = false
        val iconVisible = ContextCompat.getDrawable(this, R.drawable.ic_visibility)
        val iconHidden = ContextCompat.getDrawable(this, R.drawable.ic_visibility_off)
        etPassword.setCompoundDrawablesWithIntrinsicBounds(null, null, iconHidden, null)

        etPassword.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val drawableEnd = etPassword.compoundDrawables[2]
                if (drawableEnd != null && event.rawX >= (etPassword.right - etPassword.paddingEnd - drawableEnd.bounds.width())) {
                    isPasswordVisible = !isPasswordVisible
                    if (isPasswordVisible) {
                        etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(null, null, iconVisible, null)
                    } else {
                        etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(null, null, iconHidden, null)
                    }
                    etPassword.setSelection(etPassword.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun setupAuthLinks() {
        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            startActivity(Intent(this, ResetPasswordActivity::class.java))
        }
        findViewById<TextView>(R.id.tvRegister).setOnClickListener {
            startActivity(Intent(this, CreateAccountRoleActivity::class.java))
        }
        findViewById<TextView>(R.id.tvTerms).setOnClickListener {
            startActivity(Intent(this, DataPrivacyActivity::class.java))
        }
        findViewById<TextView>(R.id.tvPrivacy).setOnClickListener {
            startActivity(Intent(this, DataPrivacyActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        isLoggingIn = false
        btnLogin.isEnabled = true 
        btnLogin.alpha = 1.0f
    }
}
