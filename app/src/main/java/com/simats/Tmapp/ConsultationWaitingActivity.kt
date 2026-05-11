package com.simats.Tmapp

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
import org.json.JSONObject
import com.simats.Tmapp.api.*

class ConsultationWaitingActivity : AppCompatActivity() {

    // ── state ─────────────────────────────────────────────────────────────────
    private var appointmentId: Int = -1
    private var doctorId: Int = -1
    private var doctorName: String? = null
    private var doctorSpecialization: String? = null
    private var doctorPhoto: String? = null

    private var channelName: String? = null
    private var consultationId: Int? = null
    private var isReadyNotified = false

    private val apiService = ApiClient.instance

    // ── UI ────────────────────────────────────────────────────────────────────
    private lateinit var tvTimer: TextView
    private lateinit var btnWaiting: MaterialButton

    // ── timer (display only — does NOT drive polling) ─────────────────────────
    private var waitSeconds = 0
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            waitSeconds++
            val m = waitSeconds / 60
            val s = waitSeconds % 60
            tvTimer.text = String.format("Waiting: %d:%02d", m, s)
            timerHandler.postDelayed(this, 1000)
        }
    }

    // ── polling (separate from timer) ─────────────────────────────────────────
    private val pollHandler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            checkConsultationStatus()
            pollHandler.postDelayed(this, 5000)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation_waiting)

        appointmentId      = intent.getIntExtra("appointment_id", -1)
        doctorId           = intent.getIntExtra("doctor_id", -1)
        doctorName         = intent.getStringExtra("doctor_name")
        doctorSpecialization = intent.getStringExtra("doctor_specialization")
        doctorPhoto        = intent.getStringExtra("doctor_photo")

        if (appointmentId == -1) {
            Toast.makeText(this, "Invalid appointment ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvTimer    = findViewById(R.id.tvTimerHeader)
        btnWaiting = findViewById(R.id.btnWaiting)

        val tvDocName  = findViewById<TextView>(R.id.tvDoctorName)
        val tvDocSpec  = findViewById<TextView>(R.id.tvSpeciality)
        val ivDoctor   = findViewById<ImageView>(R.id.ivDoctorImage)

        // ── doctor info ───────────────────────────────────────────────────────
        if (!doctorName.isNullOrEmpty()) {
            tvDocName.text = "Dr. $doctorName"
        } else if (doctorId != -1) {
            fetchDoctorDetails(doctorId, tvDocName, tvDocSpec)
        }

        if (!doctorSpecialization.isNullOrEmpty()) {
            tvDocSpec.text = doctorSpecialization
        }

        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        val imageUrl = when {
            !doctorPhoto.isNullOrEmpty() && doctorPhoto!!.startsWith("http") -> doctorPhoto
            !doctorPhoto.isNullOrEmpty() && doctorPhoto!!.startsWith("/")    -> "$baseUrl$doctorPhoto"
            doctorId != -1 -> "$baseUrl/api/profile/image/$doctorId?role=doctor"
            else -> null
        }
        AvatarUtils.loadAvatar(ivDoctor, imageUrl, doctorName ?: "Doctor")

        // ── button listeners ──────────────────────────────────────────────────
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        btnWaiting.setOnClickListener {
            Toast.makeText(this, "Please wait until the doctor starts the consultation", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnChat).setOnClickListener {
            val i = Intent(this, AppointmentDetailsActivity::class.java)
            i.putExtra("appointment_id", appointmentId)
            i.putExtra("doctor_name", doctorName)
            i.putExtra("doctor_specialization", doctorSpecialization)
            startActivity(i)
        }

        // ── start timer + polling + socket ────────────────────────────────────
        timerHandler.postDelayed(timerRunnable, 1000)
        pollHandler.postDelayed(pollRunnable, 3000)
        connectSocket()
    }

    // ── socket ────────────────────────────────────────────────────────────────
    private fun connectSocket() {
        try {
            val socket = SocketManager.getSocket(this)

            val patientId = SessionManager.getInstance(this).getUserId()
            socket?.emit("join", JSONObject().apply {
                put("user_id", patientId)
                put("role", "patient")
            })

            // Primary trigger: doctor started consultation
            socket?.on("consultation_started") { args ->
                if (args.isEmpty()) return@on
                val data = args[0] as? JSONObject ?: return@on

                val apptMatch =
                    data.optInt("appointment_id") == appointmentId ||
                    data.optString("appointment_id") == appointmentId.toString()

                if (!apptMatch || isReadyNotified) return@on

                val channel = data.optString("channel", "")
                val conId   = data.optInt("consultation_id", -1)

                if (channel.isNotEmpty() && conId != -1) {
                    runOnUiThread {
                        isReadyNotified = true
                        channelName     = channel
                        consultationId  = conId
                        showNotification()
                        openConsultationReadyScreen()
                    }
                }
            }

            // doctor_ready socket event (backup path)
            socket?.on("doctor_ready") { args ->
                if (isReadyNotified) return@on
                val data = if (args.isNotEmpty()) args[0] as? JSONObject else null
                val apptId = data?.optInt("appointment_id", -1) ?: -1
                if (apptId == -1 || apptId == appointmentId) {
                    runOnUiThread {
                        if (!isReadyNotified) {
                            isReadyNotified = true
                            openConsultationReadyScreen()
                        }
                    }
                }
            }

            socket?.on("consultation_ready") { args ->
                if (isReadyNotified) return@on
                val data = if (args.isNotEmpty()) args[0] as? JSONObject else null
                val apptId = data?.optInt("appointment_id", -1) ?: -1
                if (apptId == -1 || apptId == appointmentId) {
                    runOnUiThread {
                        if (!isReadyNotified) {
                            isReadyNotified = true
                            openConsultationReadyScreen()
                        }
                    }
                }
            }

            socket?.on("appointment_cancelled") { _ ->
                runOnUiThread {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Appointment Cancelled")
                        .setMessage("This appointment has been cancelled.")
                        .setPositiveButton("OK") { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                }
            }

        } catch (e: Exception) {
            Log.e("ConsultationWaiting", "Socket error: ${e.message}")
            e.printStackTrace()
        }
    }

    // ── polling ───────────────────────────────────────────────────────────────
    private fun checkConsultationStatus() {
        if (isReadyNotified) return                   // already navigating away — stop polling

        apiService.getConsultationStatus(appointmentId)
            .enqueue(object : Callback<ConsultationStatusResponse> {
                override fun onResponse(
                    call: Call<ConsultationStatusResponse>,
                    response: Response<ConsultationStatusResponse>
                ) {
                    if (!response.isSuccessful) return
                    val body = response.body() ?: return

                    // Save latest channel/id regardless of status
                    if (!body.channel.isNullOrEmpty())  channelName    = body.channel
                    if (body.consultationId != null)    consultationId = body.consultationId

                    val status = body.status?.trim()?.lowercase() ?: ""

                    Log.d("ConsultationWaiting", "Poll status=$status canJoin=${body.canJoin}")

                    // Update status text
                    val tvStatus = findViewById<TextView?>(R.id.tvStatus)
                    tvStatus?.text = when (status) {
                        "ready", "started", "ongoing" -> "Doctor is ready!"
                        else                          -> "Waiting for Doctor"
                    }

                    // FIX: check all valid "doctor started" statuses
                    if ((status == "ready" || status == "started" || status == "ongoing")
                        && body.canJoin == true
                        && !channelName.isNullOrEmpty()
                        && !isReadyNotified
                    ) {
                        isReadyNotified = true
                        runOnUiThread {
                            showNotification()
                            openConsultationReadyScreen()
                        }
                    }
                }

                override fun onFailure(call: Call<ConsultationStatusResponse>, t: Throwable) {
                    Log.e("ConsultationWaiting", "Status poll failed: ${t.message}")
                }
            })
    }

    // ── navigation ────────────────────────────────────────────────────────────
    private fun openConsultationReadyScreen() {
        pollHandler.removeCallbacks(pollRunnable)   // stop polling before leaving
        val i = Intent(this, ConsultationReadyActivity::class.java).apply {
            putExtra("appointment_id",      appointmentId)
            putExtra("doctor_id",           doctorId)
            putExtra("doctor_name",         doctorName)
            putExtra("doctor_specialization", doctorSpecialization)
            putExtra("doctor_photo",        doctorPhoto)
            putExtra("channel_name",        channelName)
            putExtra("consultation_id",     consultationId ?: -1)
        }
        startActivity(i)
        finish()
    }

    // ── notification ──────────────────────────────────────────────────────────
    private fun showNotification() {
        val channelId = "consultation_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Consultation", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(ch)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, ConsultationReadyActivity::class.java).apply {
                putExtra("appointment_id", appointmentId)
                putExtra("channel_name",   channelName)
                putExtra("consultation_id", consultationId ?: -1)
                putExtra("doctor_id",      doctorId)
                putExtra("doctor_name",    doctorName)
                putExtra("doctor_specialization", doctorSpecialization)
                putExtra("doctor_photo",   doctorPhoto)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_video_white)
            .setContentTitle("Consultation Ready")
            .setContentText("Dr. $doctorName has started your consultation. Tap to join.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(1, notification)
    }

    // ── doctor detail fallback ────────────────────────────────────────────────
    private fun fetchDoctorDetails(id: Int, tvName: TextView, tvSpec: TextView) {
        ApiClient.instance.getDoctorDetails(id)
            .enqueue(object : Callback<DoctorResponse> {
                override fun onResponse(
                    call: Call<DoctorResponse>,
                    response: Response<DoctorResponse>
                ) {
                    if (!response.isSuccessful || response.body() == null) return
                    val doctor = response.body()!!
                    doctorName           = doctor.name
                    doctorSpecialization = doctor.specialization
                    doctorPhoto          = doctor.profileImage

                    tvName.text = "Dr. ${doctor.name}"
                    tvSpec.text = doctor.specialization ?: ""

                    val ivDoctor = findViewById<ImageView>(R.id.ivDoctorImage)
                    val baseUrl  = ApiClient.BASE_URL.removeSuffix("/")
                    val imageUrl = when {
                        !doctor.profileImage.isNullOrEmpty() && doctor.profileImage!!.startsWith("http") -> doctor.profileImage
                        !doctor.profileImage.isNullOrEmpty() && doctor.profileImage!!.startsWith("/")    -> "$baseUrl${doctor.profileImage}"
                        else -> "$baseUrl/api/profile/image/$id?role=doctor"
                    }
                    AvatarUtils.loadAvatar(ivDoctor, imageUrl, doctor.name ?: "Doctor")
                }
                override fun onFailure(call: Call<DoctorResponse>, t: Throwable) {
                    Log.e("ConsultationWaiting", "Doctor fetch failed: ${t.message}")
                }
            })
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────
    override fun onResume() {
        super.onResume()
        // Re-arm poll in case onPause stopped it
        if (!isReadyNotified) {
            pollHandler.removeCallbacks(pollRunnable)
            pollHandler.postDelayed(pollRunnable, 1000)
        }
    }

    override fun onPause() {
        super.onPause()
        pollHandler.removeCallbacks(pollRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
        pollHandler.removeCallbacks(pollRunnable)

        // FIX: Off only our own listeners — do NOT call socket.disconnect()
        //      The socket is shared across the app via SocketManager
        val socket = SocketManager.getSocket(this)
        socket?.off("consultation_started")
        socket?.off("doctor_ready")
        socket?.off("consultation_ready")
        socket?.off("appointment_cancelled")
    }
}