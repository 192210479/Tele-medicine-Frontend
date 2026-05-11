package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.AppointmentResponse
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UpcomingAppointmentsActivity : AppCompatActivity() {

    private lateinit var rvAppointments: RecyclerView
    private lateinit var adapter: PatientUpcomingAppointmentAdapter
    private lateinit var sessionManager: SessionManager
    private val viewModel: UpcomingAppointmentsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upcoming_appointments)

        sessionManager = SessionManager.getInstance(this)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        rvAppointments = findViewById(R.id.rvAppointments)
        rvAppointments.layoutManager = LinearLayoutManager(this)

        adapter = PatientUpcomingAppointmentAdapter(emptyList(), 
            onJoinClick = { appointment -> startConsultationFlow(appointment) },
            onDetailsClick = { appointment ->
                val intent = Intent(this, AppointmentDetailsActivity::class.java).apply {
                    putExtra("appointment_id", appointment.id)
                }
                startActivity(intent)
            }
        )
        rvAppointments.adapter = adapter

        observeViewModel()
    }
    
    private fun observeViewModel() {
        val tvEmptyState = findViewById<android.widget.TextView>(R.id.tvEmptyState)
        
        lifecycleScope.launch {
            viewModel.appointments.collectLatest { list ->
                if (list.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    rvAppointments.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    rvAppointments.visibility = View.VISIBLE
                }
                adapter.updateList(list)
            }
        }
        
        lifecycleScope.launch {
            viewModel.error.collectLatest { msg ->
                msg?.let {
                    Toast.makeText(this@UpcomingAppointmentsActivity, it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupSocketListeners()
        refreshData()
    }

    private fun refreshData() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()
        viewModel.fetchAppointments(userId, role)
    }

    private fun setupSocketListeners() {
        val socket = SocketService.socket ?: return
        val refresh = { runOnUiThread { refreshData() } }
        socket.on("appointment_updated") { refresh() }
        socket.on("appointment_reassigned") { refresh() }
        socket.on("appointment_cancelled") { refresh() }
        socket.on("consultation_ended") { refresh() }
        socket.on("prescription_created") { refresh() }
        socket.on("consultation_ready") { refresh() }
        socket.on("consultation_started") { refresh() }
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketService.socket?.apply {
            off("appointment_updated")
            off("appointment_reassigned")
            off("appointment_cancelled")
            off("consultation_ended")
            off("prescription_created")
            off("consultation_ready")
            off("consultation_started")
        }
    }

    private fun startConsultationFlow(appointment: AppointmentResponse) {
        // PER REQUIREMENT 2: Clicking Join Consultation should open the patient waiting screen first.
        openWaitingScreen(appointment)
    }

    private fun openWaitingScreen(appointment: AppointmentResponse) {
        val intent = Intent(this@UpcomingAppointmentsActivity, ConsultationWaitingActivity::class.java).apply {
            putExtra("appointment_id", appointment.id)
            putExtra("doctor_id", appointment.doctorId)
            putExtra("doctor_name", appointment.doctorName)
            putExtra("doctor_specialization", appointment.specialization)
            putExtra("doctor_photo", appointment.doctorImage)
        }
        startActivity(intent)
    }
}
