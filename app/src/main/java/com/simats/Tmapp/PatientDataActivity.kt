package com.simats.Tmapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.ApiClient

class PatientDataActivity : AppCompatActivity() {

    private lateinit var tabInfo: LinearLayout
    private lateinit var tabHistory: LinearLayout
    private lateinit var tabReports: LinearLayout

    private lateinit var tvTabInfo: TextView
    private lateinit var tvTabHistory: TextView
    private lateinit var tvTabReports: TextView

    private lateinit var indicatorInfo: View
    private lateinit var indicatorHistory: View
    private lateinit var indicatorReports: View

    private lateinit var contentInfo: View
    private lateinit var contentHistory: View
    private lateinit var contentReports: View

    private lateinit var reportsAdapter: MedicalReportAdapter
    private var patientId: Int = -1
    private var appointmentId: Int = -1
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_data)

        initializeViews()
        setupBottomNav()
        setupTabs()

        val targetTab = intent.getIntExtra("target_tab", 0)
        switchTab(targetTab)

        findViewById<View>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        findViewById<View>(R.id.ivClose).setOnClickListener {
            onBackPressed()
        }

        findViewById<MaterialButton>(R.id.btnCreatePrescription).setOnClickListener {
            val intent = Intent(this, NewPrescriptionActivity::class.java)
            intent.putExtra("appointment_id", -1) // Should pass real ID if avail
            startActivity(intent)
        }

        sessionManager = SessionManager.getInstance(this)
        patientId = intent.getIntExtra("patient_id", -1)
        appointmentId = intent.getIntExtra("appointment_id", -1)
        val consultationId = intent.getIntExtra("consultation_id", -1)

        reportsAdapter = MedicalReportAdapter(
            sessionManager.getUserRole().lowercase(),
            onItemClick = { report -> openRecord(report) },
            onDownloadClick = { report -> openRecord(report) },
            onShareClick = { /* No sharing from this screen for now */ }
        )
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvReportsInPatientData).adapter = reportsAdapter

        findViewById<View>(R.id.cvUploadReportSection).setOnClickListener {
            val intent = Intent(this, MedicalRecordsActivity::class.java)
            intent.putExtra("patient_id", patientId)
            startActivity(intent)
        }
    }

    private fun initializeViews() {
        tabInfo = findViewById(R.id.tabInfo)
        tabHistory = findViewById(R.id.tabHistory)
        tabReports = findViewById(R.id.tabReports)

        tvTabInfo = findViewById(R.id.tvTabInfo)
        tvTabHistory = findViewById(R.id.tvTabHistory)
        tvTabReports = findViewById(R.id.tvTabReports)

        indicatorInfo = findViewById(R.id.indicatorInfo)
        indicatorHistory = findViewById(R.id.indicatorHistory)
        indicatorReports = findViewById(R.id.indicatorReports)

        contentInfo = findViewById(R.id.contentInfo)
        contentHistory = findViewById(R.id.contentHistory)
        contentReports = findViewById(R.id.contentReports)
    }

    private fun setupTabs() {
        tabInfo.setOnClickListener { switchTab(0) }
        tabHistory.setOnClickListener { switchTab(1) }
        tabReports.setOnClickListener { switchTab(2) }
    }

    private fun switchTab(index: Int) {
        // Reset all
        tvTabInfo.setTextColor(Color.parseColor("#64748B"))
        tvTabHistory.setTextColor(Color.parseColor("#64748B"))
        tvTabReports.setTextColor(Color.parseColor("#64748B"))
        tvTabInfo.typeface = android.graphics.Typeface.DEFAULT
        tvTabHistory.typeface = android.graphics.Typeface.DEFAULT
        tvTabReports.typeface = android.graphics.Typeface.DEFAULT

        indicatorInfo.visibility = View.INVISIBLE
        indicatorHistory.visibility = View.INVISIBLE
        indicatorReports.visibility = View.INVISIBLE

        contentInfo.visibility = View.GONE
        contentHistory.visibility = View.GONE
        contentReports.visibility = View.GONE

        // Set active
        when (index) {
            0 -> {
                tvTabInfo.setTextColor(Color.parseColor("#2F6FED"))
                tvTabInfo.typeface = android.graphics.Typeface.DEFAULT_BOLD
                indicatorInfo.visibility = View.VISIBLE
                contentInfo.visibility = View.VISIBLE
                if (patientId != -1) loadPatientProfile(patientId)
            }
            1 -> {
                tvTabHistory.setTextColor(Color.parseColor("#2F6FED"))
                tvTabHistory.typeface = android.graphics.Typeface.DEFAULT_BOLD
                indicatorHistory.visibility = View.VISIBLE
                contentHistory.visibility = View.VISIBLE
                if (patientId != -1) loadPatientHistory(patientId)
            }
            2 -> {
                tvTabReports.setTextColor(Color.parseColor("#2F6FED"))
                tvTabReports.typeface = android.graphics.Typeface.DEFAULT_BOLD
                indicatorReports.visibility = View.VISIBLE
                contentReports.visibility = View.VISIBLE
                if (patientId != -1) loadPatientReports(patientId)
            }
        }
    }

    private fun loadPatientProfile(patientId: Int) {
        ApiClient.instance.getPatientProfileV2(patientId).enqueue(object : retrofit2.Callback<com.simats.Tmapp.api.PatientProfileV2Response> {
            override fun onResponse(call: retrofit2.Call<com.simats.Tmapp.api.PatientProfileV2Response>, response: retrofit2.Response<com.simats.Tmapp.api.PatientProfileV2Response>) {
                if (response.isSuccessful && response.body() != null) {
                    val p = response.body()!!
                    findViewById<TextView>(R.id.tvInfoName).text = p.name
                    findViewById<TextView>(R.id.tvInfoAgeGender).text = "Age: ${p.age ?: "--"} | Gender: ${p.gender ?: "--"}"
                    findViewById<TextView>(R.id.tvInfoBloodGroup).text = "Blood Group: ${p.bloodGroup ?: "--"}"
                }
            }
            override fun onFailure(call: retrofit2.Call<com.simats.Tmapp.api.PatientProfileV2Response>, t: Throwable) {}
        })
    }

    private fun loadPatientHistory(patientId: Int) {
        ApiClient.instance.getPatientHistory(patientId).enqueue(object : retrofit2.Callback<List<com.simats.Tmapp.api.PatientHistoryResponse>> {
            override fun onResponse(call: retrofit2.Call<List<com.simats.Tmapp.api.PatientHistoryResponse>>, response: retrofit2.Response<List<com.simats.Tmapp.api.PatientHistoryResponse>>) {
                if (response.isSuccessful) {
                    val history = response.body() ?: emptyList()
                    val container = findViewById<LinearLayout>(R.id.llHistoryContainer)
                    container.removeAllViews()
                    for (h in history) {
                        val view = layoutInflater.inflate(R.layout.item_patient_history, null)
                        view.findViewById<TextView>(R.id.tvHistoryDate).text = h.date
                        view.findViewById<TextView>(R.id.tvHistoryDiagnosis).text = h.diagnosis
                        container.addView(view)
                    }
                }
            }
            override fun onFailure(call: retrofit2.Call<List<com.simats.Tmapp.api.PatientHistoryResponse>>, t: Throwable) {}
        })
    }

    private fun loadPatientReports(patientId: Int) {
        ApiClient.instance.getPatientReportsV2(patientId).enqueue(object : retrofit2.Callback<List<com.simats.Tmapp.api.PatientReportResponse>> {
            override fun onResponse(call: retrofit2.Call<List<com.simats.Tmapp.api.PatientReportResponse>>, response: retrofit2.Response<List<com.simats.Tmapp.api.PatientReportResponse>>) {
                if (response.isSuccessful) {
                    val reports = response.body() ?: emptyList()
                    // Map to MedicalRecordResponse for adapter
                    val records = reports.map { 
                        com.simats.Tmapp.api.MedicalRecordResponse(
                            id = it.fileUrl.split("/").last().toIntOrNull() ?: 0,
                            patientId = patientId,
                            fileName = it.reportName,
                            filePath = it.fileUrl,
                            createdAt = ""
                        )
                    }
                    reportsAdapter.submitList(records)
                }
            }
            override fun onFailure(call: retrofit2.Call<List<com.simats.Tmapp.api.PatientReportResponse>>, t: Throwable) {
                Toast.makeText(this@PatientDataActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openRecord(record: com.simats.Tmapp.api.MedicalRecordResponse) {
        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()
        val url = "$baseUrl/api/medical-record/download/${record.id}?user_id=$userId&role=$role"
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
        startActivity(intent)
    }

    private fun openReportViewer(title: String) {
        val intent = Intent(this, ReportViewerActivity::class.java)
        intent.putExtra("REPORT_TITLE", title)
        startActivity(intent)
    }

    private fun setupBottomNav() {
        // Implementation for bottom nav if needed, or it's handled by GlobalBottomNavigationView
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right)
    }
}
