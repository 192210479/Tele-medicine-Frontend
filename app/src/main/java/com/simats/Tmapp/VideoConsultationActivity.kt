package com.simats.Tmapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
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
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.MedicalRecordResponse
import io.agora.rtc2.IRtcEngineEventHandler
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VideoConsultationActivity : AppCompatActivity() {

    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private var tvTimer: TextView? = null
    private var isMicOn = true
    private var isVideoOn = true
    private var isSpeakerOn = true
    private var startedAt: Long = 0L
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
    private lateinit var clTopBar: View
    private lateinit var llControls: View
    private var isControlsVisible = true
    private val hideControlsHandler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }
    
    // Safety flag to prevent excessive API calls
    private var isRemoteHandled = false

    // Real-time shared docs
    private var reportsDialog: com.google.android.material.bottomsheet.BottomSheetDialog? = null
    private var reportsAdapter: MedicalReportAdapter? = null
    private var currentReports: MutableList<MedicalRecordResponse> = mutableListOf()

    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.INTERNET
    )

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (startedAt > 0) {
                val serverStartSeconds = if (startedAt > 1000000000000L) startedAt / 1000 else startedAt
                val currentSeconds = System.currentTimeMillis() / 1000
                val elapsed = currentSeconds - serverStartSeconds
                seconds = if (elapsed > 0) elapsed.toInt() else 0
            } else {
                seconds++
            }
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            tvTimer?.text = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
            handler.postDelayed(this, 1000)
        }
    }

    private val videoCallListener = object : VideoCallManager.VideoCallListener {
        override fun onJoinChannelSuccess(channel: String, uid: Int) {
            runOnUiThread {
                Toast.makeText(applicationContext, "Connected to Consultation", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onRemoteUserJoined(uid: Int) {
            runOnUiThread {
                offlineHandler.removeCallbacks(offlineRunnable)
                
                // SAFE RENDERING: Clear first
                remoteVideoContainer.removeAllViews()
                videoCallManager.setupRemoteVideo(remoteVideoContainer, uid)

                // PREVENT API SPAM: Only poll once per join lifecycle
                if (!isRemoteHandled) {
                    isRemoteHandled = true
                    
                    apiService.pollConsultation(
                        appointmentId,
                        sessionManager.getUserId(),
                        sessionManager.getUserRole().lowercase()
                    ).enqueue(object : Callback<com.simats.Tmapp.api.ConsultationStatusResponse> {
                        override fun onResponse(
                            call: Call<com.simats.Tmapp.api.ConsultationStatusResponse>,
                            response: Response<com.simats.Tmapp.api.ConsultationStatusResponse>
                        ) {
                            val serverStart = response.body()?.startedAt
                            startedAt = if (serverStart != null && serverStart > 0) serverStart else 0L
                            
                            // CLEAN TIMER START
                            handler.removeCallbacks(timerRunnable)
                            handler.post(timerRunnable)
                        }

                        override fun onFailure(
                            call: Call<com.simats.Tmapp.api.ConsultationStatusResponse>,
                            t: Throwable
                        ) {
                            startedAt = 0L
                            handler.removeCallbacks(timerRunnable)
                            handler.post(timerRunnable)
                        }
                    })
                }
            }
        }

        override fun onRemoteUserOffline(uid: Int) {
            runOnUiThread {
                remoteVideoContainer.removeAllViews()
                Toast.makeText(applicationContext, "Participant connection lost", Toast.LENGTH_LONG).show()
                offlineHandler.postDelayed(offlineRunnable, 20000)
                // RESET FLAG
                isRemoteHandled = false
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
        Toast.makeText(this, "Participant connection lost for too long.", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_consultation)

        sessionManager = SessionManager.getInstance(this)
        videoCallManager = VideoCallManager.getInstance(this)

        patientId = intent.getIntExtra("patient_id", -1)
        appointmentId = intent.getIntExtra("appointment_id", -1)
        consultationId = intent.getIntExtra("consultation_id", -1)
        patientName = intent.getStringExtra("patient_name")
        patientAge = intent.getIntExtra("patient_age", -1)
        patientGender = intent.getStringExtra("patient_gender")

        displayPatientInfo(patientName, patientAge, patientGender)

        val doctorName = intent.getStringExtra("doctor_name")
        val specialization = intent.getStringExtra("doctor_specialization")
        channelName = intent.getStringExtra("channel_name")
            ?: intent.getStringExtra("VIDEO_ROOM")
            ?: intent.getStringExtra("session_id")

        if (appointmentId == -1 || channelName.isNullOrEmpty()) {
            Toast.makeText(this, "Consultation room not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (sessionManager.getUserRole().equals("doctor", ignoreCase = true) && consultationId != -1) {
            setConsultationReady(consultationId)
        }

        val docName = if (doctorName.isNullOrEmpty() || doctorName == "null") {
            "Dr. ${sessionManager.getUserName()}"
        } else {
            "Dr. $doctorName"
        }

        findViewById<TextView>(R.id.tvDoctorName)?.text = docName
        findViewById<TextView>(R.id.tvSpeciality)?.text = specialization ?: ""

        val ivPatientAvatar = findViewById<ImageView>(R.id.ivPatientAvatar)
        if (ivPatientAvatar != null) {
            val patientIdFromIntent = intent.getIntExtra("patient_id", -1)
            val patientPhoto = intent.getStringExtra("patient_photo")
            val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
            val photoUrl = if (!patientPhoto.isNullOrEmpty()) {
                patientPhoto
            } else {
                "$baseUrl/api/profile/image/$patientIdFromIntent?role=patient"
            }

            com.bumptech.glide.Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.bg_circle_soft_blue)
                .circleCrop()
                .into(ivPatientAvatar)
        }

        tvTimer = findViewById(R.id.tvTimer)
        localVideoContainer = findViewById(R.id.localVideoContainer)
        remoteVideoContainer = findViewById(R.id.remoteVideoContainer)
        clTopBar = findViewById(R.id.clTopBar)
        llControls = findViewById(R.id.llControls)

        remoteVideoContainer.setOnClickListener {
            if (isControlsVisible) hideControls() else showControls()
        }

        showControls()

        if (checkSelfPermission()) {
            initAgoraAndJoin()
        } else {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        }

        findViewById<FloatingActionButton>(R.id.fabMic).setOnClickListener {
            resetHideTimer()
            isMicOn = !isMicOn
            val fab = it as FloatingActionButton
            fab.setImageResource(if (isMicOn) R.drawable.ic_mic else R.drawable.ic_mic_off)
            fab.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (isMicOn) ContextCompat.getColor(this, R.color.white_dim)
                else ContextCompat.getColor(this, R.color.primary_red)
            )
            videoCallManager.setMicEnabled(isMicOn)
        }

        findViewById<FloatingActionButton>(R.id.fabVideo).setOnClickListener {
            resetHideTimer()
            isVideoOn = !isVideoOn
            val fab = it as FloatingActionButton
            fab.setImageResource(if (isVideoOn) R.drawable.ic_video_white else R.drawable.ic_video_off)
            fab.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (isVideoOn) ContextCompat.getColor(this, R.color.white_dim)
                else ContextCompat.getColor(this, R.color.primary_red)
            )

            videoCallManager.setVideoEnabled(isVideoOn)
            findViewById<View>(R.id.localVideoContainer)?.visibility = if (isVideoOn) View.VISIBLE else View.INVISIBLE
            findViewById<ImageView>(R.id.ivLocalPlaceholder)?.visibility = if (isVideoOn) View.GONE else View.VISIBLE
        }

        findViewById<FloatingActionButton>(R.id.fabSpeaker).setOnClickListener {
            resetHideTimer()
            isSpeakerOn = !isSpeakerOn
            val fab = it as FloatingActionButton
            fab.setImageResource(if (isSpeakerOn) R.drawable.ic_speaker else R.drawable.ic_speaker_off)
            fab.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (isSpeakerOn) ContextCompat.getColor(this, R.color.primary_blue)
                else ContextCompat.getColor(this, R.color.white_dim)
            )
            videoCallManager.getRtcEngine()?.setEnableSpeakerphone(isSpeakerOn)
        }

        findViewById<FloatingActionButton>(R.id.fabEndCall).setOnClickListener {
            resetHideTimer()
            showEndConsultationDialog()
        }

        findViewById<FloatingActionButton>(R.id.fabSwitchCamera)?.setOnClickListener {
            resetHideTimer()
            videoCallManager.switchCamera()
            Toast.makeText(this, "Switching Camera", Toast.LENGTH_SHORT).show()
        }

        findViewById<FloatingActionButton>(R.id.fabChat).setOnClickListener {
            resetHideTimer()
            val chatIntent = Intent(this, ChatActivity::class.java)
            chatIntent.putExtra("appointment_id", appointmentId)
            chatIntent.putExtra("consultation_id", consultationId)
            startActivity(chatIntent)
        }

        findViewById<FloatingActionButton>(R.id.fabUpload).setOnClickListener {
            resetHideTimer()
            val uploadIntent = Intent(this, UploadReportActivity::class.java)
            uploadIntent.putExtra("patient_id", patientId)
            uploadIntent.putExtra("appointment_id", appointmentId)
            uploadIntent.putExtra(
                "doctor_id",
                if (sessionManager.getUserRole().equals("doctor", ignoreCase = true)) {
                    sessionManager.getUserId()
                } else {
                    intent.getIntExtra("doctor_id", -1)
                }
            )
            startActivity(uploadIntent)
        }

        findViewById<View>(R.id.cvPatientInfo)?.setOnClickListener {
            resetHideTimer()
            val patientIntent = Intent(this, PatientDataActivity::class.java)
            patientIntent.putExtra("patient_id", patientId)
            patientIntent.putExtra("appointment_id", appointmentId)
            patientIntent.putExtra("consultation_id", consultationId)
            startActivity(patientIntent)
        }

        setupDraggableVideo()
        setupSocket()
    }

    private fun initAgoraAndJoin() {
        apiService.pollConsultation(
            appointmentId,
            sessionManager.getUserId(),
            sessionManager.getUserRole().lowercase()
        ).enqueue(object : Callback<com.simats.Tmapp.api.ConsultationStatusResponse> {
            override fun onResponse(
                call: Call<com.simats.Tmapp.api.ConsultationStatusResponse>,
                response: Response<com.simats.Tmapp.api.ConsultationStatusResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val statusResponse = response.body()!!
                    val status = statusResponse.status
                    startedAt = statusResponse.startedAt ?: 0L

                    if (status.equals("completed", ignoreCase = true)) {
                        Toast.makeText(this@VideoConsultationActivity, "Consultation already finished", Toast.LENGTH_LONG).show()
                        finish()
                        return
                    }

                    videoCallManager.initAgoraEngine(videoCallListener)

                    val userId = sessionManager.getUserId()
                    apiService.getVideoToken(
                        userId,
                        channelName!!,
                        sessionManager.getUserRole().lowercase()
                    ).enqueue(object : Callback<com.simats.Tmapp.api.VideoTokenResponse> {
                        override fun onResponse(
                            call: Call<com.simats.Tmapp.api.VideoTokenResponse>,
                            response: Response<com.simats.Tmapp.api.VideoTokenResponse>
                        ) {
                            if (response.isSuccessful && response.body() != null) {
                                val body = response.body()!!
                                if (body.startedAt != null) {
                                    startedAt = body.startedAt!!
                                }
                                Log.d("Consultation", "TOKEN RECEIVED: ${body.token}")
                                Log.d("Consultation", "JOINING CHANNEL: ${body.channel}")
                                
                                // FIX JOIN ORDER: Surface MUST be attached BEFORE joining
                                videoCallManager.setupLocalVideo(localVideoContainer, body.uid)
                                videoCallManager.startPreview()
                                videoCallManager.joinChannel(body.token, body.channel, body.uid)
                                
                                startRecordingIfNeeded()
                            } else {
                                Toast.makeText(this@VideoConsultationActivity, "Failed to get video token", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(
                            call: Call<com.simats.Tmapp.api.VideoTokenResponse>,
                            t: Throwable
                        ) {
                            Toast.makeText(this@VideoConsultationActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onFailure(
                call: Call<com.simats.Tmapp.api.ConsultationStatusResponse>,
                t: Throwable
            ) {
                Toast.makeText(this@VideoConsultationActivity, "Safety check failed: ${t.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun startRecordingIfNeeded() {
        if (consultationId == -1) return
        apiService.startRecording(mapOf("consultation_id" to consultationId))
            .enqueue(object : Callback<com.simats.Tmapp.api.GenericResponse> {
                override fun onResponse(
                    call: Call<com.simats.Tmapp.api.GenericResponse>,
                    response: Response<com.simats.Tmapp.api.GenericResponse>
                ) {
                    if (response.isSuccessful) {
                        Log.d("VideoConsultation", "Recording started")
                    }
                }

                override fun onFailure(
                    call: Call<com.simats.Tmapp.api.GenericResponse>,
                    t: Throwable
                ) {
                    Log.e("VideoConsultation", "Failed to start recording: ${t.message}")
                }
            })
    }

    private fun setupDraggableVideo() {
        val cvLocalVideo = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cvLocalVideo) ?: return
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
        val userId = sessionManager.getUserId()

        val personalRoom = org.json.JSONObject()
        personalRoom.put("room", "${sessionManager.getUserRole().lowercase()}_$userId")
        socket.emit("join_room", personalRoom)

        if (appointmentId != -1) {
            val consultationRoom = org.json.JSONObject()
            consultationRoom.put("room", "consultation_$appointmentId")
            socket.emit("join_room", consultationRoom)
        }

        socket.off("medical_record_uploaded")
        socket.on("medical_record_uploaded") { args ->
            if (args.isNotEmpty()) {
                try {
                    val data = args[0] as org.json.JSONObject
                    val eventAppointmentId = data.optInt("appointment_id", -1)

                    if (eventAppointmentId == appointmentId) {
                        runOnUiThread {
                            Toast.makeText(this, "New document shared", Toast.LENGTH_SHORT).show()
                            fetchAndShowReports()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("VideoConsultation", "Socket medical_record_uploaded parse error: ${e.message}")
                }
            }
        }

        socket.off("consultation_ended")
        socket.on("consultation_ended") {
            runOnUiThread {
                if (sessionManager.getUserRole().equals("patient", ignoreCase = true)) {
                    val prescriptionIntent = Intent(this@VideoConsultationActivity, PrescriptionWaitingActivity::class.java)
                    prescriptionIntent.putExtra("appointment_id", appointmentId)
                    prescriptionIntent.putExtra("doctor_id", this@VideoConsultationActivity.intent.getIntExtra("doctor_id", -1))
                    prescriptionIntent.putExtra("doctor_name", this@VideoConsultationActivity.intent.getStringExtra("doctor_name"))
                    prescriptionIntent.putExtra("doctor_specialization", this@VideoConsultationActivity.intent.getStringExtra("doctor_specialization"))
                    prescriptionIntent.putExtra("doctor_hospital", this@VideoConsultationActivity.intent.getStringExtra("doctor_hospital"))
                    prescriptionIntent.putExtra("doctor_photo", this@VideoConsultationActivity.intent.getStringExtra("doctor_photo"))
                    startActivity(prescriptionIntent)
                    finish()
                }
            }
        }

        socket.off("call_started")
        socket.on("call_started") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as org.json.JSONObject
                if (data.optInt("appointment_id") == appointmentId) {
                    val startTime = data.optLong("start_time", 0L)
                    if (startTime > 0) {
                        runOnUiThread {
                            startedAt = startTime
                        }
                    }
                }
            }
        }
    }

    private fun fetchAndShowReports() {
        apiService.getMedicalRecords(
            userId = sessionManager.getUserId(),
            role = sessionManager.getUserRole().lowercase(),
            appointmentId = appointmentId,
            patientId = if (sessionManager.getUserRole().equals("doctor", ignoreCase = true)) patientId else null
        ).enqueue(object : Callback<List<MedicalRecordResponse>> {
            override fun onResponse(
                call: Call<List<MedicalRecordResponse>>,
                response: Response<List<MedicalRecordResponse>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    currentReports = response.body()!!.toMutableList()
                    if (currentReports.isNotEmpty()) {
                        showReportsBottomSheet(currentReports)
                    }
                } else {
                    Log.e("VideoConsultation", "Failed to fetch reports: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<MedicalRecordResponse>>, t: Throwable) {
                Log.e("VideoConsultation", "fetchAndShowReports failed: ${t.message}")
            }
        })
    }

    private fun showReportsBottomSheet(reports: List<MedicalRecordResponse>) {
        if (reportsDialog == null) {
            reportsDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.activity_medical_records, null)

            val recyclerView = view.findViewById<RecyclerView>(R.id.rvMedicalRecords)
            recyclerView.layoutManager = LinearLayoutManager(this)

            reportsAdapter = MedicalReportAdapter(
                currentRole = sessionManager.getUserRole().lowercase(),
                isSharingMode = true,
                onItemClick = { record -> downloadAndOpenRecord(record) },
                onDownloadClick = { record -> downloadAndOpenRecord(record) },
                onShareClick = { }
            )

            recyclerView.adapter = reportsAdapter

            view.findViewById<View?>(R.id.btnUploadTop)?.visibility = View.GONE
            view.findViewById<View?>(R.id.btnUpload)?.visibility = View.GONE
            view.findViewById<View?>(R.id.btnRequestRecord)?.visibility = View.GONE
            view.findViewById<View?>(R.id.ivBack)?.visibility = View.GONE
            view.findViewById<View?>(R.id.progressBar)?.visibility = View.GONE
            view.findViewById<View?>(R.id.llEmptyState)?.visibility =
                if (reports.isEmpty()) View.VISIBLE else View.GONE

            reportsDialog?.setContentView(view)
        }

        reportsAdapter?.submitList(reports.toList())
        reportsDialog?.show()
    }

    private fun downloadAndOpenRecord(record: MedicalRecordResponse) {
        apiService.downloadMedicalRecord(
            record.id,
            sessionManager.getUserId(),
            sessionManager.getUserRole().lowercase()
        ).enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(
                call: Call<okhttp3.ResponseBody>,
                response: Response<okhttp3.ResponseBody>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    saveAndOpenDownloadedFile(response.body()!!, record.fileName)
                } else {
                    Toast.makeText(this@VideoConsultationActivity, "Failed to open document", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                Toast.makeText(this@VideoConsultationActivity, "Download error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveAndOpenDownloadedFile(body: okhttp3.ResponseBody, fileName: String) {
        try {
            val file = java.io.File(cacheDir, fileName)
            val inputStream = body.byteStream()
            val outputStream = java.io.FileOutputStream(file)
            val buffer = ByteArray(4096)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                file
            )

            val extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(file.path)
            val mimeType = if (extension.isNotEmpty()) {
                android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            } else {
                contentResolver.getType(uri)
            } ?: "*/*"

            val openIntent = Intent(Intent.ACTION_VIEW)
            openIntent.setDataAndType(uri, mimeType)
            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(openIntent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkSelfPermission(): Boolean {
        for (permission in REQUESTED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (checkSelfPermission()) {
                initAgoraAndJoin()
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
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
        val request = com.simats.Tmapp.api.ConsultationEndRequest(consultationId = consultationId)
        apiService.endConsultationV2(request).enqueue(object : Callback<com.simats.Tmapp.api.GenericResponse> {
            override fun onResponse(
                call: Call<com.simats.Tmapp.api.GenericResponse>,
                response: Response<com.simats.Tmapp.api.GenericResponse>
            ) {
                if (shouldCreatePrescription) {
                    val prescriptionIntent = Intent(this@VideoConsultationActivity, CreatePrescriptionActivity::class.java)
                    prescriptionIntent.putExtra("appointment_id", appointmentId)
                    prescriptionIntent.putExtra("patient_id", patientId.toString())
                    prescriptionIntent.putExtra("patient_name", patientName)
                    prescriptionIntent.putExtra("patient_age", if (patientAge != -1) patientAge.toString() else "--")
                    prescriptionIntent.putExtra("patient_gender", patientGender ?: "--")
                    prescriptionIntent.putExtra("consultation_id", consultationId)
                    startActivity(prescriptionIntent)
                }
                finish()
            }

            override fun onFailure(
                call: Call<com.simats.Tmapp.api.GenericResponse>,
                t: Throwable
            ) {
                finish()
            }
        })
    }

    private fun setConsultationReady(consultationId: Int) {
        val request = com.simats.Tmapp.api.ConsultationReadyRequest(consultationId = consultationId)
        apiService.setConsultationReadyV2(request).enqueue(object : Callback<com.simats.Tmapp.api.GenericResponse> {
            override fun onResponse(
                call: Call<com.simats.Tmapp.api.GenericResponse>,
                response: Response<com.simats.Tmapp.api.GenericResponse>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(this@VideoConsultationActivity, "Consultation is Ready", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(
                call: Call<com.simats.Tmapp.api.GenericResponse>,
                t: Throwable
            ) {
                Log.e("VideoConsultation", "Error setting ready: ${t.message}")
            }
        })
    }

    private fun displayPatientInfo(name: String?, age: Int, gender: String?) {
        findViewById<TextView>(R.id.tvPatientName)?.text = name ?: "Patient"
        val info = mutableListOf<String>()
        if (age > 0) info.add("$age yrs")
        if (!gender.isNullOrEmpty() && gender != "null") info.add(gender)
        findViewById<TextView>(R.id.tvPatientDetails)?.text = if (info.isEmpty()) "--" else info.joinToString(" • ")
        findViewById<View>(R.id.llSymptoms)?.visibility = View.GONE
    }

    private fun showControls() {
        isControlsVisible = true
        clTopBar.animate().alpha(1f).setDuration(300).withStartAction { clTopBar.visibility = View.VISIBLE }
        llControls.animate().alpha(1f).setDuration(300).withStartAction { llControls.visibility = View.VISIBLE }
        resetHideTimer()
    }

    private fun hideControls() {
        isControlsVisible = false
        clTopBar.animate().alpha(0f).setDuration(300).withEndAction { clTopBar.visibility = View.GONE }
        llControls.animate().alpha(0f).setDuration(300).withEndAction { llControls.visibility = View.GONE }
        hideControlsHandler.removeCallbacks(hideControlsRunnable)
    }

    private fun resetHideTimer() {
        hideControlsHandler.removeCallbacks(hideControlsRunnable)
        hideControlsHandler.postDelayed(hideControlsRunnable, 4000)
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketService.socket?.off("medical_record_uploaded")
        SocketService.socket?.off("consultation_ended")
        SocketService.socket?.off("call_started")
        reportsDialog?.dismiss()
        videoCallManager.leaveChannel()
        handler.removeCallbacks(timerRunnable)
        hideControlsHandler.removeCallbacks(hideControlsRunnable)
    }
}