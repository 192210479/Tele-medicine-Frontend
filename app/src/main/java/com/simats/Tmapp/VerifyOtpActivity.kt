package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
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

class VerifyOtpActivity : AppCompatActivity() {

    private lateinit var etOtp: Array<EditText>
    private lateinit var tvTimer: TextView
    private lateinit var tvResend: TextView
    private lateinit var btnVerify: MaterialButton
    private lateinit var progressBar: ProgressBar
    
    private var email: String = ""
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)

        email = intent.getStringExtra("email") ?: ""
        
        initializeViews()
        setupOtpInputs()
        setupListeners()
        startTimer()
    }

    private fun initializeViews() {
        etOtp = arrayOf(
            findViewById(R.id.etOtp1), findViewById(R.id.etOtp2),
            findViewById(R.id.etOtp3), findViewById(R.id.etOtp4),
            findViewById(R.id.etOtp5), findViewById(R.id.etOtp6)
        )
        tvTimer = findViewById(R.id.tvTimer)
        tvResend = findViewById(R.id.tvResend)
        btnVerify = findViewById(R.id.btnVerify)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupOtpInputs() {
        for (i in 0 until 6) {
            etOtp[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1) {
                        if (i < 5) etOtp[i + 1].requestFocus()
                    }
                    checkOtpComplete()
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            etOtp[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (etOtp[i].text.isEmpty() && i > 0) {
                        etOtp[i - 1].requestFocus()
                    }
                }
                false
            }
        }
    }

    private fun checkOtpComplete() {
        val otp = etOtp.joinToString("") { it.text.toString() }
        val complete = otp.length == 6
        btnVerify.isEnabled = complete
        btnVerify.alpha = if (complete) 1.0f else 0.5f
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        btnVerify.setOnClickListener {
            verifyOtp()
        }

        tvResend.setOnClickListener {
            resendOtp()
        }
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        tvResend.visibility = View.GONE
        tvTimer.visibility = View.VISIBLE
        
        countDownTimer = object : CountDownTimer(300000, 1000) { // 5 minutes
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                tvTimer.text = String.format("Resend OTP in %02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                tvTimer.visibility = View.GONE
                tvResend.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun verifyOtp() {
        val otp = etOtp.joinToString("") { it.text.toString() }
        
        btnVerify.isEnabled = false
        btnVerify.text = ""
        progressBar.visibility = View.VISIBLE

        val body = mapOf("email" to email, "otp" to otp)
        ApiClient.instance.verifyOtp(body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                progressBar.visibility = View.GONE
                btnVerify.isEnabled = true
                btnVerify.text = "Verify OTP"

                if (response.isSuccessful) {
                    val intent = Intent(this@VerifyOtpActivity, UpdatePasswordActivity::class.java)
                    intent.putExtra("email", email)
                    intent.putExtra("otp", otp)
                    startActivity(intent)
                } else {
                    val rawError = response.errorBody()?.string() ?: ""
                    val errorMsg = try {
                        org.json.JSONObject(rawError).optString("error", "Invalid or expired OTP")
                    } catch (e: Exception) {
                        "Invalid or expired OTP"
                    }

                    Toast.makeText(this@VerifyOtpActivity, errorMsg, Toast.LENGTH_LONG).show()
                    
                    if (errorMsg.contains("expired", ignoreCase = true)) {
                        androidx.appcompat.app.AlertDialog.Builder(this@VerifyOtpActivity)
                            .setTitle("OTP Expired")
                            .setMessage("Your OTP has expired. Please request a new one.")
                            .setPositiveButton("Resend OTP") { _, _ -> resendOtp() }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }

                    etOtp.forEach { 
                        it.setBackgroundResource(R.drawable.bg_input_error)
                        it.setText("")
                    }
                    etOtp[0].requestFocus()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                btnVerify.isEnabled = true
                btnVerify.text = "Verify OTP"
                Toast.makeText(this@VerifyOtpActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun resendOtp() {
        val body = mapOf("email" to email)
        ApiClient.instance.sendOtp(body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@VerifyOtpActivity, "New OTP sent to your email", Toast.LENGTH_SHORT).show()
                    startAtTop()
                    startTimer()
                } else {
                    val rawError = response.errorBody()?.string() ?: ""
                    val msg = try { org.json.JSONObject(rawError).optString("error", "Failed to resend OTP") }
                              catch(e: Exception) { "Failed to resend OTP" }
                    Toast.makeText(this@VerifyOtpActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@VerifyOtpActivity, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun startAtTop() {
        etOtp.forEach { it.setText(""); it.setBackgroundResource(R.drawable.bg_input_selector) }
        etOtp[0].requestFocus()
    }
}
