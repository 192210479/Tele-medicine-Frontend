package com.simats.tmapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import com.simats.tmapp.api.*

class ConsultationWaitingActivity : AppCompatActivity() {
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tvTimer: TextView
    private lateinit var btnWaiting: MaterialButton
    private var appointmentId: Int = -1
    private var doctorId: Int = -1
    private var doctorName: String? = null
    private var doctorSpecialization: String? = null
    private val apiService = ApiClient.instance
    private var isReadyNotified = false
    private var channelName: String? = null
    private var consultationId: Int? = null
    private var socket: Socket? = null

    private val timerRunnable = object : Runnable {
        override fun run() {
            seconds++
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            tvTimer.text = String.format("Waiting: %d:%02d", minutes, remainingSeconds)
            
            // Step 3 — SAFE POLLING LOOP (5 seconds)
            checkConsultationStatus()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation_waiting)

        appointmentId = intent.getIntExtra("appointment_id", -1)
        doctorId = intent.getIntExtra("doctor_id", -1)
        doctorName = intent.getStringExtra("doctor_name")
        doctorSpecialization = intent.getStringExtra("doctor_specialization")

        if (appointmentId == -1) {
            Toast.makeText(this, "Invalid appointment ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvTimer = findViewById(R.id.tvTimerHeader)
        btnWaiting = findViewById(R.id.btnWaiting)

        val tvDocName = findViewById<TextView>(R.id.tvDoctorName)
        val tvDocSpec = findViewById<TextView>(R.id.tvSpeciality)

        if (!doctorName.isNullOrEmpty()) {
            tvDocName.text = "Dr. $doctorName"
        } else if (doctorId != -1) {
            fetchDoctorDetails(doctorId, tvDocName, tvDocSpec)
        }

        if (!doctorSpecialization.isNullOrEmpty()) {
            tvDocSpec.text = doctorSpecialization
        }
        
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        findViewById<MaterialButton>(R.id.btnChat).setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("appointment_id", appointmentId)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btnUpload).setOnClickListener {
            val intent = Intent(this, MedicalRecordsActivity::class.java)
            intent.putExtra("appointment_id", appointmentId)
            intent.putExtra("doctor_id", doctorId)
            intent.putExtra("patient_id", SessionManager.getInstance(this).getUserId())
            startActivity(intent)
        }

        btnWaiting.setOnClickListener {
            joinConsultation()
        }

        handler.postDelayed(timerRunnable, 1000)
        connectSocket()
    }

    private fun connectSocket() {
        try {
            socket = SocketManager.getSocket(this)
            
            // Step 1 — SOCKET LISTENER FOR STARTED
            socket?.on("consultation_started") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as? JSONObject
                    if (data?.optInt("appointment_id") == appointmentId || data?.optString("appointment_id") == appointmentId.toString()) {
                        runOnUiThread {
                            // Force check status to get the channel and navigate
                            checkConsultationStatus()
                        }
                    }
                }
            }
            
            socket?.on("appointment_cancelled") { args ->
                runOnUiThread {
                    androidx.appcompat.app.AlertDialog.Builder(this@ConsultationWaitingActivity)
                        .setTitle("Appointment Cancelled")
                        .setMessage("This appointment has been cancelled.")
                        .setPositiveButton("OK") { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkConsultationStatus() {
        // Step 2 — SAFE API CALL
        apiService.getConsultationStatus(appointmentId).enqueue(object : Callback<ConsultationStatusResponse> {
            override fun onResponse(call: Call<ConsultationStatusResponse>, response: Response<ConsultationStatusResponse>) {
                
                if (!response.isSuccessful) return
                
                val body = response.body() ?: return
                
                if (body.canJoin == true && body.channel != null && body.consultationId != null) {
                    startVideoCall(body.channel, body.consultationId)
                    return
                }
                
                val status = body.status ?: return
                
                // Step 6 — ADD LOGGING
                Log.d("Consultation", "STATUS RESPONSE: $status")
                
                val tvStatus = findViewById<TextView>(R.id.tvStatus)
                when (status.lowercase()) {
                    "scheduled" -> {
                        tvStatus.text = "Waiting for Doctor"
                    }
                    "preparing" -> {
                        tvStatus.text = "Doctor is preparing"
                    }
                    "ready" -> {
                        tvStatus.text = "Consultation Ready... Please Wait"
                    }
                    "ongoing", "live" -> {
                        tvStatus.text = "Consultation is Live"
                    }
                }
            }

            override fun onFailure(call: Call<ConsultationStatusResponse>, t: Throwable) {
                // Step 6 — ADD LOGGING
                Log.e("Consultation", "Status API failed", t)
            }
        })
    }

    private fun startVideoCall(channel: String, consultationId: Int) {
        val intent = Intent(this, PatientVideoCallActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channel)
        intent.putExtra("CONSULTATION_ID", consultationId)
        
        // Pass extra data needed by PatientVideoCallActivity
        intent.putExtra("appointment_id", appointmentId)
        intent.putExtra("doctor_id", doctorId)
        intent.putExtra("doctor_name", doctorName)
        intent.putExtra("doctor_specialization", doctorSpecialization)
        startActivity(intent)
        finish()
    }


    private fun showNotification() {
        val channelId = "consultation_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Consultation", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, ConsultationWaitingActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_video_white)
            .setContentTitle("Consultation Ready")
            .setContentText("Dr has started your consultation. Tap to join.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun joinConsultation() {
        val channel = channelName
        val conId = consultationId
        if (channel == null || conId == null) {
            Toast.makeText(this, "Consultation not ready", Toast.LENGTH_SHORT).show()
            return
        }
        startVideoCall(channel, conId)
    }

    private fun fetchDoctorDetails(id: Int, tvName: TextView, tvSpec: TextView) {
        ApiClient.instance.getDoctorDetails(id).enqueue(object : Callback<DoctorResponse> {
            override fun onResponse(
                call: Call<DoctorResponse>,
                response: Response<DoctorResponse>
            ) {

                if (response.isSuccessful && response.body() != null) {

                    val doctor = response.body()!!

                    doctorName = doctor.name
                    doctorSpecialization = doctor.specialization

                    tvName.text = "Dr. ${doctor.name}"
                    tvSpec.text = doctor.specialization

                }

            }
            override fun onFailure(call: Call<DoctorResponse>, t: Throwable) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        socket?.off("consultation_started")
        socket?.off("appointment_cancelled")
    }
}
