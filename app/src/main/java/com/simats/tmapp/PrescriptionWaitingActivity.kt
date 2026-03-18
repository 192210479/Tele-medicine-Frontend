package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.tmapp.api.ApiClient
import com.simats.tmapp.api.PrescriptionStatusResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PrescriptionWaitingActivity : AppCompatActivity() {

    private var appointmentId: Int = -1
    private var doctorId: Int = -1
    private var doctorName: String? = null
    private var doctorSpecialization: String? = null
    
    private lateinit var tvTimer: TextView
    private lateinit var tvDocName: TextView
    private lateinit var tvDocSpec: TextView
    private lateinit var btnReturn: MaterialButton
    
    private var secondsElapsed = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = true

    private val timerRunnable = object : Runnable {
        override fun run() {
            secondsElapsed++
            val minutes = secondsElapsed / 60
            val seconds = secondsElapsed % 60
            tvTimer.text = String.format("Waiting: %d:%02d", minutes, seconds)
            handler.postDelayed(this, 1000)
        }
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (isPolling) {
                checkPrescriptionStatus()
                handler.postDelayed(this, 3000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prescription_waiting)

        // Get data from intent
        appointmentId = intent.getIntExtra("appointment_id", -1)
        doctorId = intent.getIntExtra("doctor_id", -1)
        doctorName = intent.getStringExtra("doctor_name")
        doctorSpecialization = intent.getStringExtra("doctor_specialization")
        val doctorHospital = intent.getStringExtra("doctor_hospital") ?: ""
        val doctorPhoto = intent.getStringExtra("doctor_photo") ?: ""

        if (appointmentId == -1) {
            Toast.makeText(this, "Error: Missing appointment data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvTimer = findViewById(R.id.tvPrescriptionTimer)
        tvDocName = findViewById(R.id.tvDoctorName)
        tvDocSpec = findViewById(R.id.tvDoctorSpecialization)
        btnReturn = findViewById(R.id.btnReturnDashboard)

        tvDocName.text = if (!doctorName.isNullOrEmpty()) "Dr. $doctorName" else "Doctor"
        tvDocSpec.text = if (doctorHospital.isNotEmpty()) "$doctorSpecialization • $doctorHospital" else (doctorSpecialization ?: "Medical Specialist")

        val ivDoctor = findViewById<android.widget.ImageView>(R.id.ivDoctorImage)
        if (ivDoctor != null) {
            if (doctorPhoto.isNotEmpty()) {
                com.bumptech.glide.Glide.with(this).load(doctorPhoto).into(ivDoctor)
            } else {
                ivDoctor.setImageResource(R.drawable.img_doctor_sarah)
            }
        }

        btnReturn.setOnClickListener {
            val intent = Intent(this, PatientDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Start timer
        handler.post(timerRunnable)
        
        // Start polling
        handler.post(pollRunnable)
    }

    private fun checkPrescriptionStatus() {
        ApiClient.instance.getPrescriptionStatus(appointmentId).enqueue(object : Callback<PrescriptionStatusResponse> {
            override fun onResponse(call: Call<PrescriptionStatusResponse>, response: Response<PrescriptionStatusResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()?.status
                    if (status != null && status.equals("Ready", ignoreCase = true)) {
                        isPolling = false
                        navigateToReadyScreen()
                    }
                }
            }

            override fun onFailure(call: Call<PrescriptionStatusResponse>, t: Throwable) {
                // Polling continues on failure
            }
        })
    }

    private fun navigateToReadyScreen() {
        val intent = Intent(this, PrescriptionReadyActivity::class.java).apply {
            putExtra("appointment_id", appointmentId)
            putExtra("doctor_id", doctorId)
            putExtra("doctor_name", doctorName)
            putExtra("doctor_specialization", doctorSpecialization)
            putExtra("doctor_hospital", this@PrescriptionWaitingActivity.intent.getStringExtra("doctor_hospital"))
            putExtra("doctor_photo", this@PrescriptionWaitingActivity.intent.getStringExtra("doctor_photo"))
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
        handler.removeCallbacksAndMessages(null)
    }
}
