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

class UpdatePasswordActivity : AppCompatActivity() {

    private lateinit var etPass: EditText
    private lateinit var etConfirm: EditText
    private lateinit var tvPassError: TextView
    private lateinit var tvConfirmError: TextView
    private lateinit var btnUpdate: MaterialButton
    private lateinit var progressBar: ProgressBar
    
    private var email: String = ""
    private var otp: String = ""
    private var isSubmitted = false
    private var touchedFields = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_password)

        email = intent.getStringExtra("email") ?: ""
        otp = intent.getStringExtra("otp") ?: ""
        
        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        etPass = findViewById(R.id.etPassword)
        etConfirm = findViewById(R.id.etConfirmPassword)
        tvPassError = findViewById(R.id.tvPasswordError)
        tvConfirmError = findViewById(R.id.tvConfirmPasswordError)
        btnUpdate = findViewById(R.id.btnUpdate)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        setupPasswordToggle(etPass)
        setupPasswordToggle(etConfirm)

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateFields()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        etPass.addTextChangedListener(watcher)
        etConfirm.addTextChangedListener(watcher)

        etPass.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) { touchedFields.add(etPass.id); validateFields() } }
        etConfirm.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) { touchedFields.add(etConfirm.id); validateFields() } }

        btnUpdate.setOnClickListener {
            isSubmitted = true
            if (validateFields()) {
                updatePassword()
            }
        }
        
        // Initial state
        validateFields()
    }

    private fun setupPasswordToggle(editText: EditText) {
        var isVisible = false
        val iconVisible = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_visibility)
        val iconHidden = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_visibility_off)
        editText.setCompoundDrawablesWithIntrinsicBounds(null, null, iconHidden, null)

        editText.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val drawableEnd = editText.compoundDrawables[2]
                if (drawableEnd != null && event.rawX >= (editText.right - editText.paddingEnd - drawableEnd.bounds.width())) {
                    isVisible = !isVisible
                    val cursorPosition = editText.selectionStart
                    if (isVisible) {
                        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        editText.setCompoundDrawablesWithIntrinsicBounds(null, null, iconVisible, null)
                    } else {
                        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                        editText.setCompoundDrawablesWithIntrinsicBounds(null, null, iconHidden, null)
                    }
                    editText.setSelection(cursorPosition)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun validateFields(): Boolean {
        val pass = etPass.text.toString()
        val confirm = etConfirm.text.toString()
        
        val passRegex = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{8,}$")
        val isPassValid = passRegex.matches(pass)
        val isMatch = pass == confirm && confirm.isNotEmpty()

        // Pass UI
        if (isSubmitted || touchedFields.contains(etPass.id)) {
            if (!isPassValid) {
                tvPassError.text = if (pass.isEmpty()) "Password is required" else "Weak password (min 8 chars, A-Z, a-z, digit, special symbol)"
                tvPassError.visibility = View.VISIBLE
                etPass.setBackgroundResource(R.drawable.bg_input_error)
            } else {
                tvPassError.visibility = View.GONE
                etPass.setBackgroundResource(R.drawable.bg_input_selector)
            }
        }

        // Confirm UI
        if (isSubmitted || touchedFields.contains(etConfirm.id)) {
            if (!isMatch) {
                tvConfirmError.text = "Passwords do not match"
                tvConfirmError.visibility = View.VISIBLE
                etConfirm.setBackgroundResource(R.drawable.bg_input_error)
            } else {
                tvConfirmError.visibility = View.GONE
                etConfirm.setBackgroundResource(R.drawable.bg_input_selector)
            }
        }

        btnUpdate.isEnabled = isPassValid && isMatch
        btnUpdate.alpha = if (btnUpdate.isEnabled) 1.0f else 0.5f

        return isPassValid && isMatch
    }

    private fun updatePassword() {
        val newPassword = etPass.text.toString()
        
        btnUpdate.isEnabled = false
        btnUpdate.text = ""
        progressBar.visibility = View.VISIBLE

        val body = mapOf(
            "email" to email,
            "new_password" to newPassword
        )
        ApiClient.instance.resetPassword(body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                progressBar.visibility = View.GONE
                btnUpdate.isEnabled = true
                btnUpdate.text = "Update Password"

                if (response.isSuccessful) {
                    Toast.makeText(this@UpdatePasswordActivity, "Password updated successfully", Toast.LENGTH_LONG).show()
                    val intent = Intent(this@UpdatePasswordActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val rawError = response.errorBody()?.string() ?: ""
                    val errorMsg = try {
                        org.json.JSONObject(rawError).optString("error", "Failed to update password")
                    } catch (e: Exception) { "Failed to update password" }
                    Toast.makeText(this@UpdatePasswordActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                btnUpdate.isEnabled = true
                btnUpdate.text = "Update Password"
                Toast.makeText(this@UpdatePasswordActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
