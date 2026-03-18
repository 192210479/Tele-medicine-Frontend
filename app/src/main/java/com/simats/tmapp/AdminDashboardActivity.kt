package com.simats.tmapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.simats.tmapp.api.*
import java.util.Locale

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: AdminDashboardViewModel
    private lateinit var notificationViewModel: NotificationViewModel

    // Views for Stats
    private var totalPatientsText: TextView? = null
    private var activeDoctorsText: TextView? = null
    private var totalAppointmentsText: TextView? = null
    private var totalRevenueText: TextView? = null
    private var tvPendingCount: TextView? = null
    private var patientsGrowthText: TextView? = null
    private var revenueGrowthText: TextView? = null
    private var newDoctorsText: TextView? = null
    private var todayAppointmentsText: TextView? = null

    // Charts
    private lateinit var revenueLineChart: LineChart
    private lateinit var appointmentsBarChart: BarChart
    private lateinit var doctorActivityChart: BarChart
    private lateinit var patientRegistrationsChart: BarChart

    // Content Layouts
    private var llDoctorsContent: LinearLayout? = null
    private var llPatientsContent: LinearLayout? = null
    private var llAppointmentsContainer: View? = null
    private var rvAppointments: androidx.recyclerview.widget.RecyclerView? = null
    private var appointmentsAdapter: AdminAppointmentAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        sessionManager = SessionManager.getInstance(this)
        viewModel = ViewModelProvider(this)[AdminDashboardViewModel::class.java]
        notificationViewModel = ViewModelProvider(this)[NotificationViewModel::class.java]

        // Role Verification
        val currentRole = sessionManager.getUserRole().lowercase()
        if (!currentRole.contains("admin")) {
            val intent = Intent(this, sessionManager.getDashboardActivity())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        initializeViews()
        setupClickListeners()
        setupObservers()
        setupSocketListeners()

        // Initial Data Load
        viewModel.loadDashboardData()
        notificationViewModel.loadNotifications()
    }

    private fun initializeViews() {
        totalPatientsText = findViewById(R.id.tvTotalPatients)
        activeDoctorsText = findViewById(R.id.tvTotalDoctors)
        totalAppointmentsText = findViewById(R.id.tvTotalAppointments)
        totalRevenueText = findViewById(R.id.tvTotalRevenue)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        patientsGrowthText = findViewById(R.id.patientsGrowthText)
        revenueGrowthText = findViewById(R.id.revenueGrowthText)
        newDoctorsText = findViewById(R.id.newDoctorsText)
        todayAppointmentsText = findViewById(R.id.todayAppointmentsText)

        revenueLineChart = findViewById(R.id.revenueLineChart)
        appointmentsBarChart = findViewById(R.id.appointmentsBarChart)
        doctorActivityChart = findViewById(R.id.doctorActivityChart)
        patientRegistrationsChart = findViewById(R.id.patientRegistrationsChart)

        llDoctorsContent = findViewById(R.id.llDoctorsContent)
        llPatientsContent = findViewById(R.id.llPatientsContent)
        llAppointmentsContainer = findViewById(R.id.llAppointmentsContainer)
        rvAppointments = findViewById(R.id.rvAppointments)

        // Initialize RecyclerView for Appointments list
        appointmentsAdapter = AdminAppointmentAdapter(emptyList(), { _ ->
            Toast.makeText(this, "Click Manage to Cancel", Toast.LENGTH_SHORT).show()
        }, { _ ->
            Toast.makeText(this, "Click Manage to Reassign", Toast.LENGTH_SHORT).show()
        })
        rvAppointments?.adapter = appointmentsAdapter
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.btnNotifications)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        findViewById<View>(R.id.pendingApprovalsCard)?.setOnClickListener {
            startActivity(Intent(this, DoctorApprovalActivity::class.java))
        }

        findViewById<View>(R.id.btnManageAppointments)?.setOnClickListener {
            startActivity(Intent(this, AdminAppointmentsActivity::class.java))
        }

        val tabDoctors = findViewById<TextView>(R.id.tabDoctors)
        val tabPatients = findViewById<TextView>(R.id.tabPatients)
        val tabAppointmentsList = findViewById<TextView>(R.id.tabAppointmentsList)

        tabDoctors?.setOnClickListener { updateTabs("doctors") }
        tabPatients?.setOnClickListener { updateTabs("patients") }
        tabAppointmentsList?.setOnClickListener { updateTabs("appointments") }
    }

    private fun updateTabs(selectedTab: String) {
        val tabDoctors = findViewById<TextView>(R.id.tabDoctors)
        val tabPatients = findViewById<TextView>(R.id.tabPatients)
        val tabAppointmentsList = findViewById<TextView>(R.id.tabAppointmentsList)

        val defaultColor = Color.parseColor("#64748B")
        val activeColor = ContextCompat.getColor(this, R.color.primary_blue)

        // Reset all tabs
        listOf(tabDoctors, tabPatients, tabAppointmentsList).forEach {
            it?.setBackgroundColor(Color.TRANSPARENT)
            it?.setTextColor(defaultColor)
            it?.setTypeface(null, Typeface.NORMAL)
        }
        
        llDoctorsContent?.visibility = View.GONE
        llPatientsContent?.visibility = View.GONE
        llAppointmentsContainer?.visibility = View.GONE

        when (selectedTab) {
            "doctors" -> {
                tabDoctors?.setBackgroundResource(R.drawable.bg_tab_selected_light)
                tabDoctors?.setTextColor(activeColor)
                tabDoctors?.setTypeface(null, Typeface.BOLD)
                llDoctorsContent?.visibility = View.VISIBLE
                viewModel.loadDoctors()
            }
            "patients" -> {
                tabPatients?.setBackgroundResource(R.drawable.bg_tab_selected_light)
                tabPatients?.setTextColor(activeColor)
                tabPatients?.setTypeface(null, Typeface.BOLD)
                llPatientsContent?.visibility = View.VISIBLE
                viewModel.loadPatients()
            }
            "appointments" -> {
                tabAppointmentsList?.setBackgroundResource(R.drawable.bg_tab_selected_light)
                tabAppointmentsList?.setTextColor(activeColor)
                tabAppointmentsList?.setTypeface(null, Typeface.BOLD)
                llAppointmentsContainer?.visibility = View.VISIBLE
                viewModel.loadAppointments(sessionManager.getUserId())
            }
        }
    }

    private fun setupObservers() {
        viewModel.summary.observe(this) { summary ->
            summary?.let {
                totalPatientsText?.text = it.patients.toString()
                totalRevenueText?.text = String.format(Locale.getDefault(), "₹%,.0f", it.revenue)
                activeDoctorsText?.text = it.activeDoctors.toString()
                totalAppointmentsText?.text = it.appointments.toString()
            }
        }

        viewModel.revenueTrend.observe(this) { trend ->
            trend?.let { setupRevenueChart(it) }
        }

        viewModel.weeklyAppointments.observe(this) { weekly ->
            weekly?.let { setupAppointmentsChart(it) }
        }

        viewModel.doctorActivity.observe(this) { activity ->
            activity?.let { setupDoctorActivityChart(it) }
        }

        viewModel.patientRegistrations.observe(this) { registrations ->
            registrations?.let { setupPatientRegistrationsChart(it) }
        }

        viewModel.pendingDoctors.observe(this) { doctors ->
            val count = doctors?.size ?: 0
            tvPendingCount?.text = String.format(Locale.getDefault(), "%d doctors waiting for review", count)
        }

        viewModel.allDoctors.observe(this) { doctors ->
            doctors?.let { populateDoctorsList(it) }
        }

        viewModel.allPatients.observe(this) { patients ->
            patients?.let { populatePatientsList(it) }
        }

        viewModel.allAppointments.observe(this) { appts ->
            appts?.let { list ->
                // Clear old data first
                appointmentsAdapter?.updateData(emptyList())

                val adminAppts = list.map { a ->
                    AdminAppointmentResponse(
                        a.id, a.doctorId, 0, 
                        a.doctorName ?: "Unknown Doctor", 
                        a.patientName ?: "Patient ${a.id}", 
                        a.date ?: "TBD", 
                        a.time ?: "N/A", 
                        a.status ?: "Pending",
                        a.consultationStatus
                    )
                }
                
                // Group by Doctor Name
                val groupedList = mutableListOf<Any>()
                val groupedByDoctor = adminAppts.groupBy { it.doctorName ?: "Unknown Doctor" }
                
                for ((doctorName, appointments) in groupedByDoctor) {
                    groupedList.add(doctorName) // Header
                    groupedList.addAll(appointments) // Items
                }

                appointmentsAdapter?.updateData(groupedList)
            }
        }

        notificationViewModel.unreadCount.observe(this) { count ->
            val badge = findViewById<View>(R.id.vNotificationBadge)
            badge?.visibility = if (count > 0) View.VISIBLE else View.GONE
        }

        viewModel.patientsGrowth.observe(this) { patientsGrowthText?.text = it }
        viewModel.revenueGrowth.observe(this) { revenueGrowthText?.text = it }
        viewModel.newDoctorsCount.observe(this) { newDoctorsText?.text = it }
        viewModel.todayAppointmentsCount.observe(this) { todayAppointmentsText?.text = it }
    }

    private fun setupRevenueChart(trend: List<RevenueTrend>) {
        revenueLineChart.clear()
        val labels = trend.map { it.month }
        val entries = trend.mapIndexed { index, data -> Entry(index.toFloat(), data.amount.toFloat()) }
        
        if (entries.isNotEmpty()) {
            val dataSet = LineDataSet(entries, "Revenue")
            dataSet.apply {
                color = Color.parseColor("#3B82F6")
                setCircleColor(Color.parseColor("#3B82F6"))
                lineWidth = 2f
                setDrawFilled(true)
                fillColor = Color.parseColor("#EFF6FF")
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawValues(false)
            }
            revenueLineChart.data = LineData(dataSet)
        }

        revenueLineChart.apply {
            marker = CustomMarkerView(this@AdminDashboardActivity, R.layout.marker_view, labels)
            isHighlightPerTapEnabled = true
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            animateX(1000)
            invalidate()
        }
    }

    private fun setupAppointmentsChart(weekly: List<WeeklyAppointments>) {
        appointmentsBarChart.clear()
        if (weekly.isEmpty()) {
            appointmentsBarChart.invalidate()
            return
        }

        val labels = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dataMap = weekly.associate { it.day to it.count }
        val entries = labels.mapIndexed { index, label ->
            BarEntry(index.toFloat(), (dataMap[label] ?: 0).toFloat())
        }

        val dataSet = BarDataSet(entries, "Appointments")
        dataSet.color = Color.parseColor("#2DD4BF")
        dataSet.setDrawValues(false)

        appointmentsBarChart.apply {
            data = BarData(dataSet)
            marker = CustomMarkerView(this@AdminDashboardActivity, R.layout.marker_view, labels.toList())
            isHighlightPerTapEnabled = true
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun setupDoctorActivityChart(activity: List<DoctorActivity>) {
        doctorActivityChart.clear()
        if (activity.isEmpty()) {
            doctorActivityChart.invalidate()
            return
        }

        val labels = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dataMap = activity.associate { it.day to it.count }
        val entries = labels.mapIndexed { index, label ->
            BarEntry(index.toFloat(), (dataMap[label] ?: 0).toFloat())
        }

        val dataSet = BarDataSet(entries, "Activity")
        dataSet.color = Color.parseColor("#6366F1")
        dataSet.setDrawValues(false)

        doctorActivityChart.apply {
            data = BarData(dataSet)
            marker = CustomMarkerView(this@AdminDashboardActivity, R.layout.marker_view, labels.toList())
            isHighlightPerTapEnabled = true
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun setupPatientRegistrationsChart(registrations: List<PatientRegistration>) {
        patientRegistrationsChart.clear()
        if (registrations.isEmpty()) {
            patientRegistrationsChart.invalidate()
            return
        }

        val labels = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dataMap = registrations.associate { it.day to it.count }
        val entries = labels.mapIndexed { index, label ->
            BarEntry(index.toFloat(), (dataMap[label] ?: 0).toFloat())
        }

        val dataSet = BarDataSet(entries, "Registrations")
        dataSet.color = Color.parseColor("#F59E0B")
        dataSet.setDrawValues(false)

        patientRegistrationsChart.apply {
            data = BarData(dataSet)
            marker = CustomMarkerView(this@AdminDashboardActivity, R.layout.marker_view, labels.toList())
            isHighlightPerTapEnabled = true
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun populateDoctorsList(doctors: List<Doctor>) {
        llDoctorsContent?.removeAllViews()
        doctors.take(8).forEach { doctor ->
            if (doctor != null) {
                val itemView = layoutInflater.inflate(R.layout.item_admin_doctor_mini, llDoctorsContent, false)
                val tvName = itemView.findViewById<TextView>(R.id.tvName)
                if (!doctor.name.isNullOrEmpty()) {
                    tvName.text = "Dr. ${doctor.name}"
                } else {
                    tvName.text = "Unknown Doctor"
                }
                itemView.findViewById<TextView>(R.id.tvSpecialty).text = doctor.specialization ?: "General"
                itemView.findViewById<TextView>(R.id.tvStatus).text = (doctor.status ?: "Pending").replaceFirstChar { it.uppercase() }
                llDoctorsContent?.addView(itemView)
            }
        }
    }

    private fun populatePatientsList(patients: List<Patient>) {
        llPatientsContent?.removeAllViews()
        patients.take(10).forEach { patient ->
            val itemView = layoutInflater.inflate(R.layout.item_admin_patient_mini, llPatientsContent, false)
            itemView.findViewById<TextView>(R.id.tvName).text = patient.name ?: "Patient ${patient.id}"
            itemView.findViewById<TextView>(R.id.tvEmail).text = String.format(Locale.getDefault(), "Age: %d", patient.age)
            llPatientsContent?.addView(itemView)
        }
    }

    private fun setupSocketListeners() {
        val refresh = {
            runOnUiThread {
                viewModel.loadDashboardData()
                viewModel.loadDoctors()
                viewModel.loadPatients()
                viewModel.loadAppointments(sessionManager.getUserId())
                notificationViewModel.loadNotifications()
            }
        }

        SocketService.socket?.on("new_appointment") { refresh() }
        SocketService.socket?.on("doctor_registration") { refresh() }
        SocketService.socket?.on("doctor_approved") { refresh() }
        SocketService.socket?.on("doctor_status_updated") { refresh() }
        SocketService.socket?.on("appointment_updated") { refresh() }
        SocketService.socket?.on("appointment_reassigned") { refresh() }
        SocketService.socket?.on("appointment_cancelled") { refresh() }
        SocketService.socket?.on("consultation_ended") { refresh() }
        SocketService.socket?.on("patient_updated") { refresh() }
        SocketService.socket?.on("notification") { refresh() }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDashboardData()
        notificationViewModel.loadNotifications()
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketService.socket?.off("new_appointment")
        SocketService.socket?.off("doctor_registration")
        SocketService.socket?.off("doctor_approved")
        SocketService.socket?.off("doctor_status_updated")
        SocketService.socket?.off("appointment_updated")
        SocketService.socket?.off("appointment_reassigned")
        SocketService.socket?.off("appointment_cancelled")
        SocketService.socket?.off("consultation_ended")
        SocketService.socket?.off("patient_updated")
        SocketService.socket?.off("notification")
    }
}
