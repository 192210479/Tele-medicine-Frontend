package com.simats.tmapp

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.ViewModelProvider
import com.simats.tmapp.api.*
import io.socket.client.IO
import io.socket.client.Socket
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminAppointmentsActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private val apiService = ApiClient.instance
    private var socket: Socket? = null
    
    private lateinit var rvAppointments: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvEmptyStateSubtitle: TextView
    private lateinit var adapter: AdminAppointmentAdapter
    private lateinit var viewModel: AdminDashboardViewModel
    private var selectedDoctorId: Int? = null
    
    private var allAppointments: List<AdminAppointmentResponse> = emptyList()
    private var currentFilter = "Upcoming"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_manage_appointments)
        
        sessionManager = SessionManager.getInstance(this)

        rvAppointments = findViewById(R.id.rvAppointments)
        llEmptyState = findViewById(R.id.llEmptyState)
        tvEmptyStateSubtitle = findViewById(R.id.tvEmptyStateSubtitle)
        
        rvAppointments.layoutManager = LinearLayoutManager(this)
        
        viewModel = ViewModelProvider(this)[AdminDashboardViewModel::class.java]

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
                        patientId = null,
                        doctorName = a.doctorName,
                        patientName = a.patientName,
                        date = a.date,
                        time = a.time,
                        status = a.status,
                        consultationStatus = a.consultationStatus
                    )
                }
                filterAndDisplay()
            }
        }
    }

    private fun fetchAllAppointments() {
        viewModel.loadAppointments(sessionManager.getUserId())
    }


    private fun filterAndDisplay() {
        // Clear old data first
        adapter.updateGroupedData(emptyList())

        val filtered = allAppointments.filter { it.status == currentFilter }

        // Group by Doctor Name
        val groupedList = mutableListOf<Any>()
        val groupedByDoctor = filtered.groupBy { it.doctorName ?: "Unknown Doctor" }
        
        for ((doctorName, appointments) in groupedByDoctor) {
            groupedList.add(doctorName) // Header (String)
            groupedList.addAll(appointments) // Item (AdminAppointmentResponse)
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
        viewModel.cancelAppointment(appointmentId, sessionManager.getUserId()) { success, message ->
            if (success) {
                Toast.makeText(this@AdminAppointmentsActivity, message ?: "Cancelled successfully", Toast.LENGTH_SHORT).show()
                fetchAllAppointments() // Refresh list
                viewModel.loadDashboardData() // Refresh dashboard
            } else {
                Toast.makeText(this@AdminAppointmentsActivity, message ?: "Failed to cancel", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReassignDialog(appt: AdminAppointmentResponse) {
        // Load doctors for selection
        apiService.getAllDoctors(sessionManager.getUserId(), "admin").enqueue(object : Callback<List<AdminDoctorResponse>> {
            override fun onResponse(call: Call<List<AdminDoctorResponse>>, response: Response<List<AdminDoctorResponse>>) {
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
            override fun onFailure(call: Call<List<AdminDoctorResponse>>, t: Throwable) {
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

                    val slotStrings = slots.map { "${it.date} | ${it.time_slot}" }.toTypedArray()
                    
                    AlertDialog.Builder(this@AdminAppointmentsActivity)
                        .setTitle("Select Available Slot")
                        .setItems(slotStrings) { _, which ->
                            val selectedSlot = slots[which]
                            reassignAppointment(appointmentId, doctorId, selectedSlot.date, selectedSlot.time_slot)
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
            if (success) {
                Toast.makeText(this@AdminAppointmentsActivity, message ?: "Reassigned successfully", Toast.LENGTH_SHORT).show()
                fetchAllAppointments() 
                viewModel.loadDashboardData()
            } else {
                Toast.makeText(this@AdminAppointmentsActivity, message ?: "Failed to reassign", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showError(msg: String) {
        rvAppointments.visibility = View.GONE
        llEmptyState.visibility = View.VISIBLE
        tvEmptyStateSubtitle.text = msg
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
