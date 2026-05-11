package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.PrescriptionHistoryResponse
import com.simats.Tmapp.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PatientPrescriptionActivity : AppCompatActivity() {
    private lateinit var rvHistory: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var adapter: PrescriptionHistoryAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation_history) // Reusing layout

        sessionManager = SessionManager.getInstance(this)
        
        findViewById<TextView>(R.id.tvTitle).text = "My Prescriptions"
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        rvHistory = findViewById(R.id.rvHistory)
        llEmptyState = findViewById(R.id.llEmptyState)
        rvHistory.layoutManager = LinearLayoutManager(this)

        adapter = PrescriptionHistoryAdapter(emptyList()) { prescription ->
            val intent = Intent(this, PrescriptionActivity::class.java)
            intent.putExtra("appointment_id", prescription.appointmentId)
            startActivity(intent)
        }
        rvHistory.adapter = adapter

        fetchPrescriptionHistory()
    }

    private fun fetchPrescriptionHistory() {
        showEmpty(true)
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()

        ApiClient.instance.getMyAppointments(userId, role, null).enqueue(object : Callback<List<com.simats.Tmapp.api.AppointmentResponse>> {
            override fun onResponse(call: Call<List<com.simats.Tmapp.api.AppointmentResponse>>, response: Response<List<com.simats.Tmapp.api.AppointmentResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    val appointments = response.body()!!
                    if (appointments.isEmpty()) {
                        showEmpty(true)
                        return
                    }

                    // Use Coroutine to fetch all prescriptions in parallel
                    CoroutineScope(Dispatchers.IO).launch {
                        val validPrescriptions = mutableListOf<PrescriptionHistoryResponse>()
                        for (appt in appointments) {
                            try {
                                val prescriptionResponse = ApiClient.instance.getPrescription(appt.id, userId, role).execute()
                                if (prescriptionResponse.isSuccessful) {
                                    val prescription = prescriptionResponse.body()
                                    if (prescription != null && prescription.status == "Ready") {
                                        validPrescriptions.add(
                                            PrescriptionHistoryResponse(
                                                id = 0,
                                                doctorId = appt.doctorId ?: 0,
                                                doctor_name = appt.doctorName ?: "Unknown Doctor",
                                                diagnosis = prescription.diagnosis,
                                                date = appt.date ?: "",
                                                appointmentId = appt.id
                                            )
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        // Update UI on main thread
                        withContext(Dispatchers.Main) {
                            if (validPrescriptions.isEmpty()) {
                                showEmpty(true)
                            } else {
                                showEmpty(false)
                                adapter.updateList(validPrescriptions)
                            }
                        }
                    }
                } else {
                    showEmpty(true)
                }
            }

            override fun onFailure(call: Call<List<com.simats.Tmapp.api.AppointmentResponse>>, t: Throwable) {
                showEmpty(true)
                Toast.makeText(this@PatientPrescriptionActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showEmpty(show: Boolean) {
        rvHistory.visibility = if (show) View.GONE else View.VISIBLE
        llEmptyState.visibility = if (show) View.VISIBLE else View.GONE
    }
}
