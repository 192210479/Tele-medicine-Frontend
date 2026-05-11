package com.simats.Tmapp

import android.app.AlertDialog
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
import androidx.lifecycle.ViewModelProvider
import com.simats.Tmapp.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminAppointmentsActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private val apiService = ApiClient.instance
    
    private lateinit var rvAppointments: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvEmptyStateSubtitle: TextView
    private lateinit var adapter: AdminAppointmentAdapter
    private lateinit var viewModel: AdminDashboardViewModel
    
    private var allAppointments: List<AdminAppointmentResponse> = emptyList()
    private var currentFilter = "Upcoming"
    private var openSection: String = "appointments"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_manage_appointments)
        
        sessionManager = SessionManager.getInstance(this)
        openSection = intent.getStringExtra("OPEN_SECTION")?.lowercase() ?: "appointments"

        rvAppointments = findViewById(R.id.rvAppointments)
        llEmptyState = findViewById(R.id.llEmptyState)
        tvEmptyStateSubtitle = findViewById(R.id.tvEmptyStateSubtitle)
        
        rvAppointments.layoutManager = LinearLayoutManager(this)
        
        viewModel = ViewModelProvider(this)[AdminDashboardViewModel::class.java]
        openRequestedSection()

        adapter = AdminAppointmentAdapter(emptyList(), { appt ->
            showCancelDialog(appt)
        }, { appt ->
            showReassignDialog(appt)
        })
        rvAppointments.adapter = adapter

        // Tabs
        val tabUpcoming = findViewById<TextView>(R.id.tabUpcoming)
        val tabPending = findViewById<TextView>(R.id.tabPending)
        val tabCompleted = findViewById<TextView>(R.id.tabCompleted)
        val tabCancelled = findViewById<TextView>(R.id.tabCancelled)
        
        val tabs = listOf(tabUpcoming, tabPending, tabCompleted, tabCancelled)

        fun selectTab(selectedTab: TextView, filter: String) {
            for (tab in tabs) {
                tab.setBackgroundResource(android.R.color.transparent)
                tab.setTextColor(ContextCompat.getColor(this, R.color.text_grey))
            }
            selectedTab.setBackgroundResource(R.drawable.bg_tab_selected_light)
            selectedTab.setTextColor(ContextCompat.getColor(this, R.color.primary_blue))

            currentFilter = filter
            fetchAllAppointments()
        }

        tabUpcoming.setOnClickListener { selectTab(tabUpcoming, "Upcoming") }
        tabPending.setOnClickListener { selectTab(tabPending, "Pending") }
        tabCompleted.setOnClickListener { selectTab(tabCompleted, "Completed") }
        tabCancelled.setOnClickListener { selectTab(tabCancelled, "Cancelled") }
 
        // Start with upcoming
        selectTab(tabUpcoming, "Upcoming")

        findViewById<View>(R.id.ivBack).setOnClickListener {
            @Suppress("DEPRECATION")
            onBackPressed()
        }

        // Dashboard/Profile links from DoctorAppointmentsActivity reuse
        findViewById<View>(R.id.navHome)?.setOnClickListener { finish() }
        findViewById<View>(R.id.navProfile)?.setOnClickListener { 
            // Navigate to profile or just finish
            finish()
        }
        
        setupObservers()
        setupSocketListeners()
        viewModel.loadAppointments(sessionManager.getUserId())
    }

    private fun setupObservers() {
        viewModel.allAppointments.observe(this) { list ->
            if (list != null) {
                allAppointments = list.map { a ->
                    AdminAppointmentResponse(
                        id = a.id,
                        doctorId = a.doctorId,
                        patientId = a.patientId,
                        doctorName = a.doctorName,
                        patientName = a.patientName,
                        date = a.date,
                        time = a.time,
                        status = a.status,
                        consultationStatus = a.consultationStatus,
                        specialization = a.specialization
                    )
                }
                filterAndDisplay()
            }
        }
    }

    private fun openRequestedSection() {
        when (openSection) {
            "doctors" -> {
                finish()
                startActivity(Intent(this, AdminDashboardActivity::class.java).apply {
                    putExtra("OPEN_DASHBOARD_TAB", "doctors")
                })
            }

            "patients" -> {
                finish()
                startActivity(Intent(this, AdminDashboardActivity::class.java).apply {
                    putExtra("OPEN_DASHBOARD_TAB", "patients")
                })
            }

            "appointments" -> {
                // Stay in current screen as usual
            }
        }
    }

    private fun fetchAllAppointments() {
        viewModel.loadAppointments(sessionManager.getUserId())
    }


    private fun filterAndDisplay() {
        adapter.updateGroupedData(emptyList())

        // Map tab label → backend status string from /api/admin/appointments
        val backendStatus = when (currentFilter) {
            "Upcoming"  -> "Scheduled"   // future appointments
            "Pending"   -> "Missed"      // past but consultation not done
            "Completed" -> "Completed"
            "Cancelled" -> "Cancelled"
            else        -> currentFilter
        }

        val filtered = allAppointments.filter { appt ->
            appt.status.equals(backendStatus, ignoreCase = true)
        }

        val groupedList = mutableListOf<Any>()
        // Group by doctor name so admin sees all patients per doctor
        val groupedByDoctor = filtered.groupBy { it.doctorName ?: "Unknown Doctor" }
        for ((doctorName, appointments) in groupedByDoctor) {
            groupedList.add(doctorName)         // String header
            groupedList.addAll(appointments)    // AdminAppointmentResponse items
        }

        adapter.updateGroupedData(groupedList)

        if (filtered.isEmpty()) {
            rvAppointments.visibility = View.GONE
            llEmptyState.visibility = View.VISIBLE
            tvEmptyStateSubtitle.text = "No ${currentFilter.lowercase()} appointments found."
        } else {
            rvAppointments.visibility = View.VISIBLE
            llEmptyState.visibility = View.GONE
        }
    }

    private fun showCancelDialog(appt: AdminAppointmentResponse) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Appointment")
            .setMessage("Are you sure you want to cancel this appointment for ${appt.patientName}?")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                cancelAppointment(appt.id)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelAppointment(appointmentId: Int) {
        val adminId = sessionManager.getUserId()
        val body = mapOf(
            "user_id" to adminId,
            "role" to "admin"
        )
        ApiClient.instance.cancelAppointment(appointmentId, body).enqueue(object : retrofit2.Callback<GenericResponse> {
            override fun onResponse(call: retrofit2.Call<GenericResponse>, response: retrofit2.Response<GenericResponse>) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@AdminAppointmentsActivity, "Appointment cancelled successfully", Toast.LENGTH_SHORT).show()
                        fetchAllAppointments()
                        viewModel.loadDashboardData()
                    } else {
                        Toast.makeText(this@AdminAppointmentsActivity, "Failed to cancel appointment", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onFailure(call: retrofit2.Call<GenericResponse>, t: Throwable) {
                runOnUiThread {
                    Toast.makeText(this@AdminAppointmentsActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun showReassignDialog(appt: AdminAppointmentResponse) {
        apiService.getAllDoctors("admin").enqueue(object : Callback<List<Doctor>> {
            override fun onResponse(call: Call<List<Doctor>>, response: Response<List<Doctor>>) {
                if (response.isSuccessful && response.body() != null) {
                    val doctors = response.body()!!
                    val doctorNames = doctors.map { "Dr. ${it.name} (${it.specialization})" }.toTypedArray()

                    AlertDialog.Builder(this@AdminAppointmentsActivity)
                        .setTitle("Select New Doctor")
                        .setItems(doctorNames) { _, which ->
                            val selectedDoctor = doctors[which]
                            showDoctorSlotsPicker(appt.id, selectedDoctor.id)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    Toast.makeText(this@AdminAppointmentsActivity, "Could not load doctors", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Doctor>>, t: Throwable) {
                Toast.makeText(this@AdminAppointmentsActivity, "Error loading doctors", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDoctorSlotsPicker(appointmentId: Int, doctorId: Int) {
        apiService.getDoctorAvailability(doctorId).enqueue(object : Callback<List<AvailabilityResponse>> {
            override fun onResponse(call: Call<List<AvailabilityResponse>>, response: Response<List<AvailabilityResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    val slots = response.body()!!.filter { it.status == "Available" }
                    if (slots.isEmpty()) {
                        Toast.makeText(this@AdminAppointmentsActivity, "No available slots for this doctor.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val slotStrings = slots.map { "${it.date} | ${it.time}" }.toTypedArray()
                    
                    AlertDialog.Builder(this@AdminAppointmentsActivity)
                        .setTitle("Select Available Slot")
                        .setItems(slotStrings) { _, which ->
                            val selectedSlot = slots[which]
                            reassignAppointment(appointmentId, doctorId, selectedSlot.date, selectedSlot.time)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    Toast.makeText(this@AdminAppointmentsActivity, "Could not load slots", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<AvailabilityResponse>>, t: Throwable) {
                Toast.makeText(this@AdminAppointmentsActivity, "Error loading slots", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun reassignAppointment(appointmentId: Int, doctorId: Int, date: String, time: String) {
        viewModel.reassignAppointment(appointmentId, doctorId, date, time, sessionManager.getUserId()) { success, message ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this@AdminAppointmentsActivity, message ?: "Reassigned successfully", Toast.LENGTH_SHORT).show()
                    fetchAllAppointments() 
                    viewModel.loadDashboardData()
                } else {
                    Toast.makeText(this@AdminAppointmentsActivity, message ?: "Failed to reassign", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSocketListeners() {
        val refresh = {
            runOnUiThread {
                viewModel.loadAppointments(sessionManager.getUserId())
            }
        }

        SocketService.getInstance(this).connect()
        SocketService.socket?.on("new_appointment") { refresh() }
        SocketService.socket?.on("appointment_updated") { refresh() }
        SocketService.socket?.on("appointment_reassigned") { refresh() }
        SocketService.socket?.on("appointment_cancelled") { refresh() }
        SocketService.socket?.on("consultation_ended") { refresh() }
        SocketService.socket?.on("doctor_status_updated") { refresh() }
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketService.socket?.off("new_appointment")
        SocketService.socket?.off("appointment_updated")
        SocketService.socket?.off("appointment_reassigned")
        SocketService.socket?.off("appointment_cancelled")
        SocketService.socket?.off("consultation_ended")
        SocketService.socket?.off("doctor_status_updated")
    }
}
