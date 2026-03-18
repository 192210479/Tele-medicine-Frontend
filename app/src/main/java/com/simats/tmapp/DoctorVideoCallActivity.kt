package com.simats.tmapp

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
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.simats.tmapp.api.ApiClient
import io.agora.rtc2.*
import io.agora.rtc2.Constants
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorVideoCallActivity : AppCompatActivity() {
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
            Log.d("VIDEO_DEBUG","Joined channel successfully: $channel")
            runOnUiThread { 
                Toast.makeText(applicationContext, "Connected to Consultation", Toast.LENGTH_SHORT).show()
                handler.postDelayed(timerRunnable, 1000)
            }
        }

        override fun onRemoteUserJoined(uid: Int) {
            runOnUiThread { videoCallManager.setupRemoteVideo(remoteVideoContainer, uid) }
        }

        override fun onRemoteUserOffline(uid: Int) {
            runOnUiThread { remoteVideoContainer.removeAllViews() }
        }

        override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
            runOnUiThread {
                val quality = if (uid == 0) txQuality else rxQuality
                val ivSignal = findViewById<ImageView>(R.id.ivSignal)
                if (ivSignal != null) {
                    when (quality) {
                        Constants.QUALITY_EXCELLENT, Constants.QUALITY_GOOD -> ivSignal.setColorFilter(ContextCompat.getColor(this@DoctorVideoCallActivity, R.color.primary_green))
                        Constants.QUALITY_POOR, Constants.QUALITY_BAD -> ivSignal.setColorFilter(ContextCompat.getColor(this@DoctorVideoCallActivity, R.color.primary_red))
                        else -> ivSignal.setColorFilter(ContextCompat.getColor(this@DoctorVideoCallActivity, R.color.primary_orange))
                    }
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
             runOnUiThread {
                when (state) {
                    Constants.CONNECTION_STATE_RECONNECTING -> {
                        findViewById<ImageView>(R.id.ivSignal)?.setColorFilter(ContextCompat.getColor(this@DoctorVideoCallActivity, R.color.primary_orange))
                    }
                }
            }
        }

        override fun onError(err: Int) {
            Log.e("DoctorVideoCall", "Agora Error: $err")
            if (err == -100) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Reconnection failed. Ending call.", Toast.LENGTH_LONG).show()
                    this@DoctorVideoCallActivity.endConsultation()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_video_call)
        
        sessionManager = SessionManager.getInstance(this)

        appointmentId = intent.getIntExtra("appointment_id", -1)
        channelName = intent.getStringExtra("VIDEO_ROOM")
        val doctorName = intent.getStringExtra("doctor_name")
        val specialization = intent.getStringExtra("doctor_specialization")

        if (appointmentId == -1 || channelName.isNullOrEmpty()) {
            Toast.makeText(this, "Consultation room not available", Toast.LENGTH_SHORT).show()
            finish()
            return
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

        val notificationId = intent.getIntExtra("notification_id", -1)
        if (notificationId != -1) {
            markNotificationAsRead(notificationId)
        }

        tvTimer = findViewById(R.id.tvTimer)
        localVideoContainer = findViewById(R.id.localVideoContainer)
        remoteVideoContainer = findViewById(R.id.remoteVideoContainer)

        if (checkSelfPermission()) {
            initAgoraAndJoin()
        } else {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        }

        videoCallManager = VideoCallManager.getInstance(this)

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

        findViewById<FloatingActionButton>(R.id.fabChat).setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("appointment_id", appointmentId)
            startActivity(intent)
        }

        findViewById<FloatingActionButton>(R.id.fabPatientData).setOnClickListener {
            val intent = Intent(this, PatientDataActivity::class.java)
            intent.putExtra("patient_id", patientId)
            intent.putExtra("appointment_id", appointmentId)
            startActivity(intent)
        }

        findViewById<View>(R.id.cvUploadReport)?.setOnClickListener {
            val intent = Intent(this, MedicalRecordsActivity::class.java)
            intent.putExtra("patient_id", patientId)
            intent.putExtra("appointment_id", appointmentId)
            startActivity(intent)
        }
        
        findViewById<FloatingActionButton>(R.id.fabUpload).setOnClickListener {
             openFilePicker()
        }
    }

    private val filePickerLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            uri?.let { uploadFile(it) }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
        filePickerLauncher.launch(intent)
    }

    private fun uploadFile(uri: android.net.Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return
            
            val mediaType = (contentResolver.getType(uri) ?: "*/*").toMediaTypeOrNull()
            val requestFile = bytes.toRequestBody(mediaType)
            val filePart = okhttp3.MultipartBody.Part.createFormData("file", "report_${System.currentTimeMillis()}", requestFile)
            
            val userIdBody = sessionManager.getUserId().toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val roleBody = "doctor".toRequestBody("text/plain".toMediaTypeOrNull())
            val recordTypeBody = "Consultation Report".toRequestBody("text/plain".toMediaTypeOrNull())
            val appointmentIdBody = appointmentId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val patientIdBody = patientId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            apiService.uploadMedicalRecord(
                userId = userIdBody,
                role = roleBody,
                recordType = recordTypeBody,
                patientId = patientIdBody,
                appointmentId = appointmentIdBody,
                file = filePart
            ).enqueue(object : Callback<com.simats.tmapp.api.GenericResponse> {
                override fun onResponse(call: Call<com.simats.tmapp.api.GenericResponse>, response: Response<com.simats.tmapp.api.GenericResponse>) {
                    if (response.isSuccessful) Toast.makeText(this@DoctorVideoCallActivity, "Upload Successful", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this@DoctorVideoCallActivity, "Upload Failed", Toast.LENGTH_SHORT).show()
                }
                override fun onFailure(call: Call<com.simats.tmapp.api.GenericResponse>, t: Throwable) {
                    Toast.makeText(this@DoctorVideoCallActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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

    private fun initAgoraAndJoin() {
        videoCallManager = VideoCallManager.getInstance(this)
        videoCallManager.initAgoraEngine(videoCallListener)
        
        val userId = sessionManager.getUserId()
        apiService.getVideoToken(userId, channelName!!, "doctor").enqueue(object : Callback<com.simats.tmapp.api.VideoTokenResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.VideoTokenResponse>, response: Response<com.simats.tmapp.api.VideoTokenResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    videoCallManager.joinChannel(body.token, body.channel, body.uid)
                    videoCallManager.setupLocalVideo(localVideoContainer, body.uid)
                } else {
                    Toast.makeText(this@DoctorVideoCallActivity, "Failed to get video token", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<com.simats.tmapp.api.VideoTokenResponse>, t: Throwable) {
                Toast.makeText(this@DoctorVideoCallActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
        leaveChannel()
        val body = mapOf(
            "user_id" to sessionManager.getUserId(),
            "role" to sessionManager.getUserRole().lowercase()
        )

        apiService.endConsultation(appointmentId, body).enqueue(object : Callback<com.simats.tmapp.api.GenericResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.GenericResponse>, response: Response<com.simats.tmapp.api.GenericResponse>) {
                if (shouldCreatePrescription) {
                    val intent = Intent(this@DoctorVideoCallActivity, NewPrescriptionActivity::class.java)
                    intent.putExtra("appointment_id", appointmentId)
                    intent.putExtra("patient_id", patientId)
                    intent.putExtra("patient_name", patientName)
                    intent.putExtra("patient_age", patientAge)
                    intent.putExtra("patient_gender", patientGender)
                    startActivity(intent)
                    finish()
                } else {
                    navigateBack()
                }
            }

            override fun onFailure(call: Call<com.simats.tmapp.api.GenericResponse>, t: Throwable) {
                if (shouldCreatePrescription) {
                    val intent = Intent(this@DoctorVideoCallActivity, NewPrescriptionActivity::class.java)
                    intent.putExtra("appointment_id", appointmentId)
                    intent.putExtra("patient_id", patientId)
                    intent.putExtra("patient_name", patientName)
                    intent.putExtra("patient_age", patientAge)
                    intent.putExtra("patient_gender", patientGender)
                    startActivity(intent)
                    finish()
                } else {
                    navigateBack()
                }
            }
        })
    }

    private fun navigateBack() {
        finish()
    }

    private fun leaveChannel() {
        if (::videoCallManager.isInitialized) {
            videoCallManager.leaveChannel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        leaveChannel()
        handler.removeCallbacks(timerRunnable)
    }

    private fun markNotificationAsRead(notifId: Int) {
        ApiClient.instance.markNotificationRead(notifId).enqueue(object : Callback<com.simats.tmapp.api.ApiResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.ApiResponse>, response: Response<com.simats.tmapp.api.ApiResponse>) {
                // Success
            }
            override fun onFailure(call: Call<com.simats.tmapp.api.ApiResponse>, t: Throwable) {}
        })
    }
}
