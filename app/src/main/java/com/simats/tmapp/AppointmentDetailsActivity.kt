package com.simats.tmapp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.simats.tmapp.api.AppointmentResponse
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AppointmentDetailsActivity : AppCompatActivity() {

    private lateinit var tvPatientName: TextView
    private lateinit var tvDoctorName: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvConsultationStatus: TextView


    private var appointmentId: Int = -1
    private var doctorId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment_details)



        tvPatientName = findViewById(R.id.tvPatientName)
        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvDate = findViewById(R.id.tvDate)
        tvTime = findViewById(R.id.tvTime)
        tvStatus = findViewById(R.id.tvStatus)
        tvConsultationStatus = findViewById(R.id.tvConsultationStatus)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        findViewById<View>(R.id.btnShareReports).setOnClickListener {
            if (appointmentId != -1 && doctorId != -1) {
                val intent = android.content.Intent(this, ShareMedicalRecordsActivity::class.java)
                intent.putExtra("appointment_id", appointmentId)
                intent.putExtra("doctor_id", doctorId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Doctor information not available", Toast.LENGTH_SHORT).show()
            }
        }

        appointmentId = intent.getIntExtra("appointment_id", -1)
        if (appointmentId != -1) {
            fetchAppointmentDetails(appointmentId)
        } else {
            Toast.makeText(this, "Invalid Appointment ID", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchAppointmentDetails(appointmentId: Int) {
        ApiClient.instance.getAppointmentDetails(appointmentId).enqueue(object : Callback<AppointmentResponse> {
            override fun onResponse(call: Call<AppointmentResponse>, response: Response<AppointmentResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val appt = response.body()!!
                    doctorId = appt.doctorId ?: -1
                    tvPatientName.text = appt.patientName ?: "N/A"
                    tvDoctorName.text = "Dr. ${appt.doctorName ?: "N/A"}"
                    val combinedUtc = "${appt.date} ${appt.time}"
                    tvDate.text = TimeUtils.convertUtcToLocal(combinedUtc, outputPattern = "dd MMM yyyy")
                    tvTime.text = TimeUtils.convertUtcToLocal(combinedUtc, outputPattern = "hh:mm a")
                    tvStatus.text = appt.status ?: "N/A"
                    tvConsultationStatus.text = appt.consultationStatus ?: "Pending"
                } else {
                    Toast.makeText(this@AppointmentDetailsActivity, "Failed to load details", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AppointmentResponse>, t: Throwable) {
                Toast.makeText(this@AppointmentDetailsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
