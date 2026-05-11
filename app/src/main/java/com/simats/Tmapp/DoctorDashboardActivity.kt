package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.ConsultationStartResponse
import com.simats.Tmapp.api.ConsultationStartRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class DoctorDashboardActivity : AppCompatActivity() {

    private lateinit var viewModel: DoctorDashboardViewModel
    private lateinit var scheduleAdapter: DoctorScheduleAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard)

        sessionManager = SessionManager.getInstance(this)
        viewModel = ViewModelProvider(this).get(DoctorDashboardViewModel::class.java)

        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
        setupSocket()
        refreshData()
    }

    override fun onResume() {
        super.onResume()
        refreshData()

        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        val avatarVersion = sessionManager.getAvatarVersion()
        val avatarUrl = "$baseUrl/api/profile/image/${sessionManager.getUserId()}?role=doctor&v=$avatarVersion"

        AvatarUtils.loadAvatar(
            findViewById(R.id.ivAvatar),
            avatarUrl,
            sessionManager.getUserName().ifEmpty { "Doctor" }
        )
    }

    private fun setupRecyclerViews() {
        val rvSchedule = findViewById<RecyclerView>(R.id.rvSchedule)
        rvSchedule.layoutManager = LinearLayoutManager(this)
        scheduleAdapter = DoctorScheduleAdapter(
            onItemClick = { appt ->
                val intent = Intent(this, AppointmentDetailsActivity::class.java)
                intent.putExtra("appointment_id", appt.id)
                startActivity(intent)
            }
        )
        rvSchedule.adapter = scheduleAdapter
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.ivAvatar).setOnClickListener {
            startActivity(Intent(this, DoctorProfileSettingsActivity::class.java))
        }

        findViewById<View>(R.id.btnStartConsultation).setOnClickListener {
            startConsultation()
        }

        findViewById<View>(R.id.tvViewAll).setOnClickListener {
            val intent = Intent(this, DoctorAppointmentsActivity::class.java)
            intent.putExtra("FILTER_TODAY", true)
            startActivity(intent)
        }

        findViewById<View>(R.id.qaReviewReports).setOnClickListener {
            startActivity(Intent(this, DoctorPatientsActivity::class.java))
        }

        findViewById<View>(R.id.qaViewAppointments).setOnClickListener {
            startActivity(Intent(this, DoctorAppointmentsActivity::class.java))
        }

        findViewById<View>(R.id.qaStartConsultation).setOnClickListener {
            startConsultation()
        }

        // ✅ FIX FOR CREATE SLOT
        findViewById<View>(R.id.qaCreateSlotAvailability).setOnClickListener {
            startActivity(Intent(this, CreateSlotActivity::class.java))
        }

        findViewById<View>(R.id.flDoctorNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        findViewById<View>(R.id.llStats).setOnClickListener {
            startActivity(Intent(this, PaymentsActivity::class.java))
        }
    }

    private fun observeViewModel() {

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
            
        val todayVisible = list ?: emptyList()

        val pendingCount = todayVisible.count { appt ->
            val status = DoctorAppointmentHelper.getStatus(appt)
            status == DoctorAppointmentHelper.AppointmentStatus.UPCOMING
        }

        findViewById<TextView>(R.id.tvPatientsCount).text = todayVisible.size.toString()
        findViewById<TextView>(R.id.tvPendingCount).text = pendingCount.toString()
            
            updateMergedNextAppointment()
        }

        viewModel.doctorProfile.observe(this) { doctor ->
            if (doctor != null) {
                findViewById<TextView>(R.id.tvDoctorName).text = "Dr. ${doctor.name}"
            }
        }

        viewModel.dashboardData.observe(this) { data ->
            val greetingText = data?.greeting ?: "Hello"
            findViewById<TextView>(R.id.tvGreeting).text = "$greetingText,"
        }
    }

    private fun updateMergedNextAppointment() {
        val next = DoctorAppointmentHelper.getNextAppointment(viewModel.allAppointments.value ?: emptyList())
        val card = findViewById<View>(R.id.cardNextAppt)

        if (next == null) {
            card.visibility = View.GONE
        } else {
            card.visibility = View.VISIBLE

            val patientName = DoctorAppointmentHelper.cleanText(next.patientName, "Patient")
            val timeText = DoctorAppointmentHelper.cleanText(next.localTime ?: next.time, "--:--")
            val inTimeText = DoctorAppointmentHelper.getTimeUntil(next)

            findViewById<TextView>(R.id.tvNextPatientName).text = patientName
            findViewById<TextView>(R.id.tvNextAppointmentTime).text = timeText
            findViewById<TextView>(R.id.tvInTime).text = inTimeText

            val status = DoctorAppointmentHelper.getStatus(next)
            val btn = findViewById<View>(R.id.btnStartConsultation)
            btn.visibility = if (status == DoctorAppointmentHelper.AppointmentStatus.UPCOMING) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun refreshData() {
        val doctorId = sessionManager.getUserId()
        viewModel.loadDashboardData(doctorId)
    }

    private fun startConsultation() {
        val next = DoctorAppointmentHelper.getNextAppointment(viewModel.allAppointments.value ?: emptyList()) ?: return
        
        val req = ConsultationStartRequest(next.id, sessionManager.getUserId())
        ApiClient.instance.startConsultation(req).enqueue(object : Callback<ConsultationStartResponse> {
            override fun onResponse(call: Call<ConsultationStartResponse>, response: Response<ConsultationStartResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val resBody = response.body()!!
                    val intent = Intent(this@DoctorDashboardActivity, VideoConsultationActivity::class.java)
                    intent.putExtra("appointment_id", next.id)
                    intent.putExtra("consultation_id", resBody.consultationId)
                    intent.putExtra("channel_name", resBody.channel)
                    intent.putExtra("patient_name", next.patientName ?: "Patient")
                    intent.putExtra("doctor_id", sessionManager.getUserId())
                    next.patientId?.let { intent.putExtra("patient_id", it) }
                    next.patientAge?.let { intent.putExtra("patient_age", it) }
                    next.patientGender?.let { intent.putExtra("patient_gender", it) }
                    startActivity(intent)
                }
            }
            override fun onFailure(call: Call<ConsultationStartResponse>, t: Throwable) {}
        })
    }

    private fun setupSocket() {
        val socket = SocketService.socket ?: return
        val doctorId = sessionManager.getUserId()
        socket.emit("join_room", "doctor_$doctorId")
        
        val refresh = { runOnUiThread { refreshData() } }
        socket.on("new_appointment") { refresh() }
        socket.on("appointment_updated") { refresh() }
        socket.on("appointment_cancelled") { refresh() }
    }
}
