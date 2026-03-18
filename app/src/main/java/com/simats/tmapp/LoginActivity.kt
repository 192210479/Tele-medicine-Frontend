package com.simats.tmapp

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.simats.tmapp.api.LoginRequest
import com.simats.tmapp.api.LoginResponse
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private var selectedRole = "Patient"
    private lateinit var sessionManager: SessionManager
    private var isLoggingIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initial theme application
        ThemeManager.applyTheme(SessionManager.getInstance(this))
        
        setContentView(R.layout.activity_login)
        
        sessionManager = SessionManager.getInstance(this)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val cbTerms = findViewById<CheckBox>(R.id.cbTerms)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        val tvEmailError = findViewById<TextView>(R.id.tvEmailError)
        val tvPasswordError = findViewById<TextView>(R.id.tvPasswordError)
        val tvTermsError = findViewById<TextView>(R.id.tvTermsError)

        val cardPatient = findViewById<LinearLayout>(R.id.cardPatient)
        val cardDoctor = findViewById<LinearLayout>(R.id.cardDoctor)
        val cardAdmin = findViewById<LinearLayout>(R.id.cardAdmin)

        val ivPatient = findViewById<android.widget.ImageView>(R.id.ivPatient)
        val tvPatient = findViewById<TextView>(R.id.tvPatient)
        val ivDoctor = findViewById<android.widget.ImageView>(R.id.ivDoctor)
        val tvDoctor = findViewById<TextView>(R.id.tvDoctor)
        val ivAdmin = findViewById<android.widget.ImageView>(R.id.ivAdmin)
        val tvAdmin = findViewById<TextView>(R.id.tvAdmin)

        val btnFingerprint = findViewById<View>(R.id.btnFingerprint)

        fun updateRoleSelectionUI() {
            val colorSelected = ContextCompat.getColor(this, R.color.primary_blue)
            val colorUnselected = ContextCompat.getColor(this, R.color.text_grey)
            
            // Patient
            cardPatient.setBackgroundResource(if (selectedRole == "Patient") R.drawable.bg_card_selected else R.drawable.bg_card_unselected)
            tvPatient.setTextColor(if (selectedRole == "Patient") colorSelected else colorUnselected)
            tvPatient.setTypeface(null, if (selectedRole == "Patient") android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            androidx.core.widget.ImageViewCompat.setImageTintList(ivPatient, android.content.res.ColorStateList.valueOf(if (selectedRole == "Patient") colorSelected else colorUnselected))

            // Doctor
            cardDoctor.setBackgroundResource(if (selectedRole == "Doctor") R.drawable.bg_card_selected else R.drawable.bg_card_unselected)
            tvDoctor.setTextColor(if (selectedRole == "Doctor") colorSelected else colorUnselected)
            tvDoctor.setTypeface(null, if (selectedRole == "Doctor") android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            androidx.core.widget.ImageViewCompat.setImageTintList(ivDoctor, android.content.res.ColorStateList.valueOf(if (selectedRole == "Doctor") colorSelected else colorUnselected))

            // Admin
            cardAdmin.setBackgroundResource(if (selectedRole == "Admin") R.drawable.bg_card_selected else R.drawable.bg_card_unselected)
            tvAdmin.setTextColor(if (selectedRole == "Admin") colorSelected else colorUnselected)
            tvAdmin.setTypeface(null, if (selectedRole == "Admin") android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            androidx.core.widget.ImageViewCompat.setImageTintList(ivAdmin, android.content.res.ColorStateList.valueOf(if (selectedRole == "Admin") colorSelected else colorUnselected))
        }

        fun validate(): Boolean {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val termsAccepted = cbTerms.isChecked

            var isValid = true

            if (email.isEmpty()) {
                tvEmailError.text = "Email address is required"
                tvEmailError.visibility = View.VISIBLE
                etEmail.setBackgroundResource(R.drawable.bg_input_error)
                isValid = false
            } else {
                tvEmailError.visibility = View.GONE
                etEmail.setBackgroundResource(R.drawable.bg_input)
            }

            if (password.isEmpty()) {
                tvPasswordError.text = "Password is required"
                tvPasswordError.visibility = View.VISIBLE
                etPassword.setBackgroundResource(R.drawable.bg_input_error)
                isValid = false
            } else {
                tvPasswordError.visibility = View.GONE
                etPassword.setBackgroundResource(R.drawable.bg_input)
            }

            if (!termsAccepted) {
                tvTermsError.text = "You must accept the terms and conditions"
                tvTermsError.visibility = View.VISIBLE
                isValid = false
            } else {
                tvTermsError.visibility = View.GONE
            }

            // Update Login Button State
            if (!isLoggingIn) {
                btnLogin.isEnabled = isValid
                if (isValid) {
                    btnLogin.alpha = 1.0f
                    btnLogin.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_blue))
                } else {
                    btnLogin.alpha = 0.5f
                    btnLogin.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_blue))
                }
            }

            return isValid
        }

        // Real-time validation
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { validate() }
            override fun afterTextChanged(s: Editable?) {}
        }
        etEmail.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)
        cbTerms.setOnCheckedChangeListener { _, _ -> validate() }

        // Password Show/Hide logic purely in Kotlin
        var isPasswordVisible = false
        val iconVisible = ContextCompat.getDrawable(this, R.drawable.ic_visibility)
        val iconHidden = ContextCompat.getDrawable(this, R.drawable.ic_visibility_off)
        etPassword.setCompoundDrawablesWithIntrinsicBounds(null, null, iconHidden, null)

        etPassword.setOnTouchListener { v, event ->
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

        // Initial validation to set button state
        validate()

        cardPatient.setOnClickListener {
            selectedRole = "Patient"
            updateRoleSelectionUI()
        }
        cardDoctor.setOnClickListener {
            selectedRole = "Doctor"
            updateRoleSelectionUI()
        }
        cardAdmin.setOnClickListener {
            selectedRole = "Admin"
            updateRoleSelectionUI()
        }

        btnLogin.setOnClickListener {
            if (isLoggingIn) return@setOnClickListener
            
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Email required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (validate()) {
                isLoggingIn = true
                btnLogin.isEnabled = false
                btnLogin.alpha = 0.5f

                val loginRequest = LoginRequest(
                    email = email,
                    password = password,
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
                                // Save the role returned by the server, or the selected role if none returned
                                val role = loginResponse.role ?: selectedRole
                                sessionManager.saveUserRole(role)
                                sessionManager.saveUserEmail(email)
                                loginResponse.userId?.let { userId ->
                                    sessionManager.saveUserId(userId)
                                }
                                
                                SocketManager.getInstance(this@LoginActivity).connect()

                                ThemeManager.applyTheme(sessionManager)

                                val intent = Intent(this@LoginActivity, sessionManager.getDashboardActivity())
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                val errorMsg = loginResponse?.error ?: "Login failed"
                                Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
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
        }

        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, CreateAccountRoleActivity::class.java))
        }
        btnFingerprint.setOnClickListener {
            // Biometric logic
            val biometricManager = androidx.biometric.BiometricManager.from(this)
            val authInt = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            
            when (biometricManager.canAuthenticate(authInt)) {
                androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> {
                    val executor = ContextCompat.getMainExecutor(this)
                    val biometricPrompt = androidx.biometric.BiometricPrompt(this, executor,
                        object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                            }
                            override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                val role = selectedRole
                                sessionManager.saveUserRole(role)
                                SocketManager.getInstance(this@LoginActivity).connect()
                                ThemeManager.applyTheme(sessionManager)
                                val intent = Intent(this@LoginActivity, sessionManager.getDashboardActivity())
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()
                                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                            }
                        })
                        
                    val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Biometric login for TMApp")
                        .setSubtitle("Log in using your biometric credential")
                        .setAllowedAuthenticators(authInt)
                        .build()
                        
                    biometricPrompt.authenticate(promptInfo)
                }
                else -> {
                    Toast.makeText(this, "Biometric authentication not available", Toast.LENGTH_SHORT).show()
                }
            }
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
        findViewById<MaterialButton>(R.id.btnLogin)?.let {
            it.isEnabled = true 
            it.alpha = 1.0f
        }
    }
}
