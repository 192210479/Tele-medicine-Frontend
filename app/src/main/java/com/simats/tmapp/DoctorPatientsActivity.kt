package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.simats.tmapp.api.Patient
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorPatientsActivity : AppCompatActivity() {
    private lateinit var rvPatients: RecyclerView
    private lateinit var patientAdapter: PatientAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_patients)

        sessionManager = SessionManager.getInstance(this)
        rvPatients = findViewById(R.id.rvPatients)

        patientAdapter = PatientAdapter { patient ->
            val intent = Intent(this, DoctorPatientRecordsActivity::class.java)
            intent.putExtra("patient_id", patient.id)
            intent.putExtra("patient_name", patient.name)
            intent.putExtra("last_appointment", patient.lastAppointment)
            startActivity(intent)
        }
        rvPatients.adapter = patientAdapter

        findViewById<View>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        loadDoctorPatients()
    }

    private fun loadDoctorPatients() {
        val doctorId = sessionManager.getUserId()
        ApiClient.instance.getDoctorPatients(doctorId)
            .enqueue(object : Callback<List<Patient>> {
                override fun onResponse(call: Call<List<Patient>>, response: Response<List<Patient>>) {
                    if (response.isSuccessful) {
                        patientAdapter.submitList(response.body() ?: emptyList())
                    } else {
                        Toast.makeText(this@DoctorPatientsActivity, "Failed to load patients", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<Patient>>, t: Throwable) {
                    Toast.makeText(this@DoctorPatientsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
