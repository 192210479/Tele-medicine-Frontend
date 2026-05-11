package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.ApiClient

class PrescriptionReadyActivity : AppCompatActivity() {
    private var appointmentId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prescription_ready)

        appointmentId = intent.getIntExtra("appointment_id", -1)
        val doctorId = intent.getIntExtra("doctor_id", -1)
        val doctorName = intent.getStringExtra("doctor_name") ?: "Doctor"
        val doctorSpecialization = intent.getStringExtra("doctor_specialization") ?: "Specialist"
        val doctorHospital = intent.getStringExtra("doctor_hospital") ?: ""
        val doctorPhoto = intent.getStringExtra("doctor_photo") ?: ""

        val tvDoctorName = findViewById<android.widget.TextView>(com.simats.Tmapp.R.id.tvDoctorName)
        if (tvDoctorName != null) tvDoctorName.text = "Dr. $doctorName"

        val tvDoctorSpec = findViewById<android.widget.TextView>(com.simats.Tmapp.R.id.tvDoctorSpecialty)
        if (tvDoctorSpec != null) {
            tvDoctorSpec.text = if (doctorHospital.isNotEmpty()) "$doctorSpecialization • $doctorHospital" else doctorSpecialization
        }

        val ivDoctor = findViewById<android.widget.ImageView>(com.simats.Tmapp.R.id.ivDoctorImage)
        if (ivDoctor != null) {
            val baseUrl = ApiClient.BASE_URL.removeSuffix("/")

            val finalDoctorImage = when {
                doctorPhoto.isNotEmpty() && doctorPhoto.startsWith("http") -> doctorPhoto
                doctorPhoto.isNotEmpty() && doctorPhoto.startsWith("/") -> "$baseUrl$doctorPhoto"
                doctorId != -1 -> "$baseUrl/api/profile/image/$doctorId?role=doctor"
                else -> null
            }

            AvatarUtils.loadAvatar(
                imageView = ivDoctor,
                imageUrl = finalDoctorImage,
                name = doctorName
            )
        }

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        findViewById<MaterialButton>(R.id.btnView).setOnClickListener {
            val intent = Intent(this, ViewPrescriptionActivity::class.java)
            intent.putExtra("appointment_id", appointmentId)
            intent.putExtra("doctor_id", doctorId)
            intent.putExtra("doctor_name", doctorName)
            intent.putExtra("doctor_specialization", doctorSpecialization)
            intent.putExtra("doctor_hospital", doctorHospital)
            intent.putExtra("doctor_photo", doctorPhoto)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            startActivity(Intent(this, PatientDashboardActivity::class.java))
            finishAffinity()
        }
    }
}
