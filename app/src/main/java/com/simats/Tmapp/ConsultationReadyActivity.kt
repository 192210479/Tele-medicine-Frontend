package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.simats.Tmapp.api.*

class ConsultationReadyActivity : AppCompatActivity() {

    // ── state ─────────────────────────────────────────────────────────────────
    private var appointmentId: Int = -1
    private var doctorId: Int = -1
    private var doctorName: String? = null
    private var doctorSpecialization: String? = null
    private var doctorPhoto: String? = null
    private var channelName: String? = null
    private var consultationId: Int? = null
    private var isJoinEnabled = false

    // ── UI ────────────────────────────────────────────────────────────────────
    private lateinit var btnJoin: MaterialButton

    // ── polling ───────────────────────────────────────────────────────────────
    // FIX: Declare at class level — was local in setupSocketListeners() causing
    //      UninitializedPropertyAccessException crash in onDestroy
    private val pollHandler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            fetchConsultationStatus()
            pollHandler.postDelayed(this, 4000)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation_ready)

        appointmentId        = intent.getIntExtra("appointment_id", -1)
        doctorId             = intent.getIntExtra("doctor_id", -1)
        doctorName           = intent.getStringExtra("doctor_name")
        doctorSpecialization = intent.getStringExtra("doctor_specialization")
        doctorPhoto          = intent.getStringExtra("doctor_photo")
        channelName          = intent.getStringExtra("channel_name")
        consultationId       = intent.getIntExtra("consultation_id", -1).takeIf { it != -1 }

        if (appointmentId == -1) {
            Toast.makeText(this, "Invalid appointment ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ── views ─────────────────────────────────────────────────────────────
        btnJoin = findViewById(R.id.btnJoin)
        disableJoinButton()

        val tvDoctorName   = findViewById<TextView?>(R.id.tvDoctorName)
        val tvSpeciality   = findViewById<TextView?>(R.id.tvSpeciality)
        val ivDoctorImage  = findViewById<ImageView?>(R.id.ivDoctorImage)

        if (tvDoctorName != null && !doctorName.isNullOrEmpty()) {
            tvDoctorName.text = "Dr. $doctorName"
        }
        if (tvSpeciality != null && !doctorSpecialization.isNullOrEmpty()) {
            tvSpeciality.text = doctorSpecialization
        }
        if (ivDoctorImage != null) {
            val baseUrl  = ApiClient.BASE_URL.removeSuffix("/")
            val imageUrl = when {
                !doctorPhoto.isNullOrEmpty() && doctorPhoto!!.startsWith("http") -> doctorPhoto
                !doctorPhoto.isNullOrEmpty() && doctorPhoto!!.startsWith("/")    -> "$baseUrl$doctorPhoto"
                doctorId != -1 -> "$baseUrl/api/profile/image/$doctorId?role=doctor"
                else -> null
            }
            AvatarUtils.loadAvatar(ivDoctorImage, imageUrl, doctorName ?: "Doctor")
        }

        // ── back ──────────────────────────────────────────────────────────────
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // ── join button ───────────────────────────────────────────────────────
        btnJoin.setOnClickListener {
            if (isJoinEnabled) startConsultation()
            else Toast.makeText(this, "Please wait for the doctor to start the call", Toast.LENGTH_SHORT).show()
        }

        // ── chat / details ────────────────────────────────────────────────────
        findViewById<MaterialButton?>(R.id.btnChat)?.setOnClickListener {
            startActivity(
                Intent(this, AppointmentDetailsActivity::class.java).apply {
                    putExtra("appointment_id", appointmentId)
                    putExtra("doctor_name", doctorName)
                    putExtra("doctor_specialization", doctorSpecialization)
                }
            )
        }

        // ── handle notification tap (mark as read) ────────────────────────────
        val notificationId = intent.getIntExtra("notification_id", -1)
        if (notificationId != -1) markNotificationAsRead(notificationId)

        // ── If channel already passed from waiting screen, enable join now ────
        if (!channelName.isNullOrEmpty() && consultationId != null) {
            enableJoinButton()
        }

        // ── start polling + socket ────────────────────────────────────────────
        pollHandler.postDelayed(pollRunnable, 1000)
        setupSocketListeners()
    }

    // ── socket ────────────────────────────────────────────────────────────────
    private fun setupSocketListeners() {
        // FIX: Use SocketManager only — single socket instance across the app.
        //      SocketService.socket is a fallback if SocketManager has no socket yet.
        val socket = SocketManager.getSocket(this) ?: SocketService.socket ?: return

        val patientId = SessionManager.getInstance(this).getUserId()
        socket.emit("join", org.json.JSONObject().apply {
            put("user_id", patientId)
            put("role", "patient")
        })

        val handleReady: (Array<Any>) -> Unit = { args ->
            val data  = if (args.isNotEmpty()) args[0] as? org.json.JSONObject else null
            val apptId = data?.optInt("appointment_id", -1) ?: -1

            if (apptId == -1 || apptId == appointmentId) {
                // Update channel/consultationId if fresher data arrives
                data?.optString("channel")?.takeIf { it.isNotEmpty() }?.let { channelName = it }
                val ci = data?.optInt("consultation_id", -1) ?: -1
                if (ci != -1) consultationId = ci

                runOnUiThread { enableJoinButton() }
            }
        }

        socket.on("consultation_started") { handleReady(it) }
        socket.on("doctor_ready")         { handleReady(it) }
        socket.on("consultation_ready")   { handleReady(it) }

        socket.on("consultation_ended") { args ->
            val data  = if (args.isNotEmpty()) args[0] as? org.json.JSONObject else null
            val apptId = data?.optInt("appointment_id", -1) ?: -1
            if (apptId == -1 || apptId == appointmentId) {
                runOnUiThread { finish() }
            }
        }
    }

    // ── polling ───────────────────────────────────────────────────────────────
    private fun fetchConsultationStatus() {
        if (isJoinEnabled) {
            // Already enabled — no need to keep polling, but keep running in case
            // we need channel refresh
        }

        ApiClient.instance.getConsultationStatus(appointmentId)
            .enqueue(object : Callback<ConsultationStatusResponse> {
                override fun onResponse(
                    call: Call<ConsultationStatusResponse>,
                    response: Response<ConsultationStatusResponse>
                ) {
                    if (!response.isSuccessful || response.body() == null) return
                    val body = response.body()!!

                    // Always update channel/id from latest server response
                    if (!body.channel.isNullOrEmpty()) channelName    = body.channel
                    if (body.consultationId != null)   consultationId = body.consultationId

                    val status = body.status?.trim()?.lowercase() ?: ""
                    Log.d("ConsultationReady", "Poll status=$status canJoin=${body.canJoin}")

                    // FIX: "ready", "started", "ongoing" all valid states to join
                    if (status == "ready" || status == "started" || status == "ongoing") {
                        runOnUiThread { enableJoinButton() }
                    }
                }
                override fun onFailure(call: Call<ConsultationStatusResponse>, t: Throwable) {
                    Log.e("ConsultationReady", "Status poll failed: ${t.message}")
                }
            })
    }

    // ── join button state ─────────────────────────────────────────────────────
    private fun enableJoinButton() {
        if (isJoinEnabled) return          // already enabled — no-op
        isJoinEnabled = true
        btnJoin.isEnabled = true
        btnJoin.alpha     = 1.0f
        btnJoin.text      = "Join Video Call Now"
    }

    private fun disableJoinButton() {
        isJoinEnabled     = false
        btnJoin.isEnabled = false
        btnJoin.alpha     = 0.5f
        btnJoin.text      = "Waiting for Doctor to Start..."
    }

    // ── start video call ──────────────────────────────────────────────────────
    private fun startConsultation() {
        if (channelName.isNullOrEmpty()) {
            Toast.makeText(this, "Consultation room not ready yet", Toast.LENGTH_SHORT).show()
            return
        }
        if (consultationId == null) {
            Toast.makeText(this, "Consultation ID missing. Please wait.", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, PatientVideoCallActivity::class.java).apply {
                putExtra("appointment_id",      appointmentId)
                putExtra("consultation_id",     consultationId!!)
                putExtra("channel_name",        channelName)
                putExtra("doctor_id",           doctorId)
                putExtra("doctor_name",         doctorName)
                putExtra("doctor_specialization", doctorSpecialization)
                putExtra("doctor_photo",        doctorPhoto)
                // Pass both key variants so PatientVideoCallActivity always finds the value
                putExtra("CHANNEL_NAME",        channelName)
                putExtra("CONSULTATION_ID",     consultationId!!)
            }
        )
        finish()
    }

    // ── notification read ─────────────────────────────────────────────────────
    private fun markNotificationAsRead(notifId: Int) {
        ApiClient.instance.markNotificationRead(notifId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {}
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
            })
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────
    override fun onResume() {
        super.onResume()
        // Re-arm poll in case it was stopped by onPause
        pollHandler.removeCallbacks(pollRunnable)
        pollHandler.postDelayed(pollRunnable, 1000)
    }

    override fun onPause() {
        super.onPause()
        pollHandler.removeCallbacks(pollRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        // FIX: pollHandler is a class-level field — safe to call removeCallbacks here
        pollHandler.removeCallbacks(pollRunnable)

        // FIX: Only remove our own listeners — do NOT call socket.disconnect()
        //      The socket is shared; disconnecting here kills it for the whole app
        val socket = SocketManager.getSocket(this) ?: SocketService.socket
        socket?.off("consultation_started")
        socket?.off("doctor_ready")
        socket?.off("consultation_ready")
        socket?.off("consultation_ended")
    }
}