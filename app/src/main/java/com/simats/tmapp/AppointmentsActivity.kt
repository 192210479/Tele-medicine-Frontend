package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.simats.tmapp.api.AppointmentResponse
import com.simats.tmapp.api.ApiClient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AppointmentsActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var rvAppointments: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var llEmptyState: LinearLayout
    private lateinit var adapter: AppointmentsAdapter
    private var allAppointments: List<AppointmentResponse> = emptyList()
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointments)

        sessionManager = SessionManager.getInstance(this)
        val isDoctor = sessionManager.getUserRole().lowercase().contains("doctor")

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        rvAppointments = findViewById(R.id.rvAppointments)
        tabLayout = findViewById(R.id.tabLayout)
        llEmptyState = findViewById(R.id.llEmptyState)

        rvAppointments.layoutManager = LinearLayoutManager(this)
        adapter = AppointmentsAdapter(emptyList(), isDoctor, onStartConsultationClick = { appointment ->
            val status = appointment.status ?: ""
            if (status.equals("Upcoming", true) || status.equals("Scheduled", true) || status.equals("Booked", true)) {
                val intent = Intent(this, ConsultationWaitingActivity::class.java)
                intent.putExtra("appointment_id", appointment.id)
                intent.putExtra("doctor_id", appointment.doctorId)
                intent.putExtra("doctor_name", appointment.doctorName)
                intent.putExtra("doctor_specialization", appointment.specialization)
                startActivity(intent)
            }
        }, onShareReportsClick = { appointment ->
            val intent = Intent(this, MedicalRecordsActivity::class.java)
            intent.putExtra("appointment_id", appointment.id)
            intent.putExtra("doctor_id", appointment.doctorId)
            startActivity(intent)
        }, onViewDetailsClick = { appointment ->
            val intent = Intent(this, AppointmentDetailsActivity::class.java)
            intent.putExtra("appointment_id", appointment.id)
            startActivity(intent)
        })
        rvAppointments.adapter = adapter

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                filterAndDisplay()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        if (tabLayout.tabCount > 1) {
            // Check if the middle tab is "Missed" and remove it
            for (i in 0 until tabLayout.tabCount) {
                if (tabLayout.getTabAt(i)?.text?.toString()?.contains("Missed", true) == true) {
                    tabLayout.removeTabAt(i)
                    break 
                }
            }
        }

        fetchAppointments()
    }

    private fun fetchAppointments() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()

        // Fetch all appointments (no filter on status to get everything)
        ApiClient.instance.getMyAppointments(userId, role, null).enqueue(object : Callback<List<AppointmentResponse>> {
            override fun onResponse(call: Call<List<AppointmentResponse>>, response: Response<List<AppointmentResponse>>) {
                android.util.Log.d("APPOINTMENTS_API", response.body().toString())
                if (response.isSuccessful && response.body() != null) {
                    allAppointments = response.body()!!
                    filterAndDisplay()
                } else {
                    showEmpty(true)
                }
            }

            override fun onFailure(call: Call<List<AppointmentResponse>>, t: Throwable) {
                showEmpty(true)
            }
        })
    }

    private fun filterAndDisplay() {
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val filtered = when (currentTab) {
            0 -> allAppointments.filter { it ->
                val isCompleted = it.status.equals("Completed", true) || it.consultationStatus.equals("Completed", true)
                if (it.status.equals("Cancelled", true)) return@filter false
                if (isCompleted) return@filter false
                true
            }
            1 -> allAppointments.filter { it ->
                val isCompleted = it.status.equals("Completed", true) || it.consultationStatus.equals("Completed", true)
                isCompleted || it.status.equals("Cancelled", true) // Allow history/cancelled here
            }
            else -> allAppointments
        }
        
        // Sorting: Nearest date and time for Upcoming, Newest first for others
        val sorted = if (currentTab == 0) {
            filtered.sortedWith(
                compareBy<AppointmentResponse> {
                    it.date ?: ""
                }.thenBy {
                    it.time ?: ""
                }
            )
        } else {
            filtered.sortedWith(compareByDescending<AppointmentResponse> { it.date ?: "" }.thenByDescending { it.time ?: "" })
        }

        adapter.updateList(sorted)
        showEmpty(sorted.isEmpty())
    }

    private fun showEmpty(show: Boolean) {
        rvAppointments.visibility = if (show) View.GONE else View.VISIBLE
        llEmptyState.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        SocketManager.getInstance(this).connect()
        fetchAppointments()
        setupSocketListeners()
    }

    private fun setupSocketListeners() {
        val socket = SocketService.socket ?: return
        val refreshAction = {
            runOnUiThread { fetchAppointments() }
        }
        socket.on("new_appointment") { refreshAction() }
        socket.on("appointment_updated") { refreshAction() }
        socket.on("appointment_reassigned") { refreshAction() }
        socket.on("appointment_cancelled") {
            runOnUiThread {
                fetchAppointments()
                androidx.appcompat.app.AlertDialog.Builder(this@AppointmentsActivity)
                    .setTitle("Appointment Cancelled")
                    .setMessage("An appointment has been cancelled.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
        socket.on("consultation_started") { refreshAction() }
        socket.on("consultation_ready") { refreshAction() }
        socket.on("consultation_ended") { refreshAction() }
        socket.on("prescription_created") { refreshAction() }
    }

    override fun onDestroy() {
        super.onDestroy()
        val socket = SocketService.socket ?: return
        socket.off("new_appointment")
        socket.off("appointment_updated")
        socket.off("appointment_reassigned")
        socket.off("appointment_cancelled")
        socket.off("consultation_ready")
        socket.off("consultation_started")
        socket.off("consultation_ended")
    }
}
