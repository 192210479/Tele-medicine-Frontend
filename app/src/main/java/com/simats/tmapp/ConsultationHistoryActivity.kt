package com.simats.tmapp

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
import com.simats.tmapp.api.AppointmentResponse
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ConsultationHistoryActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var rvHistory: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var adapter: AppointmentsAdapter
    private var allHistory: List<AppointmentResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation_history)

        sessionManager = SessionManager.getInstance(this)
        val isDoctor = sessionManager.getUserRole().lowercase().contains("doctor")

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        rvHistory = findViewById(R.id.rvHistory)
        llEmptyState = findViewById(R.id.llEmptyState)
        val etSearch = findViewById<EditText>(R.id.etSearch)

        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = AppointmentsAdapter(emptyList(), isDoctor) { appointment ->
            // View Details or Prescription logic
            val intent = Intent(this, PrescriptionActivity::class.java)
            intent.putExtra("appointment_id", appointment.id)
            startActivity(intent)
        }
        rvHistory.adapter = adapter

        fetchHistory()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun fetchHistory() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()

        // Fetch Completed appointments
        ApiClient.instance.getMyAppointments(userId, role, "Completed").enqueue(object : Callback<List<AppointmentResponse>> {
            override fun onResponse(call: Call<List<AppointmentResponse>>, response: Response<List<AppointmentResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    val completed = response.body()!!
                    // Also fetch Cancelled
                    fetchCancelled(completed)
                } else {
                    showEmpty(true)
                }
            }

            override fun onFailure(call: Call<List<AppointmentResponse>>, t: Throwable) {
                showEmpty(true)
            }
        })
    }

    private fun fetchCancelled(completed: List<AppointmentResponse>) {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()

        ApiClient.instance.getMyAppointments(userId, role, "Cancelled").enqueue(object : Callback<List<AppointmentResponse>> {
            override fun onResponse(call: Call<List<AppointmentResponse>>, response: Response<List<AppointmentResponse>>) {
                val cancelled = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
                allHistory = (completed + cancelled).sortedByDescending { (it.date ?: "") + (it.time ?: "") }
                adapter.updateList(allHistory)
                showEmpty(allHistory.isEmpty())
            }

            override fun onFailure(call: Call<List<AppointmentResponse>>, t: Throwable) {
                allHistory = completed.sortedByDescending { (it.date ?: "") + (it.time ?: "") }
                adapter.updateList(allHistory)
                showEmpty(allHistory.isEmpty())
            }
        })
    }

    private fun filterList(query: String) {
        if (query.isEmpty()) {
            adapter.updateList(allHistory)
            return
        }
        val filtered = allHistory.filter { 
            (it.doctorName?.contains(query, true) == true) || 
            (it.patientName?.contains(query, true) == true) ||
            (it.date?.contains(query, true) == true)
        }
        adapter.updateList(filtered)
    }

    private fun showEmpty(show: Boolean) {
        rvHistory.visibility = if (show) View.GONE else View.VISIBLE
        llEmptyState.visibility = if (show) View.VISIBLE else View.GONE
    }
}
