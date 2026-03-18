package com.simats.tmapp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.tmapp.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PatientPrescriptionsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppointmentsAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation_history) // Reuse history layout

        sessionManager = SessionManager.getInstance(this)
        findViewById<TextView>(R.id.tvTitle).text = "My Prescriptions"
        
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        recyclerView = findViewById(R.id.rvHistory)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fetchPrescriptions()
    }

    private fun fetchPrescriptions() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()

        ApiClient.instance.getMyAppointments(userId, role).enqueue(object : Callback<List<AppointmentResponse>> {
            override fun onResponse(call: Call<List<AppointmentResponse>>, response: Response<List<AppointmentResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    // Filter for completed appointments (only these can have prescriptions)
                    val completed = response.body()!!.filter { it.status == "Completed" }
                    if (completed.isEmpty()) {
                        findViewById<View>(R.id.llEmptyState).visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        findViewById<View>(R.id.llEmptyState).visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adapter = AppointmentsAdapter(completed, role == "doctor") { appointment ->
                            // Optional: details click handling
                        }
                        recyclerView.adapter = adapter
                    }
                } else {
                    Toast.makeText(this@PatientPrescriptionsActivity, "Error fetching prescriptions", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<AppointmentResponse>>, t: Throwable) {
                Toast.makeText(this@PatientPrescriptionsActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
