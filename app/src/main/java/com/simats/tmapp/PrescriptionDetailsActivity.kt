package com.simats.tmapp

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.simats.tmapp.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PrescriptionDetailsActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private var appointmentId: Int = -1
    private lateinit var llMedicineList: LinearLayout
    private lateinit var tvDoctorName: TextView
    private lateinit var tvDoctorSpec: TextView
    private lateinit var tvDiagnosis: TextView
    private lateinit var tvAdvice: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prescription_details)

        sessionManager = SessionManager.getInstance(this)
        appointmentId = intent.getIntExtra("appointment_id", -1)

        if (appointmentId == -1) {
            Toast.makeText(this, "Invalid Appointment ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        llMedicineList = findViewById(R.id.llMedicineList)
        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvDoctorSpec = findViewById(R.id.tvDoctorSpecialization)
        tvDiagnosis = findViewById(R.id.tvDiagnosis)
        tvAdvice = findViewById(R.id.tvAdvice)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        fetchAppointmentDetails()
        fetchPrescription()
    }

    private fun fetchAppointmentDetails() {
        ApiClient.instance.getAppointmentDetails(appointmentId).enqueue(object : Callback<AppointmentResponse> {
            override fun onResponse(call: Call<AppointmentResponse>, response: Response<AppointmentResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val appt = response.body()!!
                    val docName = appt.doctorName
                    tvDoctorName.text = if (docName?.lowercase() == "null" || docName.isNullOrEmpty()) "Doctor" else "Dr. $docName"
                    tvDoctorSpec.text = appt.specialization ?: "Specialization"
                }
            }
            override fun onFailure(call: Call<AppointmentResponse>, t: Throwable) {}
        })
    }

    private fun fetchPrescription() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()

        ApiClient.instance.getPrescription(appointmentId, userId, role).enqueue(object : Callback<PrescriptionResponse> {
            override fun onResponse(call: Call<PrescriptionResponse>, response: Response<PrescriptionResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val pres = response.body()!!
                    tvDiagnosis.text = pres.diagnosis
                    tvAdvice.text = if (pres.advice.isNullOrEmpty()) "No advice provided" else pres.advice
                    
                    llMedicineList.removeAllViews()
                    pres.medicines?.forEach { medicine ->
                        addMedicineToUI(medicine)
                    }
                } else {
                    Toast.makeText(this@PrescriptionDetailsActivity, "Prescription not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PrescriptionResponse>, t: Throwable) {
                Toast.makeText(this@PrescriptionDetailsActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addMedicineToUI(medicine: MedicineResponse) {
        val view = layoutInflater.inflate(R.layout.item_medicine_card, llMedicineList, false)
        view.findViewById<TextView>(R.id.tvMedicineName).text = medicine.name
        view.findViewById<TextView>(R.id.tvDosage).text = medicine.dosage
        view.findViewById<TextView>(R.id.tvFrequency).text = medicine.frequency
        view.findViewById<TextView>(R.id.tvDuration).text = medicine.duration
        view.findViewById<TextView>(R.id.tvInstructions).text = medicine.instructions
        view.findViewById<ImageView>(R.id.ivDelete).visibility = View.GONE // View-only
        
        llMedicineList.addView(view)
    }
}
