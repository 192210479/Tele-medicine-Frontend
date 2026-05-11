package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class PrescriptionCreatingActivity : AppCompatActivity() {
    private var seconds = 3
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tvTimer: TextView
    private var appointmentId: Int = -1
    private var consultationId: Int = -1

    private val pollRunnable: Runnable = object : Runnable {
        override fun run() {
            if (appointmentId == -1) return
            
            com.simats.Tmapp.api.ApiClient.instance.getPrescriptionStatus(appointmentId).enqueue(object : retrofit2.Callback<com.simats.Tmapp.api.PrescriptionStatusResponse> {
                override fun onResponse(call: retrofit2.Call<com.simats.Tmapp.api.PrescriptionStatusResponse>, response: retrofit2.Response<com.simats.Tmapp.api.PrescriptionStatusResponse>) {
                    if (response.isSuccessful && response.body()?.status == "Ready") {
                        navigateToReady()
                    } else {
                        handler.postDelayed(pollRunnable, 3000)
                    }
                }
                override fun onFailure(call: retrofit2.Call<com.simats.Tmapp.api.PrescriptionStatusResponse>, t: Throwable) {
                    handler.postDelayed(pollRunnable, 5000)
                }
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prescription_creating)

        appointmentId = intent.getIntExtra("appointment_id", -1)
        consultationId = intent.getIntExtra("consultation_id", -1)
        tvTimer = findViewById(R.id.tvTimer)
        
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        findViewById<MaterialButton>(R.id.btnReturn).setOnClickListener {
            startActivity(Intent(this, PatientDashboardActivity::class.java))
            finishAffinity()
        }

        handler.post(pollRunnable)
        tvTimer.text = "Consultation ended. Creating prescription..."
    }

    private fun navigateToReady() {
        if (isFinishing) return
        val intent = Intent(this, PrescriptionActivity::class.java)
        intent.putExtra("appointment_id", appointmentId)
        intent.putExtra("consultation_id", consultationId)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
    }
}
