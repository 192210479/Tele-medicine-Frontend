package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.simats.tmapp.api.AppointmentResponse
import com.simats.tmapp.api.ApiClient
import io.socket.client.Socket
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DoctorAppointmentsActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private val apiService = ApiClient.instance
    
    private lateinit var rvAppointments: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvEmptyStateSubtitle: TextView
    private lateinit var adapter: AppointmentsAdapter
    
    private lateinit var tabUpcoming: TextView
    private lateinit var tabCompleted: TextView
    private lateinit var tabCancelled: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    
    private var currentStatus = "Scheduled"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_appointments)
        
        sessionManager = SessionManager.getInstance(this)
        val isDoctor = sessionManager.getUserRole().lowercase().contains("doctor")

        rvAppointments = findViewById(R.id.rvAppointments)
        llEmptyState = findViewById(R.id.llEmptyState)
        tvEmptyStateSubtitle = findViewById(R.id.tvEmptyStateSubtitle)
        
        tabUpcoming = findViewById(R.id.tabUpcoming)
        tabCompleted = findViewById(R.id.tabCompleted)
        tabCancelled = findViewById(R.id.tabCancelled)
        
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { reloadAppointments() }
        
        rvAppointments.layoutManager = LinearLayoutManager(this)
        
        adapter = AppointmentsAdapter(
            appointments = emptyList(),
            isDoctor = isDoctor,
            onStartConsultationClick = { appointment ->
                startConsultationForAppt(appointment.id, appointment.patientName, appointment.patientId, appointment.patientAge, appointment.patientGender)
            },
            onCancelClick = { appointment ->
                cancelAppointmentForAppt(appointment.id)
            },
            onViewDetailsClick = { appointment ->
                val intent = Intent(this, AppointmentDetailsActivity::class.java)
                intent.putExtra("appointment_id", appointment.id)
                startActivity(intent)
            }
        )
        rvAppointments.adapter = adapter

        // Tabs
        val tabs = listOf(tabUpcoming, tabCompleted, tabCancelled)

        fun selectTab(selectedTab: TextView, status: String) {
            // Reset tabs
            for (tab in tabs) {
                tab.setBackgroundResource(android.R.color.transparent)
                tab.setTextColor(ContextCompat.getColor(this, R.color.text_grey))
            }
            // Set active tab
            selectedTab.setBackgroundResource(R.drawable.bg_tab_selected_light)
            selectedTab.setTextColor(ContextCompat.getColor(this, R.color.primary_blue))

            currentStatus = status
            reloadAppointments()
        }

        tabUpcoming.setOnClickListener { selectTab(tabUpcoming, "Scheduled") }
        tabCompleted.setOnClickListener { selectTab(tabCompleted, "Completed") }
        tabCancelled.setOnClickListener { selectTab(tabCancelled, "Cancelled") }

        // Start with upcoming
        selectTab(tabUpcoming, "Scheduled")

        // Click Actions
        findViewById<View>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        // Bottom Nav Logic depending on role
        if (isDoctor) {
            findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
                val intent = Intent(this, DoctorDashboardActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
            findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
                val intent = Intent(this, DoctorProfileSettingsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
        } else {
            // Patient bottom nav hooks inside DoctorAppointmentsActivity 
            // since it was originally sharing the layout logic!
            findViewById<View>(R.id.navHome)?.setOnClickListener { 
                startActivity(Intent(this, PatientDashboardActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
                finish()
            }
            findViewById<View>(R.id.navBook)?.setOnClickListener { 
                startActivity(Intent(this, SelectDoctorActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
                finish()
            }
            findViewById<View>(R.id.navHistory)?.setOnClickListener { 
                startActivity(Intent(this, ConsultationHistoryActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
                finish()
            }
            findViewById<View>(R.id.navProfile)?.setOnClickListener { 
                startActivity(Intent(this, ProfileActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
                finish()
            }
        }
        
    }

    private var allAppointments: List<AppointmentResponse> = emptyList()

    private fun reloadAppointments() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()

        swipeRefresh.isRefreshing = true
        // Single Source of Truth: Fetch ALL
        apiService.getMyAppointments(userId, role).enqueue(object : Callback<List<AppointmentResponse>> {
            override fun onResponse(call: Call<List<AppointmentResponse>>, response: Response<List<AppointmentResponse>>) {
                swipeRefresh.isRefreshing = false
                if (response.isSuccessful && response.body() != null) {
                    android.util.Log.d("AppointmentsActivity", "FETCHING APPOINTMENTS")
                    allAppointments = response.body()!!
                    android.util.Log.d("AppointmentsActivity", "UPDATED LIST SIZE: ${allAppointments.size}")
                    filterAndDisplay()
                } else {
                    showEmptyState("Could not load appointments.")
                }
            }

            override fun onFailure(call: Call<List<AppointmentResponse>>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                showEmptyState("Network error while loading appointments.")
            }
        })
    }

    private fun filterAndDisplay() {
        val filterToday = intent.getBooleanExtra("FILTER_TODAY", false)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        
        if (filterToday) {
            findViewById<TextView>(R.id.tvTitle)?.text = "Today's Appointments"
            // If filtering today, we usually want all statuses unless specified
        }

        val now = Calendar.getInstance()

        // Frontend Filtering
        val filteredList = allAppointments.filter { appt ->
            val isCompleted = appt.status?.equals("Completed", ignoreCase = true) == true || 
                              appt.consultationStatus?.equals("Completed", ignoreCase = true) == true

            val statusMatch = when (currentStatus) {
                "Scheduled" -> {
                    if (appt.status?.equals("Cancelled", ignoreCase = true) == true) return@filter false
                    if (isCompleted) return@filter false
                    
                    try {
                        val timeStr = if (appt.time?.length == 5) "${appt.time}:00" else appt.time
                        val apptDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse("${appt.date} $timeStr")
                        apptDateTime != null && apptDateTime.after(now.time)
                    } catch (e: Exception) {
                        appt.status?.equals("Scheduled", true) == true || appt.status?.equals("Upcoming", true) == true
                    }
                }
                "Completed" -> isCompleted
                "Cancelled" -> appt.status?.equals("Cancelled", true) == true
                else -> true
            }
            
            val dateMatch = if (filterToday) appt.date == todayStr else true
            
            statusMatch && dateMatch
        }

        val sortedList = filteredList.sortedWith(
            compareBy<AppointmentResponse> { it.date ?: "" }.thenBy { it.time ?: "" }
        )
        adapter.updateList(sortedList)

        // Debug logging tracking states across lists
        val upcomingCount = allAppointments.count { 
            val isCompleted = it.status?.equals("Completed", ignoreCase = true) == true || 
                              it.consultationStatus?.equals("Completed", ignoreCase = true) == true
            val isCancelled = it.status?.equals("Cancelled", ignoreCase = true) == true
            !isCompleted && !isCancelled
        }
        val completedCount = allAppointments.count { 
            it.status?.equals("Completed", ignoreCase = true) == true || 
            it.consultationStatus?.equals("Completed", ignoreCase = true) == true
        }
        android.util.Log.d("AppointmentsActivity", "UPCOMING COUNT: $upcomingCount")
        android.util.Log.d("AppointmentsActivity", "COMPLETED COUNT: $completedCount")

        if (sortedList.isEmpty()) {
            showEmptyState("You don't have any ${currentStatus.lowercase()} appointments.")
        } else {
            rvAppointments.visibility = View.VISIBLE
            llEmptyState.visibility = View.GONE
        }
    }

    private fun showEmptyState(message: String) {
        rvAppointments.visibility = View.GONE
        llEmptyState.visibility = View.VISIBLE
        tvEmptyStateSubtitle.text = message
    }

    private fun startConsultationForAppt(apptId: Int, patientName: String?, patientId: Int?, patientAge: Int? = null, patientGender: String? = null) {
        val req = com.simats.tmapp.api.ConsultationStartRequest(apptId, sessionManager.getUserId())
        apiService.startConsultation(req).enqueue(object : Callback<com.simats.tmapp.api.ConsultationStartResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.ConsultationStartResponse>, response: Response<com.simats.tmapp.api.ConsultationStartResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val resBody = response.body()!!
                    android.util.Log.d("Consultation", "START API CALLED")
                    android.util.Log.d("Consultation", "STATUS RESPONSE: ${resBody.status}")
                    
                    apiService.getConsultationStatus(apptId).enqueue(object : Callback<com.simats.tmapp.api.ConsultationStatusResponse> {
                        override fun onResponse(call: Call<com.simats.tmapp.api.ConsultationStatusResponse>, statusResponse: Response<com.simats.tmapp.api.ConsultationStatusResponse>) {
                            if (statusResponse.isSuccessful && statusResponse.body() != null) {
                                val statusBody = statusResponse.body()!!
                                android.util.Log.d("Consultation", "STATUS RESPONSE: ${statusBody.status}")
                                if (statusBody.status == "ready" && statusBody.canJoin == true) {
                                    val intent = Intent(this@DoctorAppointmentsActivity, VideoConsultationActivity::class.java)
                                    intent.putExtra("appointment_id", apptId)
                                    intent.putExtra("consultation_id", resBody.consultationId)
                                    intent.putExtra("channel_name", resBody.channel)
                                    intent.putExtra("patient_name", patientName ?: "Patient")
                                    intent.putExtra("doctor_id", sessionManager.getUserId())
                                    if (patientId != null) {
                                        intent.putExtra("patient_id", patientId)
                                    }
                                    if (patientAge != null) {
                                        intent.putExtra("patient_age", patientAge)
                                    }
                                    if (patientGender != null) {
                                        intent.putExtra("patient_gender", patientGender)
                                    }
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this@DoctorAppointmentsActivity, "Consultation not ready", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        override fun onFailure(call: Call<com.simats.tmapp.api.ConsultationStatusResponse>, t: Throwable) {
                            Toast.makeText(this@DoctorAppointmentsActivity, "Status Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    Toast.makeText(this@DoctorAppointmentsActivity, "Failed to start consultation", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<com.simats.tmapp.api.ConsultationStartResponse>, t: Throwable) {
                Toast.makeText(this@DoctorAppointmentsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun cancelAppointmentForAppt(apptId: Int) {
        val body = mapOf(
            "user_id" to sessionManager.getUserId(),
            "role" to "doctor"
        )

        apiService.cancelAppointment(apptId, body).enqueue(object : Callback<com.simats.tmapp.api.GenericResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.GenericResponse>, response: Response<com.simats.tmapp.api.GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@DoctorAppointmentsActivity, "Appointment cancelled", Toast.LENGTH_SHORT).show()
                    reloadAppointments()
                } else {
                    Toast.makeText(this@DoctorAppointmentsActivity, "Failed to cancel", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<com.simats.tmapp.api.GenericResponse>, t: Throwable) {
                Toast.makeText(this@DoctorAppointmentsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupSocket() {
        val socket = SocketService.socket ?: return
        
        val joinRoomData = JSONObject()
        joinRoomData.put("room", "doctor_${sessionManager.getUserId()}")
        socket.emit("join_room", joinRoomData)
        
        socket.on("new_appointment") {
            runOnUiThread { reloadAppointments() }
        }
        socket.on("appointment_updated") {
            runOnUiThread { reloadAppointments() }
        }
        socket.on("appointment_reassigned") {
            runOnUiThread { reloadAppointments() }
        }
        socket.on("appointment_cancelled") {
            runOnUiThread { reloadAppointments() }
        }
        socket.on("consultation_started") {
            runOnUiThread { reloadAppointments() }
        }
        socket.on("consultation_ended") {
            runOnUiThread { reloadAppointments() }
        }
        socket.on("prescription_created") {
            runOnUiThread { reloadAppointments() }
        }
        socket.on("consultation_ready") {
            runOnUiThread { reloadAppointments() }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure socket is connected
        SocketManager.getInstance(this).connect()
        
        // Start with upcoming tab selected and fetch appointments
        tabUpcoming.setBackgroundResource(R.drawable.bg_tab_selected_light)
        tabUpcoming.setTextColor(ContextCompat.getColor(this, R.color.primary_blue))
        currentStatus = "Scheduled"

        reloadAppointments()
        setupSocket()
    }
}
