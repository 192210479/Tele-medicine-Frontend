package com.simats.Tmapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.ConsultationEndRequest
import com.simats.Tmapp.api.ConsultationStatusResponse
import com.simats.Tmapp.api.GenericResponse
import com.simats.Tmapp.api.VideoTokenResponse
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PatientVideoCallActivity : AppCompatActivity() {

    // ── state ─────────────────────────────────────────────────────────────────
    private var appointmentId: Int = -1
    private var consultationId: Int = -1
    private var channelName: String? = null
    private var doctorName: String? = null
    private var doctorSpecialization: String? = null

    private var startedAt: Long = 0L
    private var isSpeakerOn = true
    private var isMicOn = true
    private var isVideoOn = true
    private var isSignalSent = false
    private var isRemoteJoinedHandled = false
    private var isControlsVisible = true
    private var isCallEnded = false     // FIX: guard against double-ending the call

    // ── services ──────────────────────────────────────────────────────────────
    private val apiService = ApiClient.instance
    private lateinit var sessionManager: SessionManager
    private lateinit var videoCallManager: VideoCallManager

    // ── UI ────────────────────────────────────────────────────────────────────
    private lateinit var tvTimer: TextView
    private lateinit var localVideoContainer: FrameLayout
    private lateinit var remoteVideoContainer: FrameLayout
    private lateinit var clTopBar: View
    private lateinit var llControls: View

    // ── handlers ──────────────────────────────────────────────────────────────
    private val timerHandler        = Handler(Looper.getMainLooper())
    private val statusCheckHandler  = Handler(Looper.getMainLooper())
    private val hideControlsHandler = Handler(Looper.getMainLooper())
    private val offlineHandler      = Handler(Looper.getMainLooper())

    // ── permissions ───────────────────────────────────────────────────────────
    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.INTERNET
    )

    // ── timer runnable ────────────────────────────────────────────────────────
    private var timerSeconds = 0
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (startedAt > 0) {
                val serverStartSecs = if (startedAt > 1_000_000_000_000L) startedAt / 1000 else startedAt
                val elapsed = (System.currentTimeMillis() / 1000) - serverStartSecs
                timerSeconds = if (elapsed > 0) elapsed.toInt() else 0
            } else {
                timerSeconds++
            }
            val m = timerSeconds / 60
            val s = timerSeconds % 60
            tvTimer.text = String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s)
            timerHandler.postDelayed(this, 1000)
        }
    }

    // ── status poll runnable ──────────────────────────────────────────────────
    private val statusCheckRunnable = object : Runnable {
        override fun run() {
            if (!isCallEnded) {
                checkCallStatus()
                statusCheckHandler.postDelayed(this, 5000)
            }
        }
    }

    // ── controls hide runnable ────────────────────────────────────────────────
    private val hideControlsRunnable = Runnable { hideControls() }

    // ── offline doctor runnable ───────────────────────────────────────────────
    private val offlineRunnable = Runnable {
        if (!isCallEnded) {
            Toast.makeText(this, "Doctor connection lost. Please wait or rejoin later.", Toast.LENGTH_LONG).show()
        }
    }

    // ── Agora event listener ──────────────────────────────────────────────────
    private val videoCallListener = object : VideoCallManager.VideoCallListener {

        override fun onJoinChannelSuccess(channel: String, uid: Int) {
            runOnUiThread {
                Toast.makeText(applicationContext, "Joining call…", Toast.LENGTH_SHORT).show()
                tvTimer.text = "00:00"

                // Emit once — guard with isSignalSent
                if (!isSignalSent) {
                    isSignalSent = true
                    SocketService.socket?.emit("join_room", org.json.JSONObject().apply {
                        put("appointment_id", appointmentId)
                        put("room", "consult_$appointmentId")
                    })
                    SocketService.socket?.emit("patient_ready", org.json.JSONObject().apply {
                        put("appointment_id", appointmentId)
                    })
                    Log.d("SIGNALING", "Emitted join_room + patient_ready")
                }
            }
        }

        override fun onRemoteUserJoined(uid: Int) {
            runOnUiThread {
                offlineHandler.removeCallbacks(offlineRunnable)

                // Render remote video
                remoteVideoContainer.removeAllViews()
                val remoteView = RtcEngine.CreateRendererView(this@PatientVideoCallActivity)
                remoteVideoContainer.addView(remoteView)
                videoCallManager.getRtcEngine()?.setupRemoteVideo(
                    VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
                )

                // Sync timer exactly once when the remote user first joins
                if (!isRemoteJoinedHandled) {
                    isRemoteJoinedHandled = true
                    syncTimerWithServer()
                }
            }
        }

        override fun onRemoteUserOffline(uid: Int) {
            runOnUiThread {
                remoteVideoContainer.removeAllViews()
                Toast.makeText(applicationContext, "Doctor disconnected. Waiting…", Toast.LENGTH_LONG).show()
                offlineHandler.postDelayed(offlineRunnable, 20_000)
                // Allow re-handling if doctor rejoins
                isRemoteJoinedHandled = false
            }
        }

        override fun onError(err: Int) {
            Log.e("PatientVideoCall", "Agora error: $err")
            if (err == -100) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Reconnection failed. Ending call.", Toast.LENGTH_LONG).show()
                    endConsultation()
                }
            }
        }

        override fun onConnectionLost() {
            runOnUiThread {
                Toast.makeText(applicationContext, "Connection lost. Reconnecting…", Toast.LENGTH_LONG).show()
            }
        }

        override fun onConnectionInterrupted() {
            runOnUiThread {
                Toast.makeText(applicationContext, "Connection interrupted. Retrying…", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.d("PatientVideoCall", "Connection state=$state reason=$reason")
        }

        override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
            if (txQuality > 4 || rxQuality > 4) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Poor network detected", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onRtcStats(stats: IRtcEngineEventHandler.RtcStats) {
            if (stats.lastmileDelay > 500) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "High latency detected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        sessionManager   = SessionManager.getInstance(this)
        videoCallManager = VideoCallManager.getInstance(this)

        // ── read intent extras ────────────────────────────────────────────────
        channelName = intent.getStringExtra("channel_name")
            ?: intent.getStringExtra("CHANNEL_NAME")
            ?: intent.getStringExtra("VIDEO_ROOM")

        consultationId = intent.getIntExtra("CONSULTATION_ID", -1).takeIf { it != -1 }
            ?: intent.getIntExtra("consultation_id", -1)

        appointmentId    = intent.getIntExtra("appointment_id", -1)
        doctorName       = intent.getStringExtra("doctor_name")
        doctorSpecialization = intent.getStringExtra("doctor_specialization")

        Log.d("PatientVideoCall", "Channel=$channelName  ConsultationID=$consultationId  ApptID=$appointmentId")

        // ── validate ──────────────────────────────────────────────────────────
        if (appointmentId == -1 || consultationId == -1 || channelName.isNullOrEmpty()) {
            Log.e("PatientVideoCall", "Invalid intent data — aborting")
            Toast.makeText(this, "Consultation room not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ── views ─────────────────────────────────────────────────────────────
        tvTimer              = findViewById(R.id.tvTimer)
        localVideoContainer  = findViewById(R.id.localVideoContainer)
        remoteVideoContainer = findViewById(R.id.remoteVideoContainer)
        clTopBar             = findViewById(R.id.clTopBar)
        llControls           = findViewById(R.id.llControls)

        // ── doctor name / avatar ──────────────────────────────────────────────
        val displayName = if (doctorName.isNullOrEmpty() || doctorName == "null") "Doctor" else "Dr. $doctorName"
        findViewById<TextView?>(R.id.tvDoctorName)?.text = displayName
        findViewById<TextView?>(R.id.tvSpeciality)?.text = if (doctorSpecialization == "null") "" else doctorSpecialization ?: ""

        val ivAvatar = findViewById<ImageView?>(R.id.ivDoctorAvatar)
        if (ivAvatar != null) {
            val doctorId    = intent.getIntExtra("doctor_id", -1)
            val doctorPhoto = intent.getStringExtra("doctor_photo")
            val baseUrl     = ApiClient.BASE_URL.removeSuffix("/")
            val photoUrl    = if (!doctorPhoto.isNullOrEmpty()) doctorPhoto
                              else "$baseUrl/api/profile/image/$doctorId?role=doctor"
            com.bumptech.glide.Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.bg_circle_soft_blue)
                .circleCrop()
                .into(ivAvatar)
        }

        // ── initial controls state ────────────────────────────────────────────
        hideControls()

        remoteVideoContainer.setOnClickListener {
            if (isControlsVisible) hideControls() else showControls()
        }
        findViewById<View?>(R.id.cvLocalVideo)?.setOnClickListener {
            if (isControlsVisible) hideControls() else showControls()
        }

        // ── FAB: mic ──────────────────────────────────────────────────────────
        findViewById<FloatingActionButton>(R.id.fabMic).setOnClickListener {
            resetHideTimer()
            isMicOn = !isMicOn
            (it as FloatingActionButton).apply {
                setImageResource(if (isMicOn) R.drawable.ic_mic else R.drawable.ic_mic_off)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (isMicOn) android.graphics.Color.parseColor("#B3000000")
                    else android.graphics.Color.parseColor("#EF4444")
                )
            }
            videoCallManager.setMicEnabled(isMicOn)
        }

        // ── FAB: video ────────────────────────────────────────────────────────
        findViewById<FloatingActionButton>(R.id.fabVideo).setOnClickListener {
            resetHideTimer()
            isVideoOn = !isVideoOn
            (it as FloatingActionButton).apply {
                setImageResource(if (isVideoOn) R.drawable.ic_video else R.drawable.ic_video_off)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (isVideoOn) android.graphics.Color.parseColor("#B3000000")
                    else android.graphics.Color.parseColor("#EF4444")
                )
            }
            videoCallManager.setVideoEnabled(isVideoOn)
            localVideoContainer.visibility = if (isVideoOn) View.VISIBLE else View.INVISIBLE
            findViewById<ImageView?>(R.id.ivLocalPlaceholder)?.visibility = if (isVideoOn) View.GONE else View.VISIBLE
        }

        // ── FAB: speaker ──────────────────────────────────────────────────────
        findViewById<FloatingActionButton>(R.id.fabSpeaker).setOnClickListener {
            resetHideTimer()
            isSpeakerOn = !isSpeakerOn
            (it as FloatingActionButton).apply {
                setImageResource(if (isSpeakerOn) R.drawable.ic_speaker else R.drawable.ic_speaker_off)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (isSpeakerOn) android.graphics.Color.parseColor("#2F6FED")
                    else android.graphics.Color.parseColor("#B3000000")
                )
            }
            videoCallManager.getRtcEngine()?.setEnableSpeakerphone(isSpeakerOn)
        }

        // ── FAB: end call ─────────────────────────────────────────────────────
        findViewById<FloatingActionButton>(R.id.fabEndCall).setOnClickListener {
            resetHideTimer()
            showEndCallDialog()
        }

        // ── FAB: switch camera ────────────────────────────────────────────────
        findViewById<FloatingActionButton?>(R.id.fabSwitchCamera)?.setOnClickListener {
            resetHideTimer()
            videoCallManager.switchCamera()
        }

        // ── FAB: chat ─────────────────────────────────────────────────────────
        findViewById<FloatingActionButton>(R.id.fabChat).setOnClickListener {
            resetHideTimer()
            startActivity(
                Intent(this, ChatActivity::class.java).apply {
                    putExtra("appointment_id",  appointmentId)
                    putExtra("consultation_id", consultationId)
                }
            )
        }

        // ── FAB: upload ───────────────────────────────────────────────────────
        findViewById<FloatingActionButton?>(R.id.fabUpload)?.setOnClickListener {
            resetHideTimer()
            openFilePicker()
        }

        setupDraggableVideo()
        setupSocketListeners()

        // ── permissions + join ────────────────────────────────────────────────
        if (checkSelfPermission()) initAgoraAndJoin()
        else ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
    }

    // ── agora init + join ─────────────────────────────────────────────────────
    private fun initAgoraAndJoin() {
        // Safety check: is consultation already finished?
        apiService.pollConsultation(appointmentId, sessionManager.getUserId(), "patient")
            .enqueue(object : Callback<ConsultationStatusResponse> {
                override fun onResponse(
                    call: Call<ConsultationStatusResponse>,
                    response: Response<ConsultationStatusResponse>
                ) {
                    val body   = response.body()
                    val status = body?.status?.lowercase() ?: ""

                    if (status == "completed" || status == "ended") {
                        Toast.makeText(this@PatientVideoCallActivity, "Consultation already finished", Toast.LENGTH_LONG).show()
                        finish()
                        return
                    }

                    startedAt = body?.startedAt ?: 0L

                    videoCallManager.initAgoraEngine(videoCallListener)

                    val userId = sessionManager.getUserId()
                    apiService.getVideoToken(userId, channelName!!, "patient")
                        .enqueue(object : Callback<VideoTokenResponse> {
                            override fun onResponse(
                                call: Call<VideoTokenResponse>,
                                response: Response<VideoTokenResponse>
                            ) {
                                if (!response.isSuccessful || response.body() == null) {
                                    Toast.makeText(this@PatientVideoCallActivity, "Failed to get video token", Toast.LENGTH_SHORT).show()
                                    return
                                }
                                val tokenBody = response.body()!!

                                if (tokenBody.startedAt != null && tokenBody.startedAt!! > 0) {
                                    startedAt = tokenBody.startedAt!!
                                }

                                Log.d("PatientVideoCall", "Token: ${tokenBody.token}  Channel: ${tokenBody.channel}  UID: ${tokenBody.uid}")

                                // Setup local video surface
                                localVideoContainer.removeAllViews()
                                val surfaceView = RtcEngine.CreateRendererView(this@PatientVideoCallActivity)
                                surfaceView.setZOrderMediaOverlay(true)
                                localVideoContainer.addView(surfaceView)
                                videoCallManager.getRtcEngine()?.setupLocalVideo(
                                    VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
                                )
                                videoCallManager.startPreview()

                                // Join channel
                                videoCallManager.joinChannel(tokenBody.token, tokenBody.channel, tokenBody.uid)

                                // FIX: Start status polling AFTER join is initiated (not before)
                                statusCheckHandler.removeCallbacks(statusCheckRunnable)
                                statusCheckHandler.postDelayed(statusCheckRunnable, 5000)
                            }

                            override fun onFailure(call: Call<VideoTokenResponse>, t: Throwable) {
                                Toast.makeText(this@PatientVideoCallActivity, "Token error: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onFailure(call: Call<ConsultationStatusResponse>, t: Throwable) {
                    Toast.makeText(this@PatientVideoCallActivity, "Safety check failed: ${t.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    // ── sync timer with server ────────────────────────────────────────────────
    private fun syncTimerWithServer() {
        apiService.pollConsultation(appointmentId, sessionManager.getUserId(), "patient")
            .enqueue(object : Callback<ConsultationStatusResponse> {
                override fun onResponse(
                    call: Call<ConsultationStatusResponse>,
                    response: Response<ConsultationStatusResponse>
                ) {
                    val serverStart = response.body()?.startedAt
                    startedAt = if (serverStart != null && serverStart > 0) serverStart else 0L
                    timerHandler.removeCallbacks(timerRunnable)
                    timerHandler.post(timerRunnable)
                }
                override fun onFailure(call: Call<ConsultationStatusResponse>, t: Throwable) {
                    startedAt = 0L
                    timerHandler.removeCallbacks(timerRunnable)
                    timerHandler.post(timerRunnable)
                }
            })
    }

    // ── socket listeners ──────────────────────────────────────────────────────
    private fun setupSocketListeners() {
        val socket = SocketService.socket ?: return

        socket.on("both_users_connected") {
            Log.d("SIGNALING", "Both users connected in consult_$appointmentId")
        }

        socket.on("offer") { _ ->
            // Acknowledge offer
            socket.emit("answer", org.json.JSONObject().apply {
                put("appointment_id", appointmentId)
                put("status", "ready")
            })
        }

        socket.on("call_started") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as? org.json.JSONObject ?: return@on
                if (data.optInt("appointment_id") == appointmentId) {
                    val t = data.optLong("start_time", 0L)
                    runOnUiThread {
                        if (t > 0) startedAt = t
                        timerHandler.removeCallbacks(timerRunnable)
                        timerHandler.post(timerRunnable)
                    }
                }
            }
        }

        socket.on("consultation_ended") { args ->
            val data  = if (args.isNotEmpty()) args[0] as? org.json.JSONObject else null
            val apptId = data?.optInt("appointment_id", -1) ?: -1
            if (apptId == -1 || apptId == appointmentId) {
                runOnUiThread {
                    statusCheckHandler.removeCallbacks(statusCheckRunnable)
                    handleConsultationEnded("Doctor ended the call")
                }
            }
        }

        socket.on("doctor_ended_call") { _ ->
            runOnUiThread { handleConsultationEnded("Doctor ended the call") }
        }

        socket.on("patient_ended_call") { _ ->
            runOnUiThread { handleConsultationEnded("You ended the call") }
        }

        socket.on("user_disconnected") { _ ->
            runOnUiThread { Toast.makeText(this, "User left the call", Toast.LENGTH_SHORT).show() }
        }
    }

    // ── status polling ────────────────────────────────────────────────────────
    private fun checkCallStatus() {
        apiService.getConsultationStatus(appointmentId)
            .enqueue(object : Callback<ConsultationStatusResponse> {
                override fun onResponse(
                    call: Call<ConsultationStatusResponse>,
                    response: Response<ConsultationStatusResponse>
                ) {
                    val status = response.body()?.status?.lowercase() ?: return
                    if (status == "ended" || status == "completed") {
                        statusCheckHandler.removeCallbacks(statusCheckRunnable)
                        runOnUiThread { handleConsultationEnded() }
                    }
                }
                override fun onFailure(call: Call<ConsultationStatusResponse>, t: Throwable) {
                    Log.e("PatientVideoCall", "Status check failed: ${t.message}")
                }
            })
    }

    // ── consultation ended ────────────────────────────────────────────────────
    private fun handleConsultationEnded(message: String = "Consultation has ended") {
        // FIX: Guard against showing the dialog twice
        if (isCallEnded) return
        isCallEnded = true

        videoCallManager.leaveChannel()
        timerHandler.removeCallbacks(timerRunnable)
        statusCheckHandler.removeCallbacks(statusCheckRunnable)

        AlertDialog.Builder(this)
            .setTitle("Consultation Ended")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ -> navigateToPrescriptionWaiting() }
            .show()
    }

    private fun navigateToPrescriptionWaiting() {
        startActivity(
            Intent(this, PrescriptionWaitingActivity::class.java).apply {
                putExtra("appointment_id",      appointmentId)
                putExtra("consultation_id",     consultationId)
                putExtra("doctor_id",           intent.getIntExtra("doctor_id", -1))
                putExtra("doctor_name",         doctorName)
                putExtra("doctor_specialization", doctorSpecialization)
            }
        )
        finish()
    }

    // ── end call ──────────────────────────────────────────────────────────────
    private fun showEndCallDialog() {
        AlertDialog.Builder(this)
            .setTitle("End Call")
            .setMessage("Are you sure you want to end this consultation?")
            .setPositiveButton("End Call")  { _, _ -> endConsultation() }
            .setNegativeButton("Continue", null)
            .show()
    }

    private fun endConsultation() {
        if (isCallEnded) return
        isCallEnded = true

        videoCallManager.leaveChannel()
        timerHandler.removeCallbacks(timerRunnable)
        statusCheckHandler.removeCallbacks(statusCheckRunnable)

        apiService.endConsultationV2(ConsultationEndRequest(consultationId = consultationId))
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    navigateToPrescriptionWaiting()
                }
                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    navigateToPrescriptionWaiting()
                }
            })
    }

    // ── draggable local video ─────────────────────────────────────────────────
    private fun setupDraggableVideo() {
        val cvLocalVideo = findViewById<com.google.android.material.card.MaterialCardView?>(R.id.cvLocalVideo) ?: return
        var dX = 0f
        var dY = 0f
        cvLocalVideo.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> { dX = view.x - event.rawX; dY = view.y - event.rawY }
                android.view.MotionEvent.ACTION_MOVE -> {
                    view.animate().x(event.rawX + dX).y(event.rawY + dY).setDuration(0).start()
                }
                android.view.MotionEvent.ACTION_UP -> view.performClick()
            }
            true
        }
    }

    // ── controls show/hide ────────────────────────────────────────────────────
    private fun showControls() {
        if (isControlsVisible) { resetHideTimer(); return }
        isControlsVisible = true
        clTopBar.visibility   = View.VISIBLE
        llControls.visibility = View.VISIBLE
        clTopBar.animate().alpha(1f).setDuration(300).start()
        llControls.animate().alpha(1f).setDuration(300).start()
        resetHideTimer()
    }

    private fun hideControls() {
        if (!isControlsVisible) return
        isControlsVisible = false
        clTopBar.animate().alpha(0f).setDuration(300).withEndAction { clTopBar.visibility = View.GONE }.start()
        llControls.animate().alpha(0f).setDuration(300).withEndAction { llControls.visibility = View.GONE }.start()
        hideControlsHandler.removeCallbacks(hideControlsRunnable)
    }

    private fun resetHideTimer() {
        hideControlsHandler.removeCallbacks(hideControlsRunnable)
        hideControlsHandler.postDelayed(hideControlsRunnable, 3000)
    }

    // ── file upload ───────────────────────────────────────────────────────────
    private val pickFileLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
            uri?.let { showUploadDialog(it) }
        }

    private fun openFilePicker() { pickFileLauncher.launch("*/*") }

    private fun showUploadDialog(uri: android.net.Uri) {
        val editText = android.widget.EditText(this).apply { hint = "Record type (e.g. Test Result)" }
        AlertDialog.Builder(this)
            .setTitle("Share Medical Report")
            .setView(editText)
            .setPositiveButton("Share") { _, _ ->
                val type = editText.text.toString().ifEmpty { "Shared Report" }
                uploadMedicalRecord(uri, type)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadMedicalRecord(uri: android.net.Uri, type: String) {
        val patientId     = sessionManager.getUserId()
        val file          = getFileFromUri(uri) ?: return
        val mimeType      = contentResolver.getType(uri) ?: "application/octet-stream"
        val requestFile   = okhttp3.RequestBody.create(mimeType.toMediaTypeOrNull(), file)
        val body          = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)
        val doctorIdFromIntent = intent.getIntExtra("doctor_id", -1)

        apiService.uploadMedicalRecord(
            patientId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            "patient".toRequestBody("text/plain".toMediaTypeOrNull()),
            type.toRequestBody("text/plain".toMediaTypeOrNull()),
            patientId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            doctorIdFromIntent.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            appointmentId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            listOf(body)
        ).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@PatientVideoCallActivity, "File shared with doctor", Toast.LENGTH_SHORT).show()
                    SocketService.socket?.emit("medical_record_uploaded", org.json.JSONObject().apply {
                        put("user_id",        patientId)
                        put("role",           "patient")
                        put("patient_id",     patientId)
                        put("doctor_id",      doctorIdFromIntent)
                        put("appointment_id", appointmentId)
                    })
                } else {
                    Toast.makeText(this@PatientVideoCallActivity, "Upload failed. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@PatientVideoCallActivity, "Network error during upload.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getFileFromUri(uri: android.net.Uri): java.io.File? {
        val fileName = getFileName(uri) ?: "temp_file"
        val file = java.io.File(cacheDir, fileName)
        return try {
            contentResolver.openInputStream(uri)?.use { it.copyTo(java.io.FileOutputStream(file)) }
            file
        } catch (e: Exception) { null }
    }

    private fun getFileName(uri: android.net.Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val i = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (i != -1) name = cursor.getString(i)
            }
        }
        return name
    }

    // ── permissions ───────────────────────────────────────────────────────────
    private fun checkSelfPermission(): Boolean =
        REQUESTED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) initAgoraAndJoin()
            else { Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show(); finish() }
        }
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────
    override fun onResume() {
        super.onResume()
        if (!isCallEnded) {
            statusCheckHandler.removeCallbacks(statusCheckRunnable)
            statusCheckHandler.postDelayed(statusCheckRunnable, 3000)
        }
    }

    override fun onPause() {
        super.onPause()
        statusCheckHandler.removeCallbacks(statusCheckRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        // FIX: leaveChannel() is safe to call again — it is idempotent
        if (!isCallEnded) videoCallManager.leaveChannel()

        timerHandler.removeCallbacks(timerRunnable)
        statusCheckHandler.removeCallbacks(statusCheckRunnable)
        hideControlsHandler.removeCallbacks(hideControlsRunnable)
        offlineHandler.removeCallbacks(offlineRunnable)

        // Off only our own listeners — do NOT disconnect the shared socket
        SocketService.socket?.off("consultation_ended")
        SocketService.socket?.off("call_started")
        SocketService.socket?.off("offer")
        SocketService.socket?.off("both_users_connected")
        SocketService.socket?.off("doctor_ended_call")
        SocketService.socket?.off("patient_ended_call")
        SocketService.socket?.off("user_disconnected")
    }
}