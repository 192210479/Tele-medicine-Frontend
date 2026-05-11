package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.Patient
import com.simats.Tmapp.api.ApiClient
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
            android.util.Log.d("PATIENT_NAV", "Tapped patient: id=${patient.id}, name=${patient.fullName}, lastAppointment=${patient.lastAppointment}")

            val intent = Intent(this@DoctorPatientsActivity, DoctorPatientRecordsActivity::class.java).apply {
                putExtra("patient_id", patient.id ?: -1)
                putExtra("patient_name", patient.fullName ?: "Patient")
                putExtra("last_appointment", patient.lastAppointment ?: "")
            }

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
                        val patients = response.body() ?: emptyList()
                        android.util.Log.d("PATIENT_LIST", "Loaded patients count=${patients.size}, data=$patients")
                        patientAdapter.submitList(patients)
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
