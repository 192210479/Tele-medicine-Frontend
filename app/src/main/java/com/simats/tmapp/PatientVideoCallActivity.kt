package com.simats.tmapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.simats.tmapp.api.ApiClient
import io.agora.rtc2.*
import io.agora.rtc2.Constants
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import org.json.JSONObject

class PatientVideoCallActivity : AppCompatActivity() {
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tvTimer: TextView
    private var isMicOn = true
    private var isVideoOn = true
    private var isSpeakerOn = true
    private var appointmentId: Int = -1
    private var consultationId: Int = -1
    private val apiService = ApiClient.instance
    private lateinit var sessionManager: SessionManager
    
    private var channelName: String? = null
    private lateinit var videoCallManager: VideoCallManager

    private lateinit var localVideoContainer: FrameLayout
    private lateinit var remoteVideoContainer: FrameLayout
    
    // Polling for consultation status
    private val statusCheckHandler = Handler(Looper.getMainLooper())
    private var doctorName: String? = null
    private var doctorSpecialization: String? = null
    
    private val statusCheckRunnable = object : Runnable {
        override fun run() {
            checkCallStatus()
            statusCheckHandler.postDelayed(this, 3000)
        }
    }

    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.INTERNET
    )

    private val timerRunnable = object : Runnable {
        override fun run() {
            seconds++
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            tvTimer.text = String.format("%02d:%02d", minutes, remainingSeconds)
            handler.postDelayed(this, 1000)
        }
    }

    private val videoCallListener = object : VideoCallManager.VideoCallListener {
        override fun onJoinChannelSuccess(channel: String, uid: Int) {
            runOnUiThread {
                Toast.makeText(applicationContext, "Connected to Consultation", Toast.LENGTH_SHORT).show()
                handler.postDelayed(timerRunnable, 1000)
            }
        }

        override fun onRemoteUserJoined(uid: Int) {
            runOnUiThread {
                offlineHandler.removeCallbacks(offlineRunnable)
                videoCallManager.setupRemoteVideo(remoteVideoContainer, uid)
            }
        }

        override fun onRemoteUserOffline(uid: Int) {
            runOnUiThread {
                remoteVideoContainer.removeAllViews()
                Toast.makeText(applicationContext, "Doctor connection lost", Toast.LENGTH_LONG).show()
                offlineHandler.postDelayed(offlineRunnable, 20000)
            }
        }

        override fun onError(err: Int) {
            Log.e("PatientVideoCall", "Agora Error: $err")
            if (err == -100) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Reconnection failed. Ending call.", Toast.LENGTH_LONG).show()
                    endConsultation()
                }
            }
        }

        override fun onConnectionLost() {
            runOnUiThread {
                Toast.makeText(applicationContext, "Connection lost. Reconnecting...", Toast.LENGTH_LONG).show()
            }
        }

        override fun onConnectionInterrupted() {
            runOnUiThread {
                Toast.makeText(applicationContext, "Connection interrupted. Trying to reconnect...", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.d("PatientVideoCall", "Connection state changed: $state, reason: $reason")
        }

        override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
            if (txQuality > 4 || rxQuality > 4) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Poor network connection detected", Toast.LENGTH_SHORT).show()
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

    private val offlineHandler = Handler(Looper.getMainLooper())
    private val offlineRunnable = Runnable {
        Toast.makeText(this, "Doctor connection lost. Please wait or try rejoining later.", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        sessionManager = SessionManager.getInstance(this)
        videoCallManager = VideoCallManager.getInstance(this)
        
        // Step 5 — PREVENT ACTIVITY CRASH
        // Handle both new generic keys and existing specific keys
        channelName = intent.getStringExtra("CHANNEL_NAME") 
            ?: intent.getStringExtra("channel_name") 
            ?: intent.getStringExtra("VIDEO_ROOM")
            
        consultationId = intent.getIntExtra("CONSULTATION_ID", -1)
        if (consultationId == -1) {
            consultationId = intent.getIntExtra("consultation_id", -1)
        }
        
        appointmentId = intent.getIntExtra("appointment_id", -1)
        
        doctorName = intent.getStringExtra("doctor_name")
        doctorSpecialization = intent.getStringExtra("doctor_specialization")

        // Step 6 — ADD LOGGING
        Log.d("Consultation", "Received Channel: $channelName")
        Log.d("Consultation", "Received Consultation ID: $consultationId")

        if (appointmentId == -1 || consultationId == -1 || channelName.isNullOrEmpty()) {
            Log.e("Consultation", "Invalid Intent data. AppointmentID: $appointmentId, ConsultationID: $consultationId, Channel: $channelName")
            Toast.makeText(this, "Consultation room not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val docName = if (doctorName.isNullOrEmpty() || doctorName == "null") "Doctor" else "Dr. $doctorName"
        findViewById<TextView>(R.id.tvDoctorName)?.text = docName
        findViewById<TextView>(R.id.tvSpeciality)?.text = if (doctorSpecialization == "null") "" else doctorSpecialization ?: ""

        val ivDoctorAvatar = findViewById<ImageView>(R.id.ivDoctorAvatar)
        if (ivDoctorAvatar != null) {
            val doctorId = intent.getIntExtra("doctor_id", -1)
            val doctorPhoto = intent.getStringExtra("doctor_photo")
            val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
            val photoUrl = if (!doctorPhoto.isNullOrEmpty()) doctorPhoto else "$baseUrl/api/profile/image/$doctorId?role=doctor"
            
            com.bumptech.glide.Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.bg_circle_soft_blue)
                .circleCrop()
                .into(ivDoctorAvatar)
        }

        tvTimer = findViewById(R.id.tvTimer)
        localVideoContainer = findViewById(R.id.localVideoContainer)
        remoteVideoContainer = findViewById(R.id.remoteVideoContainer)

        if (checkSelfPermission()) {
            initAgoraAndJoin()
        } else {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        }

        findViewById<FloatingActionButton>(R.id.fabMic).setOnClickListener {
            isMicOn = !isMicOn
            (it as FloatingActionButton).setImageResource(if (isMicOn) R.drawable.ic_mic else R.drawable.ic_mic_off)
            videoCallManager.toggleMic(!isMicOn)
        }

        findViewById<FloatingActionButton>(R.id.fabVideo).setOnClickListener {
            isVideoOn = !isVideoOn
            (it as FloatingActionButton).setImageResource(if (isVideoOn) R.drawable.ic_video_white else R.drawable.ic_video_off)
            videoCallManager.toggleVideo(!isVideoOn)
        }

        findViewById<FloatingActionButton>(R.id.fabSpeaker).setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            (it as FloatingActionButton).setImageResource(if (isSpeakerOn) R.drawable.ic_speaker else R.drawable.ic_speaker_off)
            videoCallManager.getRtcEngine()?.setEnableSpeakerphone(isSpeakerOn)
        }

        findViewById<FloatingActionButton>(R.id.fabEndCall).setOnClickListener {
            showEndCallDialog()
        }

        findViewById<FloatingActionButton>(R.id.fabSwitchCamera)?.setOnClickListener {
            videoCallManager.getRtcEngine()?.switchCamera()
        }

        findViewById<FloatingActionButton>(R.id.fabChat).setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("appointment_id", appointmentId)
            intent.putExtra("consultation_id", consultationId)
            startActivity(intent)
        }

        findViewById<FloatingActionButton>(R.id.fabUpload)?.setOnClickListener {
            openFilePicker()
        }

        setupDraggableVideo()
        setupSocketListeners()
    }

    private fun setupDraggableVideo() {
        val cvLocalVideo = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cvLocalVideo)
            ?: return
        var dX = 0f
        var dY = 0f

        cvLocalVideo.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    view.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                }
                android.view.MotionEvent.ACTION_UP -> {
                    view.performClick()
                }
            }
            true
        }
    }

    private fun initAgoraAndJoin() {
        // Call Safety Check
        apiService.pollConsultation(appointmentId, sessionManager.getUserId(), "patient").enqueue(object : Callback<com.simats.tmapp.api.ConsultationStatusResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.ConsultationStatusResponse>, response: Response<com.simats.tmapp.api.ConsultationStatusResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()!!.status
                    if (status.equals("completed", ignoreCase = true)) {
                        Toast.makeText(this@PatientVideoCallActivity, "Consultation already finished", Toast.LENGTH_LONG).show()
                        finish()
                        return
                    }

                    // Proceed with joining
                    videoCallManager.initAgoraEngine(videoCallListener)
                    
                    val userId = sessionManager.getUserId()
                    apiService.getVideoToken(userId, channelName!!, "patient").enqueue(object : Callback<com.simats.tmapp.api.VideoTokenResponse> {
                        override fun onResponse(call: Call<com.simats.tmapp.api.VideoTokenResponse>, response: Response<com.simats.tmapp.api.VideoTokenResponse>) {
                            if (response.isSuccessful && response.body() != null) {
                                val body = response.body()!!
                                android.util.Log.d("Consultation", "TOKEN RECEIVED: ${body.token}")
                                android.util.Log.d("Consultation", "JOINING CHANNEL: ${body.channel}")
                                videoCallManager.joinChannel(body.token, body.channel, body.uid)
                                videoCallManager.setupLocalVideo(localVideoContainer, body.uid)
                                videoCallManager.startPreview()
                                // Start polling when call starts
                                statusCheckHandler.postDelayed(statusCheckRunnable, 3000)
                            } else {
                                Toast.makeText(this@PatientVideoCallActivity, "Failed to get video token", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onFailure(call: Call<com.simats.tmapp.api.VideoTokenResponse>, t: Throwable) {
                            Toast.makeText(this@PatientVideoCallActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
            override fun onFailure(call: Call<com.simats.tmapp.api.ConsultationStatusResponse>, t: Throwable) {
                Toast.makeText(this@PatientVideoCallActivity, "Safety check failed: ${t.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun setupSocketListeners() {
        val socket = SocketService.socket ?: return
        socket.on("consultation_ended") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as org.json.JSONObject
                if (data.optInt("appointment_id") == appointmentId) {
                    runOnUiThread {
                        statusCheckHandler.removeCallbacks(statusCheckRunnable)
                        handleConsultationEnded()
                    }
                }
            }
        }
    }

    private fun checkCallStatus() {
        apiService.getConsultationStatus(appointmentId).enqueue(object : Callback<com.simats.tmapp.api.ConsultationStatusResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.ConsultationStatusResponse>, response: Response<com.simats.tmapp.api.ConsultationStatusResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()?.status
                    if (status.equals("ended", ignoreCase = true) || status.equals("completed", ignoreCase = true)) {
                        statusCheckHandler.removeCallbacks(statusCheckRunnable)
                        handleConsultationEnded()
                    }
                }
            }
            override fun onFailure(call: Call<com.simats.tmapp.api.ConsultationStatusResponse>, t: Throwable) {
                Log.e("CallStatus", "Failed to check status: ${t.message}")
            }
        })
    }

    private fun handleConsultationEnded() {
        videoCallManager.leaveChannel()

        AlertDialog.Builder(this)
            .setTitle("Consultation Ended")
            .setMessage("Doctor has ended the consultation")
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(this, PrescriptionWaitingActivity::class.java)
                intent.putExtra("appointment_id", appointmentId)
                intent.putExtra("doctor_id", intent.getIntExtra("doctor_id", -1))
                intent.putExtra("doctor_name", doctorName)
                intent.putExtra("doctor_specialization", doctorSpecialization)
                startActivity(intent)
                finish()
            }
            .show()
    }

    private fun checkSelfPermission(): Boolean {
        for (permission in REQUESTED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initAgoraAndJoin()
            } else {
                Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private val pickFileLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let { showUploadDialog(it) }
    }

    private fun openFilePicker() {
        pickFileLauncher.launch("*/*")
    }

    private fun showUploadDialog(uri: android.net.Uri) {
        val editText = android.widget.EditText(this)
        editText.hint = "Record type (e.g. Test Result)"
        
        Toast.makeText(this, "Preparing to share report...", Toast.LENGTH_SHORT).show()

        androidx.appcompat.app.AlertDialog.Builder(this)
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
        val patientId = sessionManager.getUserId()
        val file = getFileFromUri(uri) ?: return
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val requestFile = okhttp3.RequestBody.create(mimeType.toMediaTypeOrNull(), file)
        val body = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)
        
        val doctorId = intent.getIntExtra("doctor_id", -1)
        
        val userIdBody = patientId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val roleBody = "patient".toRequestBody("text/plain".toMediaTypeOrNull())
        val typeBody = type.toRequestBody("text/plain".toMediaTypeOrNull())
        val appointmentIdBody = appointmentId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val patientIdBody = patientId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val doctorIdBody = doctorId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        apiService.uploadMedicalRecord(userIdBody, roleBody, typeBody, patientIdBody, doctorIdBody, appointmentIdBody, body)
            .enqueue(object : Callback<com.simats.tmapp.api.GenericResponse> {
                override fun onResponse(call: Call<com.simats.tmapp.api.GenericResponse>, response: Response<com.simats.tmapp.api.GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@PatientVideoCallActivity, "Report shared with doctor", Toast.LENGTH_SHORT).show()
                        SocketService.socket?.emit("medical_record_uploaded", org.json.JSONObject().apply {
                            put("user_id", patientId)
                            put("role", "patient")
                            put("patient_id", patientId)
                            put("doctor_id", doctorId)
                            put("appointment_id", appointmentId)
                        })
                    } else {
                        Toast.makeText(this@PatientVideoCallActivity, "Failed to share report", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<com.simats.tmapp.api.GenericResponse>, t: Throwable) {
                    Toast.makeText(this@PatientVideoCallActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun getFileFromUri(uri: android.net.Uri): java.io.File? {
        val fileName = getFileName(uri) ?: "temp_file"
        val file = java.io.File(cacheDir, fileName)
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                java.io.FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return file
        } catch (e: Exception) {
            return null
        }
    }

    private fun getFileName(uri: android.net.Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun showEndCallDialog() {
        AlertDialog.Builder(this)
            .setTitle("End Call")
            .setMessage("Are you sure you want to end this consultation?")
            .setPositiveButton("End Call") { _, _ -> endConsultation() }
            .setNegativeButton("Continue", null)
            .show()
    }

    private fun endConsultation() {
        videoCallManager.leaveChannel()
        val request = com.simats.tmapp.api.ConsultationEndRequest(consultationId = consultationId)
        apiService.endConsultationV2(request).enqueue(object : Callback<com.simats.tmapp.api.GenericResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.GenericResponse>, response: Response<com.simats.tmapp.api.GenericResponse>) {
                navigateBack()
            }
            override fun onFailure(call: Call<com.simats.tmapp.api.GenericResponse>, t: Throwable) {
                navigateBack()
            }
        })
    }

    private fun navigateBack() {
        val intent = Intent(this, PrescriptionWaitingActivity::class.java)
        intent.putExtra("appointment_id", appointmentId)
        intent.putExtra("consultation_id", consultationId)
        intent.putExtra("doctor_id", intent.getIntExtra("doctor_id", -1))
        intent.putExtra("doctor_name", doctorName)
        intent.putExtra("doctor_specialization", doctorSpecialization)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoCallManager.leaveChannel()
        handler.removeCallbacks(timerRunnable)
        statusCheckHandler.removeCallbacks(statusCheckRunnable)
        SocketService.socket?.off("consultation_ended")
    }
}
