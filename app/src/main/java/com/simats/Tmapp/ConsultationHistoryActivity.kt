package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.AppointmentResponse
import com.simats.Tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ConsultationHistoryActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var rvHistory: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var adapter: PatientHistoryAdapter
    private var allHistory: List<AppointmentResponse> = emptyList()
    private var currentFilter = "All"
    private var currentQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation_history)

        sessionManager = SessionManager.getInstance(this)
        
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        rvHistory = findViewById(R.id.rvHistory)
        llEmptyState = findViewById(R.id.llEmptyState)
        val etSearch = findViewById<EditText>(R.id.etSearch)

        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = PatientHistoryAdapter(emptyList(), sessionManager.getUserId()) { appointment ->
            val intent = Intent(this, ViewPrescriptionActivity::class.java)
            intent.putExtra("appointment_id", appointment.id)
            intent.putExtra("doctor_id", appointment.doctorId)
            intent.putExtra("doctor_name", appointment.doctorName)
            intent.putExtra("doctor_specialization", appointment.specialization)
            intent.putExtra("doctor_photo", appointment.doctorImage)
            startActivity(intent)
        }
        rvHistory.adapter = adapter

        setupFilters()
        fetchHistory()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s.toString()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilters() {
        val tvAll = findViewById<TextView>(R.id.tvFilterAll)
        val tvCompleted = findViewById<TextView>(R.id.tvFilterCompleted)
        val tvCancelled = findViewById<TextView>(R.id.tvFilterCancelled)
        val tvMissed = findViewById<TextView>(R.id.tvFilterMissed)

        val filters = listOf(tvAll, tvCompleted, tvCancelled, tvMissed)

        filters.forEach { tv ->
            tv.setOnClickListener {
                currentFilter = tv.text.toString()
                
                // Update UI state
                filters.forEach { 
                    it.setBackgroundResource(R.drawable.bg_filter_chip_inactive)
                    it.setTextColor(android.graphics.Color.parseColor("#64748B"))
                    it.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
                tv.setBackgroundResource(R.drawable.bg_filter_chip_active)
                tv.setTextColor(android.graphics.Color.WHITE)
                tv.setTypeface(null, android.graphics.Typeface.BOLD)
                
                applyFilters()
            }
        }
    }

    private fun fetchHistory() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()

        // Fetch statuses in sequence
        ApiClient.instance.getMyAppointments(userId, role, "Completed").enqueue(object : Callback<List<AppointmentResponse>> {
            override fun onResponse(call: Call<List<AppointmentResponse>>, response: Response<List<AppointmentResponse>>) {
                val completed = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
                
                ApiClient.instance.getMyAppointments(userId, role, "Cancelled").enqueue(object : Callback<List<AppointmentResponse>> {
                    override fun onResponse(call: Call<List<AppointmentResponse>>, response: Response<List<AppointmentResponse>>) {
                        val cancelled = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
                        
                        ApiClient.instance.getMyAppointments(userId, role, "Missed").enqueue(object : Callback<List<AppointmentResponse>> {
                            override fun onResponse(call: Call<List<AppointmentResponse>>, response: Response<List<AppointmentResponse>>) {
                                val missed = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
                                allHistory = (completed + cancelled + missed).sortedByDescending { it.date ?: "" }
                                applyFilters()
                            }
                            override fun onFailure(call: Call<List<AppointmentResponse>>, t: Throwable) {
                                allHistory = (completed + cancelled).sortedByDescending { it.date ?: "" }
                                applyFilters()
                            }
                        })
                    }
                    override fun onFailure(call: Call<List<AppointmentResponse>>, t: Throwable) {
                        allHistory = completed.sortedByDescending { it.date ?: "" }
                        applyFilters()
                    }
                })
            }
            override fun onFailure(call: Call<List<AppointmentResponse>>, t: Throwable) {
                showEmpty(true)
            }
        })
    }

    private fun applyFilters() {
        var filtered = allHistory

        // Apply Status Filter
        if (currentFilter != "All") {
            filtered = filtered.filter { it.status?.equals(currentFilter, true) == true }
        }

        // Apply Search Filter
        if (currentQuery.isNotEmpty()) {
            filtered = filtered.filter { 
                (it.doctorName?.contains(currentQuery, true) == true) || 
                (it.date?.contains(currentQuery, true) == true)
            }
        }

        adapter.updateList(filtered)
        showEmpty(filtered.isEmpty())
    }

    private fun showEmpty(show: Boolean) {
        rvHistory.visibility = if (show) View.GONE else View.VISIBLE
        llEmptyState.visibility = if (show) View.VISIBLE else View.GONE
    }
}
