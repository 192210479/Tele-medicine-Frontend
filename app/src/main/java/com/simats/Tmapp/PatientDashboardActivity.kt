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
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.simats.Tmapp.api.NotificationResponse
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.MedicationReminderResponse
import com.simats.Tmapp.api.GenericResponse
import com.simats.Tmapp.api.HealthTipResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import org.json.JSONObject
import java.util.Calendar
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale

class PatientDashboardActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private var tvUserName: TextView? = null
    private var tvGreeting: TextView? = null
    private var tvHealthTip: TextView? = null
    private var ivAvatar: ImageView? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var viewModel: NotificationViewModel
    private val apiService = ApiClient.instance
    private val socket = SocketService.socket!!
    
    private val notificationReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "NEW_NOTIFICATION_RECEIVED") {
                viewModel.loadNotifications()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_dashboard)
        
        sessionManager = SessionManager.getInstance(this)
        viewModel = androidx.lifecycle.ViewModelProvider(this)[NotificationViewModel::class.java]
        
        // Role Verification
        val currentRole = sessionManager.getUserRole().lowercase()
        if (!currentRole.contains("patient")) {
            val intent = Intent(this, sessionManager.getDashboardActivity())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        tvUserName = findViewById(R.id.tvUserName)
        tvGreeting = findViewById(R.id.tvGreeting)
        tvHealthTip = findViewById(R.id.tvHealthTip)
        ivAvatar = findViewById(R.id.ivAvatar)

        // Initial UI sync
        syncProfileUI()

        val cvEmergency = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cvEmergency)
        val itemBook = findViewById<com.google.android.material.card.MaterialCardView>(R.id.itemBook)

        // Emergency links
        val navigateToEmergency = {
            val intent = Intent(this, EmergencyHelpActivity::class.java)
            startActivity(intent)
        }
        cvEmergency?.setOnClickListener { navigateToEmergency() }

        // Book links
        val navigateToSelectDoctor = {
            val intent = Intent(this, SelectDoctorActivity::class.java)
            startActivity(intent)
        }
        itemBook?.setOnClickListener { navigateToSelectDoctor() }
        
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.itemPrescriptions)?.setOnClickListener {
            startActivity(Intent(this, PrescriptionListActivity::class.java))
        }

        findViewById<com.google.android.material.card.MaterialCardView>(R.id.itemReminders)?.setOnClickListener {
            startActivity(Intent(this, MedicationRemindersActivity::class.java))
        }

        val itemMedicalRecords = findViewById<com.google.android.material.card.MaterialCardView>(R.id.itemMedicalRecords)

        val navigateToMedicalRecords = {
            startActivity(Intent(this, MedicalRecordsActivity::class.java))
        }

        itemMedicalRecords?.setOnClickListener { navigateToMedicalRecords() }

        findViewById<android.widget.FrameLayout>(R.id.flNotifications)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        // Upcoming links
        val navigateToUpcomingAppointments = {
            startActivity(Intent(this, UpcomingAppointmentsActivity::class.java))
        }
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.itemUpcoming)?.setOnClickListener { navigateToUpcomingAppointments() }

        // Dashboard grid connections
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.itemHistory)?.setOnClickListener {
            startActivity(Intent(this, ConsultationHistoryActivity::class.java))
        }

        // Profile Avatar click
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cvAvatar)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Observe Notifications
        viewModel.unreadCount.observe(this) { count ->
            val badge = findViewById<View>(R.id.vNotificationBadge)
            badge?.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
        
        viewModel.notifications.observe(this) { list ->
            // Optionally show toast for new notifications here if desired
            if (!list.isNullOrEmpty()) {
                val latest = list[0]
                if (!latest.isRead && (latest.type == "Consultation" || latest.type == "Prescription")) {
                    showPushNotification(latest)
                }
            }
        }

        setupSocketListeners()
        
        val userId = sessionManager.getUserId()
        if (userId != -1) {
            socket.emit("join_room", "patient_$userId")
            apiService.cleanupSlots(mapOf("user_id" to userId, "role" to "patient")).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {}
                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
            })
        }

        if (!socket.connected()) {
            socket.connect()
        }
        
        startReminderTimer()
        loadDashboardGreeting()
        loadDashboardData()
        loadHealthTip()
    }
    
    private var reminderRunnable: Runnable? = null
    
    private fun startReminderTimer() {
        reminderRunnable = object : Runnable {
            override fun run() {
                checkReminders()
                handler.postDelayed(this, 30000) // 30 seconds
            }
        }
        handler.postDelayed(reminderRunnable!!, 1000)
    }
    
    // Store already shown reminders for this minute to prevent duplicate dialogs
    private val shownReminders = mutableSetOf<Int>()
    private var lastCheckedMinute = -1
    
    private fun checkReminders() {
        val userId = sessionManager.getUserId()
        if (userId == -1) return
        
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeStr = String.format(Locale.getDefault(), "%02d:%02d", currentHour, currentMinute)
        
        if (currentMinute != lastCheckedMinute) {
            shownReminders.clear()
            lastCheckedMinute = currentMinute
        }

        apiService.getMedicationReminders(userId).enqueue(object : Callback<List<MedicationReminderResponse>> {
            override fun onResponse(call: Call<List<MedicationReminderResponse>>, response: Response<List<MedicationReminderResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    val reminders = response.body()!!
                    for (reminder in reminders) {
                        if (reminder.status == "Active" && reminder.reminderTime == currentTimeStr) {
                            if (!shownReminders.contains(reminder.id)) {
                                shownReminders.add(reminder.id)
                                showReminderPopup(reminder)
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<List<MedicationReminderResponse>>, t: Throwable) {
                // Silently ignore background failures
            }
        })
    }
    
    private fun showReminderPopup(reminder: MedicationReminderResponse) {
        val parts = reminder.medicineName.split("|")
        val name = parts[0]
        
        // Show Local Notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "medication_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Medication Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(this, PatientDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, reminder.id, intent, PendingIntent.FLAG_IMMUTABLE)
        
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Time to take your medicine")
            .setContentText("Medicine: $name")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            
        notificationManager.notify(reminder.id, builder.build())
        
        // Show Popup Alert Dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_logout) // Reusing existing dialog layout but changing texts
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancel)
        val btnConfirm = dialog.findViewById<MaterialButton>(R.id.btnLogoutConfirm)
        val ivClose = dialog.findViewById<ImageView>(R.id.ivClose)

        // dialog_logout.xml structure: CardView -> LinearLayout -> [RelativeLayout, TextView, LinearLayout]
        // RelativeLayout -> [TextView (Title), ImageView (Close)]
        
        try {
            val contentLinear = dialog.findViewById<LinearLayout>(ivClose?.parent?.parent?.let { it as? ViewGroup }?.id ?: -1) 
                ?: (ivClose?.parent?.parent as? LinearLayout)
            
            val headerRelative = ivClose?.parent as? RelativeLayout
            val tvTitle = headerRelative?.getChildAt(0) as? TextView
            val tvMessage = contentLinear?.getChildAt(1) as? TextView

            tvTitle?.text = "Time for Medicine"
            tvMessage?.text = "Medicine: $name"
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        btnCancel?.text = "Skip"
        btnConfirm?.text = "Mark Taken"

        btnCancel?.setOnClickListener { 
            dialog.dismiss() 
        }
        ivClose?.setOnClickListener { dialog.dismiss() }

        btnConfirm?.setOnClickListener {
            dialog.dismiss()
            apiService.completeMedicationReminder(reminder.id).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@PatientDashboardActivity, "Marked as taken", Toast.LENGTH_SHORT).show()
                        // Optional: broadcast to update lists if open
                        sendBroadcast(Intent("com.simats.tmapp.REFRESH_REMINDERS"))
                    }
                }
                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
            })
        }
        if (!isFinishing) dialog.show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        reminderRunnable?.let { handler.removeCallbacks(it) }
        socket.off("consultation_ready")
        socket.off("new_appointment")
        socket.off("appointment_updated")
        socket.off("appointment_reassigned")
        socket.off("appointment_cancelled")
        socket.off("consultation_started")
        socket.off("consultation_ended")
    }

    private fun setupSocketListeners() {
        val socket = SocketService.socket ?: return

        val refreshAction = {
            runOnUiThread { refreshDashboardData() }
        }

        socket.on("consultation_ready") {
            refreshAction()
        }

        socket.on("new_appointment") { refreshAction() }
        socket.on("appointment_updated") { refreshAction() }
        socket.on("appointment_reassigned") { refreshAction() }
        socket.on("appointment_cancelled") { args ->
            refreshAction()
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val reason = data.optString("reason", "The doctor removed the slot.")
                runOnUiThread {
                    Toast.makeText(this, "Appointment Cancelled: $reason", Toast.LENGTH_LONG).show()
                    // Reusing push notification logic
                    val notif = NotificationResponse(
                        id = (System.currentTimeMillis() % 10000).toInt(),
                        type = "Cancellation",
                        title = "Appointment Cancelled",
                        description = "Your appointment was cancelled because the doctor removed the slot.",
                        isRead = false,
                        createdAt = ""
                    )
                    showPushNotification(notif)
                }
            }
        }
        
        socket.on("consultation_started") {
            refreshAction()
        }

        socket.on("consultation_ended") { refreshAction() }
        socket.on("prescription_created") { refreshAction() }
        socket.on("medical_record_uploaded") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val role = data.optString("role")
                if (role == "doctor") {
                    runOnUiThread {
                        Toast.makeText(this@PatientDashboardActivity, "A doctor has uploaded a new record for you.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun refreshDashboardData() {
        loadDashboardGreeting()
        loadDashboardData()
        loadHealthTip()
    }

    private fun loadDashboardGreeting() {
        val userId = sessionManager.getUserId()
        if (userId == -1) return

        apiService.getDashboardData(userId, "patient").enqueue(object : Callback<com.simats.Tmapp.api.DashboardResponse> {
            override fun onResponse(call: Call<com.simats.Tmapp.api.DashboardResponse>, response: Response<com.simats.Tmapp.api.DashboardResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val greeting = response.body()?.greeting ?: "Hello"
                    tvGreeting?.text = "$greeting,"
                } else {
                    tvGreeting?.text = "Hello,"
                }
            }
            override fun onFailure(call: Call<com.simats.Tmapp.api.DashboardResponse>, t: Throwable) {
                tvGreeting?.text = "Hello,"
            }
        })
    }

    private fun loadHealthTip() {
        val userId = sessionManager.getUserId()
        if (userId == -1) return
        
        apiService.getDailyHealthTip(userId).enqueue(object : Callback<HealthTipResponse> {
            override fun onResponse(call: Call<HealthTipResponse>, response: Response<HealthTipResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    tvHealthTip?.text = response.body()?.tip
                } else {
                    tvHealthTip?.text = "Stay healthy and take care of yourself today."
                }
            }
            override fun onFailure(call: Call<HealthTipResponse>, t: Throwable) {
                tvHealthTip?.text = "Stay healthy and take care of yourself today."
            }
        })
    }

    private fun loadDashboardData() {
        viewModel.loadNotifications()
    }


    private fun syncProfileUI() {
        val userId = sessionManager.getUserId()
        if (userId == -1) return

        // Initial display from cache
        val cachedName = sessionManager.getUserName()
        if (cachedName.isNotEmpty()) {
            tvUserName?.text = cachedName
        } else {
            tvUserName?.text = "Patient"
        }

        // Fetch fresh profile data
        apiService.getProfile(userId, "patient").enqueue(object : Callback<com.simats.Tmapp.api.ProfileResponse> {
            override fun onResponse(call: Call<com.simats.Tmapp.api.ProfileResponse>, response: Response<com.simats.Tmapp.api.ProfileResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    val fullName = profile.name
                    
                    runOnUiThread {
                        tvUserName?.text = fullName
                        sessionManager.saveUserName(fullName)
                    }
                }
            }

            override fun onFailure(call: Call<com.simats.Tmapp.api.ProfileResponse>, t: Throwable) {
                // Keep cached data on failure
            }
        })

        // Profile Image
        ivAvatar?.let {
            val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
            val avatarVersion = sessionManager.getAvatarVersion()
            val avatarUrl = "$baseUrl/api/profile/image/${sessionManager.getUserId()}?role=patient&v=$avatarVersion"

            ivAvatar?.let {
                AvatarUtils.loadAvatar(
                    imageView = it,
                    imageUrl = avatarUrl,
                    name = sessionManager.getUserName().ifEmpty { "Patient" }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncProfileUI()
        refreshDashboardData()
        
        val filter = android.content.IntentFilter("NEW_NOTIFICATION_RECEIVED")
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
            .registerReceiver(notificationReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(notificationReceiver)
    }

    private fun showPushNotification(notif: NotificationResponse) {
        val channelId = "patient_notif_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Patient Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = if (notif.type == "Prescription") {
            Intent(this, ViewPrescriptionActivity::class.java).apply {
                putExtra("appointment_id", notif.appointmentId ?: -1)
                putExtra("doctor_id", notif.doctorId ?: -1)
            }
        } else {
            Intent(this, ConsultationWaitingActivity::class.java).apply {
                putExtra("appointment_id", notif.appointmentId ?: -1)
                putExtra("doctor_id", notif.doctorId ?: -1)
            }
        }
        val pendingIntent = PendingIntent.getActivity(this, notif.id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_video_white)
            .setContentTitle(notif.title)
            .setContentText(notif.description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notif.id, notification)
    }


    private fun formatTime(timeStr: String?): String {
        if (timeStr.isNullOrEmpty()) return "--:--"
        return try {
            val parser = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val parsed = parser.parse(timeStr)
            if (parsed != null) formatter.format(parsed) else {
                val parser2 = SimpleDateFormat("HH:mm", Locale.getDefault())
                val parsed2 = parser2.parse(timeStr)
                if(parsed2 != null) formatter.format(parsed2) else timeStr
            }
        } catch (e: Exception) {
            timeStr
        }
    }
}
