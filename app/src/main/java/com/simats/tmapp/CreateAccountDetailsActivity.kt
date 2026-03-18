package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.tmapp.api.PatientRegisterRequest
import com.simats.tmapp.api.RegisterResponse
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateAccountDetailsActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etAge: EditText
    private lateinit var etGender: TextView // Based on XML, it's a TextView mockup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account_details)

        // Initialize views
        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etAge = findViewById(R.id.etAge)
        etGender = findViewById(R.id.etGender)

        ivBack.setOnClickListener {
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            registerPatient()
        }
        
        etGender.setOnClickListener {
            if (etGender.text.toString() == "Male") {
                etGender.text = "Female"
            } else {
                etGender.text = "Male"
            }
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

    private fun registerPatient() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val ageStr = etAge.text.toString().trim()
        val gender = etGender.text.toString()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || ageStr.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
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

        val age = ageStr.toIntOrNull()

        val request = PatientRegisterRequest(
            name = name,
            email = email,
            password = password,
            age = age,
            gender = gender,
            phone = null, // Not in UI yet
            address = null // Not in UI yet
        )

        ApiClient.instance.registerPatient(request).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    val message = response.body()?.message ?: "Patient registered successfully"
                    Toast.makeText(this@CreateAccountDetailsActivity, message, Toast.LENGTH_LONG).show()
                    
                    val intent = Intent(this@CreateAccountDetailsActivity, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                    Toast.makeText(this@CreateAccountDetailsActivity, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Toast.makeText(this@CreateAccountDetailsActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
