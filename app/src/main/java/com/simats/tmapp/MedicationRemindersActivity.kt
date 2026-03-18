package com.simats.tmapp

import android.Manifest
import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.simats.tmapp.api.GenericResponse
import com.simats.tmapp.api.MedicationReminderResponse
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class MedicationRemindersActivity : AppCompatActivity() {

    private lateinit var llRemindersContainer: LinearLayout
    private lateinit var sessionManager: SessionManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        }
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            fetchReminders()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication_reminders)

        sessionManager = SessionManager.getInstance(this)
        llRemindersContainer = findViewById(R.id.llRemindersContainer)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener { finish() }

        setupBottomNav()

        val ivAddReminder = findViewById<ImageView>(R.id.ivAddReminder)
        ivAddReminder.setOnClickListener {
            showAddReminderDialog()
        }

        checkNotificationPermission()
        fetchReminders()

        // Register receiver to refresh UI when notification actions are performed
        val filter = IntentFilter("com.simats.tmapp.REFRESH_REMINDERS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(refreshReceiver)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, PatientDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }
        findViewById<LinearLayout>(R.id.navBook).setOnClickListener {
            startActivity(Intent(this, SelectDoctorActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navHistory).setOnClickListener {
            startActivity(Intent(this, ConsultationHistoryActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun fetchReminders() {
        val userId = sessionManager.getUserId()
        if (userId == -1) return

        ApiClient.instance.getMedicationReminders(userId).enqueue(object : Callback<List<MedicationReminderResponse>> {
            override fun onResponse(call: Call<List<MedicationReminderResponse>>, response: Response<List<MedicationReminderResponse>>) {
                if (response.isSuccessful) {
                    val reminders = response.body() ?: emptyList()
                    displayReminders(reminders)
                    scheduleAlarms(reminders)
                } else {
                    Toast.makeText(this@MedicationRemindersActivity, "Failed to load reminders", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<MedicationReminderResponse>>, t: Throwable) {
                Toast.makeText(this@MedicationRemindersActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun scheduleAlarms(reminders: List<MedicationReminderResponse>) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val userId = sessionManager.getUserId()
        
        for (reminder in reminders) {
            if (reminder.status == "Completed" || reminder.status == "Skipped") continue

            val timeParts = reminder.reminderTime.split(":")
            if (timeParts.size != 2) continue

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
                set(Calendar.SECOND, 0)
                
                // If time has passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val intent = Intent(this, ReminderReceiver::class.java).apply {
                action = "com.simats.tmapp.ACTION_REMINDER"
                putExtra("reminder_id", reminder.id)
                putExtra("medicine_name", reminder.medicineName)
                putExtra("user_id", userId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                reminder.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun displayReminders(reminders: List<MedicationReminderResponse>) {
        val header = llRemindersContainer.getChildAt(0)
        llRemindersContainer.removeAllViews()
        if (header != null) llRemindersContainer.addView(header)

        if (reminders.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No reminders for today"
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, 50, 0, 0)
                setTextColor(Color.GRAY)
            }
            llRemindersContainer.addView(emptyTv)
            return
        }

        val inflater = LayoutInflater.from(this)
        val prefs = getSharedPreferences("MedicationPrefs", Context.MODE_PRIVATE)
        val currentTime = System.currentTimeMillis()

        for (reminder in reminders) {
            // Filter logic for "Completed" reminders
            if (reminder.status == "Completed") {
                val completionTime = prefs.getLong("taken_${reminder.id}", 0L)
                if (completionTime != 0L && (currentTime - completionTime) > 5 * 60 * 1000) {
                    continue // Hide if older than 5 minutes
                }
            }

            val itemView = inflater.inflate(R.layout.item_medication_reminder, llRemindersContainer, false)
            
            val tvName = itemView.findViewById<TextView>(R.id.tvMedicineName)
            val tvDetails = itemView.findViewById<TextView>(R.id.tvMedicineDetails)
            val tvTime = itemView.findViewById<TextView>(R.id.tvReminderTime)
            val llActions = itemView.findViewById<LinearLayout>(R.id.llActions)
            val llTakenBadge = itemView.findViewById<LinearLayout>(R.id.llTakenBadge)
            val llSkippedBadge = itemView.findViewById<LinearLayout>(R.id.llSkippedBadge)
            val btnMarkTaken = itemView.findViewById<MaterialButton>(R.id.btnMarkTaken)
            val btnSkip = itemView.findViewById<MaterialButton>(R.id.btnSkip)
            val flIcon = itemView.findViewById<FrameLayout>(R.id.flIcon)
            val ivIcon = itemView.findViewById<ImageView>(R.id.ivIcon)

            // Handle the combined medicine name and details
            val parts = reminder.medicineName.split("|")
            val name = parts[0]
            val details = if (parts.size > 1) parts[1] else ""
            
            tvName.text = name
            tvDetails.text = details
            tvTime.text = reminder.reminderTime

            when (reminder.status) {
                "Completed" -> {
                    llActions.visibility = View.GONE
                    llTakenBadge.visibility = View.VISIBLE
                    llSkippedBadge.visibility = View.GONE
                    flIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                    ivIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#16A34A"))
                }
                "Skipped" -> {
                    llActions.visibility = View.GONE
                    llTakenBadge.visibility = View.GONE
                    llSkippedBadge.visibility = View.VISIBLE
                    flIcon.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_light))
                    ivIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_danger))
                    
                    // Update skipped badge color to red as requested
                    val tvSkipped = llSkippedBadge.findViewById<TextView>(R.id.tvSkippedText ?: View.NO_ID) 
                        ?: llSkippedBadge.getChildAt(1) as? TextView
                    tvSkipped?.setTextColor(ContextCompat.getColor(this, R.color.red_danger))
                    val ivSkipped = llSkippedBadge.getChildAt(0) as? ImageView
                    ivSkipped?.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_danger))
                }
                else -> {
                    llActions.visibility = View.VISIBLE
                    llTakenBadge.visibility = View.GONE
                    llSkippedBadge.visibility = View.GONE
                    
                    // Active state: blue icon
                    flIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EFF6FF"))
                    ivIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#2F6FED"))
                    
                    btnMarkTaken.setOnClickListener {
                        markAsTaken(reminder.id)
                    }
                    
                    btnSkip.setOnClickListener {
                        skipReminder(reminder.id)
                    }
                }
            }

            llRemindersContainer.addView(itemView)
        }
    }

    private fun markAsTaken(reminderId: Int) {
        ApiClient.instance.completeMedicationReminder(reminderId).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    val prefs = getSharedPreferences("MedicationPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putLong("taken_$reminderId", System.currentTimeMillis()).apply()
                    
                    Toast.makeText(this@MedicationRemindersActivity, "Marked as taken", Toast.LENGTH_SHORT).show()
                    fetchReminders()
                } else {
                    Toast.makeText(this@MedicationRemindersActivity, "Failed to update status", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@MedicationRemindersActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun skipReminder(reminderId: Int) {
        // Since there's no specific skip API, we use the complete API to mark it as processed
        ApiClient.instance.completeMedicationReminder(reminderId).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MedicationRemindersActivity, "Reminder skipped", Toast.LENGTH_SHORT).show()
                    // Manually refresh the UI to show the skipped state
                    fetchReminders()
                } else {
                    Toast.makeText(this@MedicationRemindersActivity, "Failed to skip reminder", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@MedicationRemindersActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddReminderDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_reminder)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

        val etMedName = dialog.findViewById<EditText>(R.id.etMedName)
        val etTime = dialog.findViewById<EditText>(R.id.etTime)
        val etDosage = dialog.findViewById<EditText>(R.id.etDosage)
        val cgMealTiming = dialog.findViewById<ChipGroup>(R.id.cgMealTiming)
        val chipBefore = dialog.findViewById<Chip>(R.id.chipBeforeFood)
        val chipAfter = dialog.findViewById<Chip>(R.id.chipAfterFood)
        val chipSleep = dialog.findViewById<Chip>(R.id.chipBeforeSleep)
        
        etTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                val time = String.format("%02d:%02d", hour, minute)
                etTime.setText(time)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        dialog.findViewById<ImageView>(R.id.ivCloseAdd).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        
        dialog.findViewById<MaterialButton>(R.id.btnAddMedicine).setOnClickListener {
            val name = etMedName.text.toString()
            val time = etTime.text.toString()
            val dosage = etDosage.text.toString()
            
            val selectedTimings = mutableListOf<String>()
            if (chipBefore.isChecked) selectedTimings.add("Before Food")
            if (chipAfter.isChecked) selectedTimings.add("After food")
            if (chipSleep.isChecked) selectedTimings.add("Before sleep")
            val mealTiming = selectedTimings.joinToString(" • ")

            if (name.isNotEmpty() && time.isNotEmpty()) {
                val medicineDetails = if (dosage.isNotEmpty()) "$dosage • $mealTiming" else mealTiming
                // Store combined name and details in medicine_name field
                addReminder("$name|$medicineDetails", time, dialog)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun addReminder(name: String, time: String, dialog: Dialog) {
        val userId = sessionManager.getUserId()
        if (userId == -1) return

        val body = mapOf(
            "user_id" to userId,
            "role" to "patient",
            "medicine_name" to name,
            "reminder_time" to time
        )

        ApiClient.instance.addMedicationReminder(body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MedicationRemindersActivity, "Reminder added", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    fetchReminders()
                } else {
                    Toast.makeText(this@MedicationRemindersActivity, "Failed to add reminder", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@MedicationRemindersActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
