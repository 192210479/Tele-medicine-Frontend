package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.tmapp.api.DoctorRegisterRequest
import com.simats.tmapp.api.RegisterResponse
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateDoctorDetailsActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etSpec: EditText
    private lateinit var etLicense: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_doctor_details)

        // Initialize views
        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etSpec = findViewById(R.id.etSpec)
        etLicense = findViewById(R.id.etLicense)

        ivBack.setOnClickListener {
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            registerDoctor()
        }

        // Programmatic Password Show/Hide logic
        var isPasswordVisible = false
        val iconVisible = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_visibility)
        val iconHidden = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_visibility_off)
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
    }

    private fun registerDoctor() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val spec = etSpec.text.toString().trim()
        val license = etLicense.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
            if (email.isEmpty()) Toast.makeText(this, "Email required", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        val passwordPattern = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$")
        if (!passwordPattern.matches(password)) {
            Toast.makeText(this, "Password must contain uppercase, lowercase, number and special character", Toast.LENGTH_LONG).show()
            return
        }

        val request = DoctorRegisterRequest(
            name = name,
            email = email,
            password = password,
            specialization = spec,
            licenseNumber = license,
            experience_years = 0, // Fixed parameter name to match models.kt
            fee = 0.0,           
            languages = "",      
            bio = ""
        )

        ApiClient.instance.registerDoctor(request).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    val message = response.body()?.message ?: "Registration successful"
                    Toast.makeText(this@CreateDoctorDetailsActivity, message, Toast.LENGTH_LONG).show()
                    
                    val intent = Intent(this@CreateDoctorDetailsActivity, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    val error = response.errorBody()?.string() ?: "Registration failed"
                    Toast.makeText(this@CreateDoctorDetailsActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Toast.makeText(this@CreateDoctorDetailsActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
