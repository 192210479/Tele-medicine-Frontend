package com.simats.Tmapp

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
import com.simats.Tmapp.api.AppointmentResponse
import com.simats.Tmapp.api.ApiClient
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
    private lateinit var doctorAdapter: GroupedDoctorAppointmentAdapter
    
    private lateinit var tabUpcoming: TextView
    private lateinit var tabMissed: TextView
    private lateinit var tabCompleted: TextView
    private lateinit var tabCancelled: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    
    private var currentStatus = "Upcoming"
    private var allAppointments: List<AppointmentResponse> = emptyList()
    private var isStartingConsultation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_appointments)
        
        sessionManager = SessionManager.getInstance(this)
        
        rvAppointments = findViewById(R.id.rvAppointments)
        llEmptyState = findViewById(R.id.llEmptyState)
        tvEmptyStateSubtitle = findViewById(R.id.tvEmptyStateSubtitle)
        
        tabUpcoming = findViewById(R.id.tabUpcoming)
        tabMissed = findViewById(R.id.tabMissed)
        tabCompleted = findViewById(R.id.tabCompleted)
        tabCancelled = findViewById(R.id.tabCancelled)
        
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { reloadAppointments() }
        
        rvAppointments.layoutManager = LinearLayoutManager(this)
        rvAppointments.clipToPadding = false
        rvAppointments.setPadding(
            rvAppointments.paddingLeft,
            rvAppointments.paddingTop,
            rvAppointments.paddingRight,
            (104 * resources.displayMetrics.density).toInt()
        )
                
        doctorAdapter = GroupedDoctorAppointmentAdapter(
            onStartCallClick = { appt ->
                startConsultationForAppt(
                    appt.id,
                    appt.patientName,
                    appt.patientId,
                    appt.patientAge,
                    appt.patientGender,
                    appt.patientImage
                )
            },
            onViewDetailsClick = { appt ->
                val intent = Intent(this, AppointmentDetailsActivity::class.java)
                intent.putExtra("appointment_id", appt.id)
                startActivity(intent)
            },
            onActionClick = { appt, action ->
                handleApptAction(appt, action)
            }
        )
        rvAppointments.adapter = doctorAdapter

        setupTabs()
        
        findViewById<View>(R.id.ivBack).setOnClickListener { onBackPressed() }

        // Global Bottom Nav
        val bottomNav = findViewById<GlobalBottomNavigationView>(R.id.bottom_nav) ?: findViewById<View>(R.id.bottomNav)
        // If the layout uses include with ID bottomNav, we might need to find within it if it's not a GlobalBottomNavigationView instance
        
        reloadAppointments()
        setupSocket()
    }

    private fun setupTabs() {
        val tabs = listOf(tabUpcoming, tabMissed, tabCompleted, tabCancelled)

        fun selectTab(selectedTab: TextView, status: String) {
            for (tab in tabs) {
                tab.setBackgroundResource(android.R.color.transparent)
                tab.setTextColor(ContextCompat.getColor(this, R.color.text_grey))
            }
            selectedTab.setBackgroundResource(R.drawable.bg_tab_selected_light)
            selectedTab.setTextColor(ContextCompat.getColor(this, R.color.primary_blue))
            currentStatus = status
            filterAndDisplay()
        }

        tabUpcoming.setOnClickListener { selectTab(tabUpcoming, "Upcoming") }
        tabMissed.setOnClickListener { selectTab(tabMissed, "Missed") }
        tabCompleted.setOnClickListener { selectTab(tabCompleted, "Completed") }
        tabCancelled.setOnClickListener { selectTab(tabCancelled, "Cancelled") }

        // Default
        selectTab(tabUpcoming, "Upcoming")
    }

    private fun reloadAppointments() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()

        swipeRefresh.isRefreshing = true

        // Pass null for status — fetch ALL and filter client-side by tab.
        // Passing status="Upcoming" returns nothing because backend stores "Scheduled".
        apiService.getMyAppointments(userId, role, null).enqueue(object : Callback<List<AppointmentResponse>> {
            override fun onResponse(call: Call<List<AppointmentResponse>>, response: Response<List<AppointmentResponse>>) {
                swipeRefresh.isRefreshing = false
                if (response.isSuccessful && response.body() != null) {
                    allAppointments = response.body()!!
                    filterAndDisplay()
                } else {
                    showEmptyState("Could not load appointments.")
                }
            }
            override fun onFailure(call: Call<List<AppointmentResponse>>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                showEmptyState("Network error. Please check your connection.")
            }
        })
    }

    private fun filterAndDisplay() {
        val filterToday = intent.getBooleanExtra("FILTER_TODAY", false)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        
        val filteredList = allAppointments.filter { appt ->
            if (filterToday && appt.date != todayStr) return@filter false
            
            val status = DoctorAppointmentHelper.getStatus(appt)
            when (currentStatus) {
                "Upcoming" -> status == DoctorAppointmentHelper.AppointmentStatus.UPCOMING
                "Missed" -> status == DoctorAppointmentHelper.AppointmentStatus.MISSED
                "Completed" -> status == DoctorAppointmentHelper.AppointmentStatus.COMPLETED
                "Cancelled" -> status == DoctorAppointmentHelper.AppointmentStatus.CANCELLED
                else -> true
            }
        }

        val groupedItems = mutableListOf<AppointmentListItem>()
        val sortedList = DoctorAppointmentHelper.sort(filteredList)
        
        var lastDate = ""
        for (appt in sortedList) {
            val dateStr = appt.date ?: ""
            if (dateStr != lastDate) {
                groupedItems.add(AppointmentListItem.Header(formatHeaderDate(dateStr)))
                lastDate = dateStr
            }
            groupedItems.add(AppointmentListItem.Item(appt))
        }

        doctorAdapter.updateList(groupedItems)

        if (groupedItems.isEmpty()) {
            showEmptyState("No ${currentStatus.lowercase()} appointments.")
        } else {
            rvAppointments.visibility = View.VISIBLE
            llEmptyState.visibility = View.GONE
        }
    }

    private fun formatHeaderDate(dateStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
            val date = parser.parse(dateStr)
            if (date != null) {
                val today = Calendar.getInstance()
                val target = Calendar.getInstance().apply { time = date }
                
                if (today.get(Calendar.YEAR) == target.get(Calendar.YEAR) && 
                    today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
                    "Today, " + formatter.format(date)
                } else {
                    formatter.format(date)
                }
            } else dateStr
        } catch (e: Exception) { dateStr }
    }

    private fun handleApptAction(appt: AppointmentResponse, action: String) {
        when(action) {
            "missed" -> Toast.makeText(this, "Marked as missed", Toast.LENGTH_SHORT).show()
            "info" -> showCancellationInfoDialog(appt)
        }
    }

    private fun showCancellationInfoDialog(appt: AppointmentResponse) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val canceller = appt.cancelledBy?.trim()?.lowercase() ?: "unknown"
        val reason = appt.cancellationReason ?: "No reason provided"
        
        val cancellationText = when (canceller) {
            "patient" -> "This appointment was cancelled by the patient."
            "doctor" -> "This appointment was cancelled by you (the doctor)."
            "admin" -> "This appointment was cancelled by the Administrator."
            else -> "This appointment has been cancelled."
        }

        val date = DoctorAppointmentHelper.cleanText(appt.date)
        val time = DoctorAppointmentHelper.cleanText(appt.localTime ?: appt.time)
        val patientName = DoctorAppointmentHelper.cleanText(appt.patientName, "Patient")

        builder.setTitle("Appointment Cancellation Info")
        builder.setMessage(
            "Patient: $patientName\n" +
            "Date: $date\n" +
            "Time: $time\n\n" +
            "$cancellationText\n\n" +
            "Reason: $reason"
        )
        builder.setPositiveButton("Close", null)
        builder.show()
    }

    private fun showEmptyState(message: String) {
        rvAppointments.visibility = View.GONE
        llEmptyState.visibility = View.VISIBLE
        tvEmptyStateSubtitle.text = message
    }

    private fun startConsultationForAppt(apptId: Int, patientName: String?, patientId: Int?, patientAge: Int? = null, patientGender: String? = null, patientPhoto: String? = null) {
        if (isStartingConsultation) return
        isStartingConsultation = true

        val req = com.simats.Tmapp.api.ConsultationStartRequest(apptId, sessionManager.getUserId())
        apiService.startConsultation(req).enqueue(object : Callback<com.simats.Tmapp.api.ConsultationStartResponse> {
            override fun onResponse(call: Call<com.simats.Tmapp.api.ConsultationStartResponse>, response: Response<com.simats.Tmapp.api.ConsultationStartResponse>) {
                isStartingConsultation = false
                if (response.isSuccessful && response.body() != null) {
                    val resBody = response.body()!!
                    val intent = Intent(this@DoctorAppointmentsActivity, VideoConsultationActivity::class.java)
                    intent.putExtra("appointment_id", apptId)
                    intent.putExtra("consultation_id", resBody.consultationId)
                    intent.putExtra("channel_name", resBody.channel)
                    intent.putExtra("patient_name", patientName ?: "Patient")
                    intent.putExtra("doctor_id", sessionManager.getUserId())
                    patientId?.let { intent.putExtra("patient_id", it) }
                    patientAge?.let { intent.putExtra("patient_age", it) }
                    patientGender?.let { intent.putExtra("patient_gender", it) }
                    intent.putExtra("patient_photo", patientPhoto)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@DoctorAppointmentsActivity, "Failed to start consultation", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<com.simats.Tmapp.api.ConsultationStartResponse>, t: Throwable) {
                isStartingConsultation = false
                Toast.makeText(this@DoctorAppointmentsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupSocket() {
        val socket = SocketService.socket ?: return
        val doctorId = sessionManager.getUserId()

        socket.emit("join_room", "doctor_$doctorId")

        val refresh = { _: Array<Any> ->
            runOnUiThread { reloadAppointments() }
        }

        // Remove old listeners first to avoid duplicate triggers
        socket.off("new_appointment")
        socket.off("appointment_updated")
        socket.off("appointment_reassigned")
        socket.off("appointment_cancelled")
        socket.off("consultation_started")
        socket.off("consultation_ended")
        socket.off("prescription_created")

        socket.on("new_appointment", refresh)
        socket.on("appointment_updated", refresh)
        socket.on("appointment_reassigned", refresh)
        socket.on("appointment_cancelled", refresh)
        socket.on("consultation_started", refresh)
        socket.on("consultation_ended", refresh)
        socket.on("prescription_created", refresh)
    }

    override fun onResume() {
        super.onResume()
        reloadAppointments()
    }
    override fun onDestroy() {
        super.onDestroy()

        val socket = SocketService.socket ?: return
        socket.off("new_appointment")
        socket.off("appointment_updated")
        socket.off("appointment_reassigned")
        socket.off("appointment_cancelled")
        socket.off("consultation_started")
        socket.off("consultation_ended")
        socket.off("prescription_created")
    }
}
