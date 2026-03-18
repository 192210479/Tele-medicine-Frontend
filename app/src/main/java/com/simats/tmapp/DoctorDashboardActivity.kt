package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.simats.tmapp.api.AvailabilityResponse
import com.simats.tmapp.api.AppointmentResponse
import com.simats.tmapp.api.ConsultationActionRequest
import com.simats.tmapp.api.ConsultationStartRequest
import com.simats.tmapp.api.ConsultationStartResponse
import com.simats.tmapp.api.ApiClient
import io.socket.client.Socket
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class DoctorDashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val viewModel: DoctorDashboardViewModel by viewModels()
    private val apiService = ApiClient.instance
    
    private lateinit var scheduleAdapter: DoctorScheduleAdapter
    private lateinit var upcomingAdapter: DoctorScheduleAdapter
    private lateinit var slotAdapter: DoctorSlotAdapter
    
    private var appointmentId: Int = -1
    private var nextPatientName: String? = null
    private var nextPatientId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard)

        sessionManager = SessionManager.getInstance(this)
        
        // Header
        findViewById<TextView>(R.id.tvDoctorName).text = "Dr. ${sessionManager.getUserName()}"

        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
        setupSocket()
        refreshData()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun setupRecyclerViews() {
        // Today's Schedule RV
        val rvSchedule = findViewById<RecyclerView>(R.id.rvSchedule)
        rvSchedule.layoutManager = LinearLayoutManager(this)
        scheduleAdapter = DoctorScheduleAdapter(
            onItemClick = { appointment -> openAppointmentDetails(appointment.id) },
            onStartConsultationClick = { appointment ->
                startConsultationForAppt(appointment.id, appointment.patientName, appointment.patientId, appointment.patientAge, appointment.patientGender)
            },
            onViewReportsClick = { patientId, apptId -> openPatientRecords(patientId, apptId) }
        )
        rvSchedule.adapter = scheduleAdapter

        // Upcoming Appointments RV
        val rvUpcoming = findViewById<RecyclerView>(R.id.rvUpcoming)
        rvUpcoming.layoutManager = LinearLayoutManager(this)
        upcomingAdapter = DoctorScheduleAdapter(
            onItemClick = { appointment -> openAppointmentDetails(appointment.id) },
            onViewReportsClick = { patientId, apptId -> openPatientRecords(patientId, apptId) }
        )
        rvUpcoming.adapter = upcomingAdapter

        // My Slots RV
        val rvSlots = findViewById<RecyclerView>(R.id.rvSlots)
        rvSlots.layoutManager = LinearLayoutManager(this)
        slotAdapter = DoctorSlotAdapter(
            onDeleteSlot = { slot -> showSlotActionConfirmation(slot, "delete") },
            onCancelSlot = { slot -> showSlotActionConfirmation(slot, "cancel") }
        )
        rvSlots.adapter = slotAdapter
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.qaStartConsultation).setOnClickListener {
            if (appointmentId != -1) startConsultation()
            else {
                Toast.makeText(this, "No upcoming consultation found", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DoctorAppointmentsActivity::class.java))
            }
        }
        findViewById<View>(R.id.btnStartConsultation).setOnClickListener { startConsultation() }
        findViewById<View>(R.id.tvViewAll).setOnClickListener {
            val intent = Intent(this, DoctorAppointmentsActivity::class.java)
            intent.putExtra("FILTER_TODAY", true)
            startActivity(intent)
        }
        findViewById<View>(R.id.qaViewAppointments).setOnClickListener {
            startActivity(Intent(this, DoctorAppointmentsActivity::class.java))
        }
        findViewById<View>(R.id.qaCreateSlotAvailability).setOnClickListener {
            startActivity(Intent(this, CreateSlotActivity::class.java))
        }
        findViewById<View>(R.id.qaReviewReports).setOnClickListener {
            startActivity(Intent(this, DoctorPatientsActivity::class.java))
        }
    }

    private fun observeViewModel() {
        val doctorId = sessionManager.getUserId()

        viewModel.upcomingAppointments.observe(this) { list ->
            val rv = findViewById<RecyclerView>(R.id.rvUpcoming)
            val empty = findViewById<TextView>(R.id.tvNoUpcomingAppointments)
            if (list.isNullOrEmpty()) {
                empty.visibility = View.VISIBLE
                rv.visibility = View.GONE
            } else {
                empty.visibility = View.GONE
                rv.visibility = View.VISIBLE
                upcomingAdapter.updateList(list)
            }
            updateMergedNextAppointment()
        }

        viewModel.todayAppointments.observe(this) { list ->
            val rv = findViewById<RecyclerView>(R.id.rvSchedule)
            val empty = findViewById<TextView>(R.id.tvNoAppointmentsToday)
            if (list.isNullOrEmpty()) {
                empty.visibility = View.VISIBLE
                rv.visibility = View.GONE
            } else {
                empty.visibility = View.GONE
                rv.visibility = View.VISIBLE
                scheduleAdapter.updateList(list)
            }
            
            // Update Stats immediately
            val allAppts = viewModel.allAppointments.value ?: emptyList()
            val todayTotal = allAppts.filter { 
                it.date == LocalDate.now().toString() 
            }
            
            val pendingCount = todayTotal.count { 
                !it.status.equals("Completed", true) && !it.consultationStatus.equals("Completed", true) 
            }
            val completedCount = todayTotal.count { 
                it.status.equals("Completed", true) || it.consultationStatus.equals("Completed", true) 
            }
            
            findViewById<TextView>(R.id.tvPatientsCount).text = todayTotal.size.toString()
            findViewById<TextView>(R.id.tvPendingCount).text = pendingCount.toString()
            
            // Debug Logs matching constraints
            Log.d("DoctorDashboard", "FETCHING APPOINTMENTS")
            Log.d("DoctorDashboard", "UPDATED LIST SIZE: ${allAppts.size}")
            Log.d("DoctorDashboard", "UPCOMING COUNT: ${list?.size ?: 0}")
            Log.d("DoctorDashboard", "COMPLETED COUNT: $completedCount")
            
            updateMergedNextAppointment()
        }

        viewModel.availabilitySlots.observe(this) { list ->
            val rv = findViewById<RecyclerView>(R.id.rvSlots)
            val empty = findViewById<TextView>(R.id.tvNoSlots)
            if (list.isNullOrEmpty()) {
                empty.visibility = View.VISIBLE
                rv.visibility = View.GONE
            } else {
                empty.visibility = View.GONE
                rv.visibility = View.VISIBLE
                slotAdapter.updateList(list)
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun updateMergedNextAppointment() {
        val todayList = viewModel.todayAppointments.value ?: emptyList()
        val upcomingList = viewModel.upcomingAppointments.value ?: emptyList()
        
        val nextAppt = todayList.firstOrNull() ?: upcomingList.firstOrNull()
        
        if (nextAppt != null) {
            appointmentId = nextAppt.id
            nextPatientName = nextAppt.patientName
            nextPatientId = nextAppt.patientId
            val combinedUtc = "${nextAppt.date} ${nextAppt.time}"
            findViewById<TextView>(R.id.tvNextPatientName).text = nextAppt.patientName ?: "Patient"
            findViewById<TextView>(R.id.tvNextAppointmentTime).text = TimeUtils.convertUtcToLocal(combinedUtc, outputPattern = "hh:mm a")
            findViewById<View>(R.id.cardNextAppt).visibility = View.VISIBLE
            
            try {
                val isToday = todayList.contains(nextAppt)
                if (isToday) {
                    val apptTime = LocalTime.parse(nextAppt.time)
                    val now = LocalTime.now()
                    if (apptTime.isAfter(now)) {
                        val diff = java.time.Duration.between(now, apptTime).toMinutes()
                        findViewById<TextView>(R.id.tvInTime).text = "in $diff mins"
                        findViewById<TextView>(R.id.tvInTime).visibility = View.VISIBLE
                    } else {
                        findViewById<TextView>(R.id.tvInTime).visibility = View.GONE
                    }
                } else {
                    findViewById<TextView>(R.id.tvInTime).text = TimeUtils.formatSimpleDate(nextAppt.date)
                    findViewById<TextView>(R.id.tvInTime).visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                findViewById<TextView>(R.id.tvInTime).visibility = View.GONE
            }
        } else {
            appointmentId = -1
            findViewById<View>(R.id.cardNextAppt).visibility = View.GONE
        }
    }

    private fun setupSocket() {
        try {
            val socket = SocketService.socket ?: return
            val doctorId = sessionManager.getUserId()
            
            socket.emit("join_room", "doctor_$doctorId")
            
            val refreshAction = { runOnUiThread { refreshData() } }
            socket.on("new_appointment") { refreshAction() }
            socket.on("appointment_updated") { refreshAction() }
            socket.on("appointment_reassigned") { refreshAction() }
            socket.on("appointment_cancelled") { refreshAction() }
            socket.on("consultation_started") { refreshAction() }
            socket.on("consultation_ended") { refreshAction() }
            socket.on("prescription_created") { refreshAction() }
            socket.on("medical_record_shared") { refreshAction() }
            socket.on("medical_record_uploaded") { refreshAction() }
        } catch (e: Exception) {
            Log.e("DoctorDashboard", "Socket error: ${e.message}")
        }
    }

    private fun refreshData() {
        val doctorId = sessionManager.getUserId()
        if (doctorId != -1) {
            viewModel.cleanupSlots(doctorId, "doctor")
            viewModel.fetchAppointments(doctorId, "doctor")
            viewModel.fetchSlots(doctorId)
        }
    }

    private fun startConsultation() {
        if (appointmentId == -1) return
        
        val list = viewModel.todayAppointments.value.orEmpty() + viewModel.upcomingAppointments.value.orEmpty()
        val appt = list.find { it.id == appointmentId }
        
        startConsultationForAppt(appointmentId, nextPatientName, nextPatientId, appt?.patientAge, appt?.patientGender)
    }

    private fun startConsultationForAppt(apptId: Int, patientName: String?, patientId: Int?, patientAge: Int? = null, patientGender: String? = null) {
        val req = ConsultationStartRequest(apptId, sessionManager.getUserId())
        apiService.startConsultation(req).enqueue(object : Callback<ConsultationStartResponse> {
            override fun onResponse(call: Call<ConsultationStartResponse>, response: Response<ConsultationStartResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val resBody = response.body()!!
                    Log.d("Consultation", "START API CALLED")
                    Log.d("Consultation", "STATUS RESPONSE: ${resBody.status}")
                    
                    apiService.getConsultationStatus(apptId).enqueue(object : Callback<com.simats.tmapp.api.ConsultationStatusResponse> {
                        override fun onResponse(call: Call<com.simats.tmapp.api.ConsultationStatusResponse>, statusResponse: Response<com.simats.tmapp.api.ConsultationStatusResponse>) {
                            if (statusResponse.isSuccessful && statusResponse.body() != null) {
                                val statusBody = statusResponse.body()!!
                                Log.d("Consultation", "STATUS RESPONSE: ${statusBody.status}")
                                if (statusBody.status == "ready" && statusBody.canJoin == true) {
                                    val intent = Intent(this@DoctorDashboardActivity, VideoConsultationActivity::class.java)
                                    intent.putExtra("appointment_id", apptId)
                                    intent.putExtra("consultation_id", resBody.consultationId)
                                    intent.putExtra("channel_name", resBody.channel)
                                    intent.putExtra("patient_name", patientName ?: "Patient")
                                    intent.putExtra("doctor_id", sessionManager.getUserId())
                                    patientId?.let { intent.putExtra("patient_id", it) }
                                    patientAge?.let { intent.putExtra("patient_age", it) }
                                    patientGender?.let { intent.putExtra("patient_gender", it) }
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this@DoctorDashboardActivity, "Consultation not ready", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        override fun onFailure(call: Call<com.simats.tmapp.api.ConsultationStatusResponse>, t: Throwable) {
                            Toast.makeText(this@DoctorDashboardActivity, "Status Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    Toast.makeText(this@DoctorDashboardActivity, "Failed to start consultation", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ConsultationStartResponse>, t: Throwable) {
                Toast.makeText(this@DoctorDashboardActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openPatientRecords(patientId: Int, apptId: Int = -1) {
        if (patientId == -1) {
            Toast.makeText(this, "Patient ID not available", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, MedicalRecordsActivity::class.java)
        intent.putExtra("PATIENT_ID", patientId)
        intent.putExtra("APPOINTMENT_ID", apptId)
        startActivity(intent)
    }

    private fun openAppointmentDetails(apptId: Int) {
        val intent = Intent(this, AppointmentDetailsActivity::class.java)
        intent.putExtra("APPOINTMENT_ID", apptId)
        startActivity(intent)
    }

    private fun showSlotActionConfirmation(slot: AvailabilityResponse, action: String) {
        val msg = if (action == "delete") "Are you sure you want to delete this slot?" else "Are you sure you want to cancel this slot?"
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG)
            .setAction("Confirm") {
                viewModel.deleteSlot(slot.id, sessionManager.getUserId(), "doctor")
            }.show()
    }
}
