package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.GenericResponse
import com.simats.Tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var tvEmailError: TextView
    private lateinit var btnSend: MaterialButton
    private lateinit var progressBar: ProgressBar
    
    private var isSubmitted = false
    private var isTouched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.etEmail)
        tvEmailError = findViewById(R.id.tvEmailError)
        btnSend = findViewById(R.id.btnSend)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isTouched || isSubmitted) validateEmail()
                else if (android.util.Patterns.EMAIL_ADDRESS.matcher(s.toString()).matches()) {
                    tvEmailError.visibility = View.GONE
                    etEmail.setBackgroundResource(R.drawable.bg_input_selector)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                isTouched = true
                validateEmail()
            }
        }

        btnSend.setOnClickListener {
            isSubmitted = true
            if (validateEmail()) {
                sendOtp()
            }
        }
    }

    private fun validateEmail(): Boolean {
        val email = etEmail.text.toString().trim()
        val isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        
        if (!isValid) {
            tvEmailError.text = if (email.isEmpty()) "Email address is required" else "Enter a valid email address"
            tvEmailError.visibility = View.VISIBLE
            etEmail.setBackgroundResource(R.drawable.bg_input_error)
        } else {
            tvEmailError.visibility = View.GONE
            etEmail.setBackgroundResource(R.drawable.bg_input_selector)
        }
        return isValid
    }

    private fun sendOtp() {
        val email = etEmail.text.toString().trim()
        
        btnSend.isEnabled = false
        btnSend.text = ""
        progressBar.visibility = View.VISIBLE

        val body = mapOf("email" to email)
        ApiClient.instance.sendOtp(mapOf("email" to email))
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {

                    if (response.isSuccessful && response.body() != null) {

                        val msg = response.body()?.message ?: "OTP sent successfully"
                        Toast.makeText(this@ResetPasswordActivity, msg, Toast.LENGTH_SHORT).show()

                        // Navigate to OTP screen
                        val intent = Intent(this@ResetPasswordActivity, VerifyOtpActivity::class.java)
                        intent.putExtra("email", email)
                        startActivity(intent)

                    } else {
                        val errorMsg = try {
                            val error = response.errorBody()?.string()
                            org.json.JSONObject(error ?: "{}").optString("error", "Failed to send OTP")
                        } catch (e: Exception) {
                            "Failed to send OTP"
                        }

                        Toast.makeText(this@ResetPasswordActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@ResetPasswordActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
