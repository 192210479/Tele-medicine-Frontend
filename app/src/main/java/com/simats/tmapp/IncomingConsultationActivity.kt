package com.simats.tmapp

import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.simats.tmapp.api.ConsultationStartRequest
import com.simats.tmapp.api.ConsultationStartResponse
import com.simats.tmapp.api.DoctorResponse
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class IncomingConsultationActivity : AppCompatActivity() {
    private var appointmentId: Int = -1
    private var doctorId: Int = -1
    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_consultation)

        appointmentId = intent.getIntExtra("appointment_id", -1)
        doctorId = intent.getIntExtra("doctor_id", -1)

        val tvDoctorName = findViewById<TextView>(R.id.tvDoctorName)
        val tvSpeciality = findViewById<TextView>(R.id.tvSpeciality)
        val ivDoctorAvatar = findViewById<ShapeableImageView>(R.id.ivDoctorAvatar)

        // Requirement #1: Pass and read doctor details
        val doctorName = intent.getStringExtra("doctor_name")
        val specialization = intent.getStringExtra("doctor_specialization")
        val doctorPhoto = intent.getStringExtra("doctor_photo")
        
        if (!doctorName.isNullOrEmpty() && doctorName != "null") {
            tvDoctorName.text = "Dr. $doctorName"
        } else {
            tvDoctorName.text = "Doctor"
        }
        
        if (!specialization.isNullOrEmpty() && specialization != "null") {
            tvSpeciality.text = specialization
        } else {
            tvSpeciality.text = "Specialist"
        }

        // Requirement #4: Dynamic Profile Photo Display
        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        val photoUrl = if (!doctorPhoto.isNullOrEmpty()) doctorPhoto else "$baseUrl/api/profile/image/$doctorId?role=doctor"
        
        com.bumptech.glide.Glide.with(this)
            .load(photoUrl)
            .placeholder(R.drawable.bg_circle_soft_blue)
            .circleCrop()
            .into(ivDoctorAvatar)

        if (doctorId != -1 && (doctorName.isNullOrEmpty() || doctorName == "null" || specialization.isNullOrEmpty())) {
            fetchDoctorDetails(doctorId, tvDoctorName, tvSpeciality, ivDoctorAvatar)
        }

        // Start Ringtone
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        findViewById<FloatingActionButton>(R.id.fabAccept).setOnClickListener {
            stopRingtone()
            acceptConsultation()
        }

        findViewById<FloatingActionButton>(R.id.fabDecline).setOnClickListener {
            stopRingtone()
            finish()
        }
    }

    private fun fetchDoctorDetails(id: Int, tvName: TextView, tvSpec: TextView, ivAvatar: ShapeableImageView) {
        ApiClient.instance.getDoctorDetails(id).enqueue(object : Callback<DoctorResponse> {
            override fun onResponse(call: Call<DoctorResponse>, response: Response<DoctorResponse>) {
                if (response.isSuccessful) {
                    val doctor = response.body()
                    doctor?.let {
                        tvName.text = "Dr. ${it.name}"
                        tvSpec.text = it.specialization
                        
                        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
                        com.bumptech.glide.Glide.with(this@IncomingConsultationActivity)
                            .load("$baseUrl/api/profile/image/${doctor.id}?role=doctor")
                            .placeholder(R.drawable.bg_circle_soft_blue)
                            .circleCrop()
                            .into(ivAvatar)
                    }
                }
            }
            override fun onFailure(call: Call<DoctorResponse>, t: Throwable) {}
        })
    }

    private fun acceptConsultation() {
        val videoRoom = intent.getStringExtra("VIDEO_ROOM")
        
        if (!videoRoom.isNullOrEmpty()) {
            // Already have room from socket, use it
            joinVideoCall(videoRoom)
        } else {
            // Missing room, must call start to get it
            fetchVideoRoomAndJoin()
        }
    }

    private fun fetchVideoRoomAndJoin() {
        val request = ConsultationStartRequest(
            appointmentId = appointmentId,
            doctorId = doctorId
        )

        ApiClient.instance.startConsultation(request).enqueue(object : Callback<ConsultationStartResponse> {
            override fun onResponse(call: Call<ConsultationStartResponse>, response: Response<ConsultationStartResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val startResponse = response.body()!!
                    joinVideoCall(startResponse.channel)
                } else {
                    Toast.makeText(this@IncomingConsultationActivity, "Failed to join consultation: ${response.code()}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onFailure(call: Call<ConsultationStartResponse>, t: Throwable) {
                Toast.makeText(this@IncomingConsultationActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun joinVideoCall(videoRoom: String?) {
        // Requirement #8 & #3: Video Room Join Validation
        if (videoRoom.isNullOrEmpty()) {
            Toast.makeText(this, "Consultation room not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        val intent = Intent(this, PatientVideoCallActivity::class.java)
        intent.putExtra("appointment_id", appointmentId)
        intent.putExtra("VIDEO_ROOM", videoRoom)
        intent.putExtra("doctor_id", doctorId)
        
        // Requirement #1 & #7: Pass consistent strings
        val docNameRaw = findViewById<TextView>(R.id.tvDoctorName).text.toString().replace("Dr. ", "")
        intent.putExtra("doctor_name", docNameRaw)
        intent.putExtra("doctor_specialization", findViewById<TextView>(R.id.tvSpeciality).text.toString())
        
        // Pass photo URL too for consistency
        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        intent.putExtra("doctor_photo", "$baseUrl/api/profile/image/$doctorId?role=doctor")
        
        startActivity(intent)
        finish()
    }

    private fun stopRingtone() {
        ringtone?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
    }
}
