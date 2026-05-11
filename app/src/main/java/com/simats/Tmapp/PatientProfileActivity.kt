package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.simats.Tmapp.api.ProfileResponse
import com.simats.Tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PatientProfileActivity : AppCompatActivity() {
    private var appointmentId: Int = -1
    private var patientId: Int = -1
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_profile)

        sessionManager = SessionManager.getInstance(this)
        appointmentId = intent.getIntExtra("appointment_id", -1)
        patientId = intent.getIntExtra("patient_id", -1)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        findViewById<MaterialButton>(R.id.btnStartConsultation).setOnClickListener {
            val intent = Intent(this, ConsultationWaitingActivity::class.java)
            intent.putExtra("appointment_id", appointmentId)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
        }

        setupBottomNav()

        if (patientId != -1) {
            fetchPatientDetails(patientId)
        } else if (appointmentId != -1) {
            fetchAppointmentDetails(appointmentId)
        }
    }

    private fun fetchPatientDetails(id: Int) {
        ApiClient.instance.getProfile(id, "patient").enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    updateUI(response.body()!!)
                }
            }
            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                Toast.makeText(this@PatientProfileActivity, "Failed to load patient profile", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchAppointmentDetails(id: Int) {
        // First get appointment to find patientId
        ApiClient.instance.getAppointmentDetails(id).enqueue(object : Callback<com.simats.Tmapp.api.AppointmentResponse> {
            override fun onResponse(call: Call<com.simats.Tmapp.api.AppointmentResponse>, response: Response<com.simats.Tmapp.api.AppointmentResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val pId = response.body()!!.patientId
                    if (pId != null) {
                        fetchPatientDetails(pId)
                    }
                }
            }
            override fun onFailure(call: Call<com.simats.Tmapp.api.AppointmentResponse>, t: Throwable) {}
        })
    }

    private fun updateUI(profile: ProfileResponse) {
        findViewById<TextView>(R.id.tvPatientName).text = profile.name
        findViewById<TextView>(R.id.tvPatientId).text = "Patient ID: #${profile.id}"
        findViewById<TextView>(R.id.tvPatientAge).text = "${profile.age ?: "--"} Years"
        
        val avatar = findViewById<ShapeableImageView>(R.id.ivAvatar) ?: findViewById<ShapeableImageView>(R.id.ivEditAvatar)
        if (avatar != null) {
            val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
            val avatarVersion = sessionManager.getAvatarVersion()

            val finalPatientImage = if (profile.id != null) {
                "$baseUrl/api/profile/image/${profile.id}?role=patient&v=$avatarVersion"
            } else {
                null
            }

            AvatarUtils.loadAvatar(
                imageView = avatar,
                imageUrl = finalPatientImage,
                name = profile.name ?: "Patient"
            )
        }
    }

    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, DoctorDashboardActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
            finish()
        }
        findViewById<LinearLayout>(R.id.navAppts)?.setOnClickListener {
            startActivity(Intent(this, DoctorAppointmentsActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
            finish()
        }
        findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, DoctorProfileSettingsActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
            finish()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
