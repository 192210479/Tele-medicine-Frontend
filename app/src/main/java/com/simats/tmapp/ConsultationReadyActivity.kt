package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.tmapp.api.ConsultationStartResponse
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ConsultationReadyActivity : AppCompatActivity() {
    private var appointmentId: Int = -1
    private var channelName: String? = null
    private var consultationId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation_ready)

        appointmentId = intent.getIntExtra("appointment_id", -1)
        if (appointmentId == -1) {
            Toast.makeText(this, "Invalid appointment ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val notificationId = intent.getIntExtra("notification_id", -1)
        if (notificationId != -1) {
            markNotificationAsRead(notificationId)
        }

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        findViewById<MaterialButton>(R.id.btnJoin).setOnClickListener {
            startConsultation()
        }

        findViewById<MaterialButton>(R.id.btnChat).setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("appointment_id", appointmentId)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btnUpload).setOnClickListener {
            val intent = Intent(this, MedicalRecordsActivity::class.java)
            intent.putExtra("appointment_id", appointmentId)
            intent.putExtra("patient_id", SessionManager.getInstance(this).getUserId())
            startActivity(intent)
        }

        fetchConsultationStatus()
    }

    private fun fetchConsultationStatus() {
        ApiClient.instance.getConsultationStatus(appointmentId).enqueue(object : Callback<com.simats.tmapp.api.ConsultationStatusResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.ConsultationStatusResponse>, response: Response<com.simats.tmapp.api.ConsultationStatusResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    channelName = response.body()!!.channel
                    consultationId = response.body()!!.consultationId
                }
            }
            override fun onFailure(call: Call<com.simats.tmapp.api.ConsultationStatusResponse>, t: Throwable) {}
        })
    }

    private fun startConsultation() {
        if (channelName == null) {
            Toast.makeText(this, "Consultation room not ready yet", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this@ConsultationReadyActivity, PatientVideoCallActivity::class.java)
        intent.putExtra("appointment_id", appointmentId)
        intent.putExtra("consultation_id", consultationId)
        intent.putExtra("channel_name", channelName)
        startActivity(intent)
        finish()
    }

    private fun markNotificationAsRead(notifId: Int) {
        ApiClient.instance.markNotificationRead(notifId).enqueue(object : Callback<com.simats.tmapp.api.ApiResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.ApiResponse>, response: Response<com.simats.tmapp.api.ApiResponse>) {
                // Success - badge will be updated on next poll or manual clear
            }
            override fun onFailure(call: Call<com.simats.tmapp.api.ApiResponse>, t: Throwable) {}
        })
    }
}
