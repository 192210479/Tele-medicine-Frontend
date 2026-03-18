package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.tmapp.api.AppointmentResponse
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UpcomingAppointmentsActivity : AppCompatActivity() {

    private lateinit var rvAppointments: RecyclerView
    private lateinit var adapter: UpcomingAppointmentAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upcoming_appointments)

        sessionManager = SessionManager.getInstance(this)

        // Back button listener
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Handle back press using OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        // Initialize RecyclerView
        rvAppointments = findViewById(R.id.rvAppointments)
        rvAppointments.layoutManager = LinearLayoutManager(this)

        // Initialize Adapter with click listener
        adapter = UpcomingAppointmentAdapter(emptyList(), 
            onJoinClick = { appointment ->
                startConsultationFlow(appointment)
            },
            onDetailsClick = { appointment ->
                val intent = Intent(this, AppointmentDetailsActivity::class.java)
                intent.putExtra("appointment_id", appointment.id)
                startActivity(intent)
            }
        )
        rvAppointments.adapter = adapter

        // Initial fetch
        fetchAppointments()
    }
 
    override fun onResume() {
        super.onResume()
        setupSocketListeners()
        fetchAppointments()
    }

    private fun setupSocketListeners() {
        val socket = SocketService.socket ?: return
        val refresh = {
            runOnUiThread { fetchAppointments() }
        }
        socket.on("appointment_updated") { refresh() }
        socket.on("appointment_reassigned") { refresh() }
        socket.on("appointment_cancelled") { refresh() }
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketService.socket?.off("appointment_updated")
        SocketService.socket?.off("appointment_reassigned")
        SocketService.socket?.off("appointment_cancelled")
    }

    private fun fetchAppointments() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()

        // Fetch appointments from Flask backend
        ApiClient.instance.getMyAppointments(userId, role, "Upcoming")
            .enqueue(object : Callback<List<AppointmentResponse>> {
                override fun onResponse(
                    call: Call<List<AppointmentResponse>>,
                    response: Response<List<AppointmentResponse>>
                ) {
                    if (response.isSuccessful) {
                        val appointments = response.body() ?: emptyList()
                        val upcomingOnly = appointments.filter { 
                            it.status.equals("Upcoming", ignoreCase = true) || it.status.equals("Scheduled", ignoreCase = true)
                        }
                        val sorted = upcomingOnly.sortedWith(compareBy<AppointmentResponse> { it.date ?: "" }.thenBy { it.time ?: "" })
                        
                        if (sorted.isEmpty()) {
                            Toast.makeText(this@UpcomingAppointmentsActivity, "No scheduled appointments found", Toast.LENGTH_SHORT).show()
                        }
                        adapter.updateList(sorted)
                    } else {
                        Toast.makeText(this@UpcomingAppointmentsActivity, "Failed to load appointments", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<AppointmentResponse>>, t: Throwable) {
                    Toast.makeText(this@UpcomingAppointmentsActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun startConsultationFlow(appointment: AppointmentResponse) {
        val intent = Intent(this@UpcomingAppointmentsActivity, ConsultationWaitingActivity::class.java)
        intent.putExtra("appointment_id", appointment.id)
        intent.putExtra("doctor_id", appointment.doctorId)
        intent.putExtra("doctor_name", appointment.doctorName)
        intent.putExtra("doctor_specialization", appointment.specialization)
        startActivity(intent)
    }
}
