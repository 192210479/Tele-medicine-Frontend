package com.simats.tmapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.simats.tmapp.api.MedicalRecordResponse
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

class VideoConsultationActivity : AppCompatActivity() {
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tvTimer: TextView
    private var isMicOn = true
    private var isVideoOn = true
    private var isSpeakerOn = true
    private var appointmentId: Int = -1
    private var patientName: String? = null
    private var patientAge: Int = -1
    private var patientGender: String? = null
    private var patientId: Int = -1
    private val apiService = ApiClient.instance
    private lateinit var sessionManager: SessionManager
    
    private var channelName: String? = null
    private var consultationId: Int = -1
    private lateinit var videoCallManager: VideoCallManager
    private lateinit var localVideoContainer: FrameLayout
    private lateinit var remoteVideoContainer: FrameLayout

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
                Toast.makeText(applicationContext, "Participant connection lost", Toast.LENGTH_LONG).show()
                offlineHandler.postDelayed(offlineRunnable, 20000)
            }
        }

        override fun onError(err: Int) {
            Log.e("VideoConsultation", "Agora Error: $err")
            if (err == -100) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Reconnection failed. Ending call.", Toast.LENGTH_LONG).show()
                    endConsultation(false)
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
            Log.d("VideoConsultation", "Connection state changed: $state, reason: $reason")
        }

        override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
            if (txQuality > 4 || rxQuality > 4) { // 4 is POOR, 5 is BAD, 6 is DOWN
                runOnUiThread {
                    Toast.makeText(applicationContext, "Poor network connection detected", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onRtcStats(stats: IRtcEngineEventHandler.RtcStats) {
            // Monitor packet loss or latency if needed
            if (stats.lastmileDelay > 500) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "High latency detected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val offlineHandler = Handler(Looper.getMainLooper())
    private val offlineRunnable = Runnable {
        Toast.makeText(this, "Participant connection lost for too long.", Toast.LENGTH_LONG).show()
        // Allow doctor to end consultation (already has the button)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_consultation)
        
        sessionManager = SessionManager.getInstance(this)
        videoCallManager = VideoCallManager.getInstance(this)

        appointmentId = intent.getIntExtra("appointment_id", -1)
        consultationId = intent.getIntExtra("consultation_id", -1)
        channelName = intent.getStringExtra("channel_name") ?: intent.getStringExtra("VIDEO_ROOM") ?: intent.getStringExtra("session_id")
        val doctorName = intent.getStringExtra("doctor_name")
        val specialization = intent.getStringExtra("doctor_specialization")

        if (appointmentId == -1 || channelName.isNullOrEmpty()) {
            Toast.makeText(this, "Consultation room not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (sessionManager.getUserRole().equals("doctor", ignoreCase = true) && consultationId != -1) {
            setConsultationReady(consultationId)
        }

        val docName = if (doctorName.isNullOrEmpty() || doctorName == "null") "Dr. ${sessionManager.getUserName()}" else "Dr. $doctorName"
        findViewById<TextView>(R.id.tvDoctorName)?.text = docName
        findViewById<TextView>(R.id.tvSpeciality)?.text = specialization ?: ""

        patientName = intent.getStringExtra("patient_name")
        patientAge = intent.getIntExtra("patient_age", -1)
        patientGender = intent.getStringExtra("patient_gender")

        if (!patientName.isNullOrEmpty() && patientName != "null") {
            findViewById<TextView>(R.id.tvPatientName)?.text = patientName
        } else {
            findViewById<TextView>(R.id.tvPatientName)?.text = "Patient"
        }
        
        findViewById<TextView>(R.id.tvPatientDetails)?.text = if (patientAge != -1) "Age: $patientAge | Gender: ${patientGender ?: "--"}" else "--"

        patientId = intent.getIntExtra("patient_id", -1)
        val ivPatientAvatar = findViewById<ImageView>(R.id.ivPatientAvatar)
        if (ivPatientAvatar != null) {
            val patientIdFromIntent = intent.getIntExtra("patient_id", -1)
            val patientPhoto = intent.getStringExtra("patient_photo")
            val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
            val photoUrl = if (!patientPhoto.isNullOrEmpty()) patientPhoto else "$baseUrl/api/profile/image/$patientIdFromIntent?role=patient"
            
            com.bumptech.glide.Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.bg_circle_soft_blue)
                .circleCrop()
                .into(ivPatientAvatar)
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
            showEndConsultationDialog()
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

        findViewById<FloatingActionButton>(R.id.fabPatientData).setOnClickListener {
            val intent = Intent(this, PatientDataActivity::class.java)
            intent.putExtra("patient_id", patientId)
            intent.putExtra("appointment_id", appointmentId)
            intent.putExtra("consultation_id", consultationId)
            startActivity(intent)
        }

        val uploadClickListener = View.OnClickListener {
            openFilePicker()
        }
        findViewById<FloatingActionButton>(R.id.fabUpload).setOnClickListener(uploadClickListener)
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cvUploadReport).setOnClickListener(uploadClickListener)

        setupDraggableVideo()
        setupSocket()
    }

    private fun initAgoraAndJoin() {
        // Call Safety Check
        apiService.pollConsultation(appointmentId, sessionManager.getUserId(), "doctor").enqueue(object : Callback<com.simats.tmapp.api.ConsultationStatusResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.ConsultationStatusResponse>, response: Response<com.simats.tmapp.api.ConsultationStatusResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()!!.status
                    if (status.equals("completed", ignoreCase = true)) {
                        Toast.makeText(this@VideoConsultationActivity, "Consultation already finished", Toast.LENGTH_LONG).show()
                        finish()
                        return
                    }

                    // Proceed with joining
                    videoCallManager.initAgoraEngine(videoCallListener)
                    
                    val userId = sessionManager.getUserId()
                    apiService.getVideoToken(userId, channelName!!, "doctor").enqueue(object : Callback<com.simats.tmapp.api.VideoTokenResponse> {
                        override fun onResponse(call: Call<com.simats.tmapp.api.VideoTokenResponse>, response: Response<com.simats.tmapp.api.VideoTokenResponse>) {
                            if (response.isSuccessful && response.body() != null) {
                                val body = response.body()!!
                                android.util.Log.d("Consultation", "TOKEN RECEIVED: ${body.token}")
                                android.util.Log.d("Consultation", "JOINING CHANNEL: ${body.channel}")
                                videoCallManager.joinChannel(body.token, body.channel, body.uid)
                                videoCallManager.setupLocalVideo(localVideoContainer, body.uid)
                                videoCallManager.startPreview()
                                startRecordingIfNeeded()
                            } else {
                                Toast.makeText(this@VideoConsultationActivity, "Failed to get video token", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onFailure(call: Call<com.simats.tmapp.api.VideoTokenResponse>, t: Throwable) {
                            Toast.makeText(this@VideoConsultationActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
            override fun onFailure(call: Call<com.simats.tmapp.api.ConsultationStatusResponse>, t: Throwable) {
                Toast.makeText(this@VideoConsultationActivity, "Safety check failed: ${t.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun startRecordingIfNeeded() {
        if (consultationId == -1) return
        apiService.startRecording(mapOf("consultation_id" to consultationId)).enqueue(object : Callback<com.simats.tmapp.api.GenericResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.GenericResponse>, response: Response<com.simats.tmapp.api.GenericResponse>) {
                if (response.isSuccessful) {
                    Log.d("VideoConsultation", "Recording started")
                }
            }
            override fun onFailure(call: Call<com.simats.tmapp.api.GenericResponse>, t: Throwable) {
                Log.e("VideoConsultation", "Failed to start recording: ${t.message}")
            }
        })
    }

    private var reportsDialog: com.google.android.material.bottomsheet.BottomSheetDialog? = null
    private var reportsAdapter: MedicalReportAdapter? = null

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

    private fun setupSocket() {
        val socket = SocketService.socket ?: return
        val doctorId = sessionManager.getUserId()
        socket.emit("join_room", "doctor_$doctorId")
        
        socket.on("medical_record_uploaded") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as org.json.JSONObject
                val eventPatientId = data.optInt("patient_id")
                if (eventPatientId == patientId) {
                    runOnUiThread {
                        Toast.makeText(this, "New medical record uploaded", Toast.LENGTH_SHORT).show()
                        if (reportsDialog?.isShowing == true) {
                            fetchReportsForBottomSheet()
                        }
                    }
                }
            }
        }

        socket.on("consultation_ended") {
            runOnUiThread {
                if (sessionManager.getUserRole().equals("patient", ignoreCase = true)) {
                    val intent = Intent(this@VideoConsultationActivity, PrescriptionWaitingActivity::class.java)
                    intent.putExtra("appointment_id", appointmentId)
                    intent.putExtra("doctor_id", this@VideoConsultationActivity.intent.getIntExtra("doctor_id", -1))
                    intent.putExtra("doctor_name", this@VideoConsultationActivity.intent.getStringExtra("doctor_name"))
                    intent.putExtra("doctor_specialization", this@VideoConsultationActivity.intent.getStringExtra("doctor_specialization"))
                    intent.putExtra("doctor_hospital", this@VideoConsultationActivity.intent.getStringExtra("doctor_hospital"))
                    intent.putExtra("doctor_photo", this@VideoConsultationActivity.intent.getStringExtra("doctor_photo"))
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun showReportsBottomSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        reportsDialog = dialog
        val view = layoutInflater.inflate(R.layout.dialog_medical_records_bottom_sheet, null)
        dialog.setContentView(view)

        val rv = view.findViewById<RecyclerView>(R.id.rvMedicalRecords)
        reportsAdapter = MedicalReportAdapter(
            sessionManager.getUserRole().lowercase(),
            onItemClick = { record -> downloadMedicalRecordFile(record.id, record.fileName) },
            onDownloadClick = { record -> downloadMedicalRecordFile(record.id, record.fileName) },
            onShareClick = { record -> downloadMedicalRecordFile(record.id, record.fileName) }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = reportsAdapter

        fetchReportsForBottomSheet()
        dialog.show()
    }

    private fun fetchReportsForBottomSheet() {
        val progressBar = reportsDialog?.findViewById<android.widget.ProgressBar>(R.id.progressBar)
        val tvEmpty = reportsDialog?.findViewById<android.widget.TextView>(R.id.tvEmpty)
        
        progressBar?.visibility = View.VISIBLE
        val doctorId = sessionManager.getUserId()
        apiService.getMedicalRecords(doctorId, "doctor", patientId)
            .enqueue(object : Callback<List<MedicalRecordResponse>> {
                override fun onResponse(call: Call<List<MedicalRecordResponse>>, response: Response<List<MedicalRecordResponse>>) {
                    progressBar?.visibility = View.GONE
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        reportsAdapter?.submitList(response.body())
                        tvEmpty?.visibility = View.GONE
                    } else {
                        tvEmpty?.visibility = View.VISIBLE
                    }
                }
                override fun onFailure(call: Call<List<MedicalRecordResponse>>, t: Throwable) {
                    progressBar?.visibility = View.GONE
                    tvEmpty?.visibility = View.VISIBLE
                }
            })
    }

    private fun downloadMedicalRecordFile(recordId: Int, fileName: String) {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()
        apiService.downloadMedicalRecord(recordId, userId, role).enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { saveAndOpenFile(it, fileName) }
                }
            }
            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {}
        })
    }

    private fun saveAndOpenFile(body: okhttp3.ResponseBody, fileName: String) {
        val file = java.io.File(getExternalFilesDir(null), fileName)
        body.byteStream().use { input ->
            java.io.FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, contentResolver.getType(uri) ?: "*/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }

    private val pickFileLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let { showUploadDialog(it) }
    }

    private fun openFilePicker() {
        pickFileLauncher.launch("*/*")
    }

    private fun showUploadDialog(uri: android.net.Uri) {
        val editText = android.widget.EditText(this)
        editText.hint = "Record type (e.g. Prescription)"
        
        android.widget.Toast.makeText(this, "Preparing to upload record...", android.widget.Toast.LENGTH_SHORT).show()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Upload Record")
            .setView(editText)
            .setPositiveButton("Upload") { _, _ ->
                val type = editText.text.toString().ifEmpty { "Prescription" }
                uploadMedicalRecord(uri, type)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadMedicalRecord(uri: android.net.Uri, type: String) {
        val doctorOrPatientId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()
        
        val file = getFileFromUri(uri) ?: return
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val requestFile = okhttp3.RequestBody.create(mimeType.toMediaTypeOrNull(), file)
        val body = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)
        
        val doctorId = sessionManager.getUserId()
        val userIdBody = doctorId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val roleBody = "doctor".toRequestBody("text/plain".toMediaTypeOrNull())
        val typeBody = type.toRequestBody("text/plain".toMediaTypeOrNull())
        val patientIdBody = patientId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val doctorIdBody = doctorId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val appointmentIdBody = appointmentId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        apiService.uploadMedicalRecord(userIdBody, roleBody, typeBody, patientIdBody, doctorIdBody, appointmentIdBody, body)
            .enqueue(object : Callback<com.simats.tmapp.api.GenericResponse> {
                override fun onResponse(call: Call<com.simats.tmapp.api.GenericResponse>, response: Response<com.simats.tmapp.api.GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@VideoConsultationActivity, "Report shared with patient", Toast.LENGTH_SHORT).show()
                        SocketService.socket?.emit("medical_record_uploaded", org.json.JSONObject().apply {
                            put("user_id", doctorId)
                            put("role", "doctor")
                            put("patient_id", patientId)
                            put("doctor_id", doctorId)
                            put("appointment_id", appointmentId)
                        })
                    } else {
                        Toast.makeText(this@VideoConsultationActivity, "Failed to share report", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<com.simats.tmapp.api.GenericResponse>, t: Throwable) {
                    Toast.makeText(this@VideoConsultationActivity, "Upload error: ${t.message}", Toast.LENGTH_SHORT).show()
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
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = cursor.getString(index)
            }
        }
        return name
    }

    private fun checkSelfPermission(): Boolean {
        for (permission in REQUESTED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun showEndConsultationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_end_consultation, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnContinueCall).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnEndCreatePrescription).setOnClickListener {
            dialog.dismiss()
            endConsultation(true)
        }

        dialog.show()
    }

    private fun endConsultation(shouldCreatePrescription: Boolean = false) {
        videoCallManager.leaveChannel()
        val request = com.simats.tmapp.api.ConsultationEndRequest(consultationId = consultationId)
        apiService.endConsultationV2(request).enqueue(object : Callback<com.simats.tmapp.api.GenericResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.GenericResponse>, response: Response<com.simats.tmapp.api.GenericResponse>) {
                if (shouldCreatePrescription) {
                    val intent = Intent(this@VideoConsultationActivity, CreatePrescriptionActivity::class.java)
                    intent.putExtra("appointment_id", appointmentId)
                    intent.putExtra("patient_id", patientId.toString())
                    intent.putExtra("patient_name", patientName)
                    intent.putExtra("patient_age", if (patientAge != -1) patientAge.toString() else "--")
                    intent.putExtra("patient_gender", patientGender ?: "--")
                    intent.putExtra("consultation_id", consultationId)
                    startActivity(intent)
                }
                finish()
            }
            override fun onFailure(call: Call<com.simats.tmapp.api.GenericResponse>, t: Throwable) {
                finish()
            }
        })
    }

    private fun setConsultationReady(consultationId: Int) {
        val request = com.simats.tmapp.api.ConsultationReadyRequest(consultationId = consultationId)
        apiService.setConsultationReady(request).enqueue(object : Callback<com.simats.tmapp.api.GenericResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.GenericResponse>, response: Response<com.simats.tmapp.api.GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@VideoConsultationActivity, "Consultation is Ready", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<com.simats.tmapp.api.GenericResponse>, t: Throwable) {
                Log.e("VideoConsultation", "Error setting ready: ${t.message}")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        videoCallManager.leaveChannel()
        handler.removeCallbacks(timerRunnable)
    }
}
