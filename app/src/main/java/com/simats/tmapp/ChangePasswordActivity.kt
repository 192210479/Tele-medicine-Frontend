package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.viewModels
import com.simats.tmapp.api.ChangePasswordRequest

class ChangePasswordActivity : AppCompatActivity() {
    
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var sessionManager: SessionManager
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        sessionManager = SessionManager.getInstance(this)
        
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        setupPasswordToggle(etCurrentPassword)
        setupPasswordToggle(etNewPassword)
        setupPasswordToggle(etConfirmPassword)

        findViewById<MaterialButton>(R.id.btnUpdatePassword).setOnClickListener {
            performPasswordUpdate()
        }

        viewModel.passwordStatus.observe(this) { (success, message) ->
            Toast.makeText(this@ChangePasswordActivity, message, Toast.LENGTH_SHORT).show()
            if (success) {
                finish()
            }
        }

        val userRole = intent.getStringExtra("userRole") ?: sessionManager.getUserRole() ?: "patient"

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val dashboardIntent = when (userRole.lowercase()) {
                "admin" -> Intent(this, AdminDashboardActivity::class.java)
                "doctor" -> Intent(this, DoctorDashboardActivity::class.java)
                else -> Intent(this, PatientDashboardActivity::class.java)
            }
            dashboardIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(dashboardIntent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navBook)?.setOnClickListener {
            if (userRole.lowercase() == "admin") {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            } else if (userRole.lowercase() == "doctor") {
                startActivity(Intent(this, DoctorAppointmentsActivity::class.java))
            } else {
                startActivity(Intent(this, SelectDoctorActivity::class.java))
            }
        }

        findViewById<LinearLayout>(R.id.navHistory)?.setOnClickListener {
            if (userRole.lowercase() == "admin") {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            } else if (userRole.lowercase() != "doctor") {
                startActivity(Intent(this, ConsultationHistoryActivity::class.java))
            }
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            val profileIntent = when (userRole.lowercase()) {
                "admin" -> Intent(this, AdminProfileActivity::class.java)
                "doctor" -> Intent(this, DoctorProfileSettingsActivity::class.java)
                else -> Intent(this, ProfileActivity::class.java)
            }
            profileIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(profileIntent)
            finish()
        }
    }

    private fun performPasswordUpdate() {
        val current = etCurrentPassword.text.toString().trim()
        val new = etNewPassword.text.toString().trim()
        val confirm = etConfirmPassword.text.toString().trim()

        if (current.isEmpty() || new.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (new != confirm) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val passwordPattern = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$")
        if (!passwordPattern.matches(new)) {
            Toast.makeText(this, "Password must contain uppercase, lowercase, number and special character", Toast.LENGTH_LONG).show()
            return
        }
        
        val email = sessionManager.getUserEmail() ?: ""
        if (email.isEmpty()) {
            Toast.makeText(this, "User session not found. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        val body = ChangePasswordRequest(
            userId = sessionManager.getUserId(),
            role = sessionManager.getUserRole() ?: "patient",
            oldPassword = current,
            newPassword = new
        )

        viewModel.changePassword(body)
    }

    private fun setupPasswordToggle(editText: com.google.android.material.textfield.TextInputEditText) {
        var isPasswordVisible = false
        val iconVisible = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_visibility)
        val iconHidden = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_visibility_off)
        editText.setCompoundDrawablesWithIntrinsicBounds(null, null, iconHidden, null)

        editText.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val drawableEnd = editText.compoundDrawables[2]
                if (drawableEnd != null && event.rawX >= (editText.right - editText.paddingEnd - drawableEnd.bounds.width())) {
                    isPasswordVisible = !isPasswordVisible
                    if (isPasswordVisible) {
                        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        editText.setCompoundDrawablesWithIntrinsicBounds(null, null, iconVisible, null)
                    } else {
                        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                        editText.setCompoundDrawablesWithIntrinsicBounds(null, null, iconHidden, null)
                    }
                    editText.setSelection(editText.text?.length ?: 0)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }
}
