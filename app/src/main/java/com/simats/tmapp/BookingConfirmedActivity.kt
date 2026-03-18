package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.tmapp.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookingConfirmedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_confirmed)

        val tvDoctorName = findViewById<TextView>(R.id.tvDoctorName)
        val tvSpeciality = findViewById<TextView>(R.id.tvSpeciality)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val tvTime = findViewById<TextView>(R.id.tvTime)

        val btnGoToDashboard =
            findViewById<MaterialButton>(R.id.btnGoToDashboard)

        val doctorName = intent.getStringExtra("DOCTOR_NAME")
        val speciality = intent.getStringExtra("SPECIALITY")
        val date = intent.getStringExtra("DATE")
        val time = intent.getStringExtra("TIME")
        val appointmentId = intent.getIntExtra("appointment_id", -1)

        fun updateUI(dName: String?, spec: String?, d: String?, t: String?) {
            tvDoctorName.text = dName ?: "Doctor"
            tvSpeciality.text = spec ?: ""
            tvDate.text = d ?: ""
            tvTime.text = t ?: ""
        }

        updateUI(doctorName, speciality, date, time)

        if (appointmentId != -1) {
            ApiClient.instance.getAppointmentDetails(appointmentId).enqueue(object : Callback<AppointmentResponse> {
                override fun onResponse(call: Call<AppointmentResponse>, response: Response<AppointmentResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val appt = response.body()!!
                        updateUI(appt.doctorName, appt.specialization, appt.date, appt.time)
                    }
                }
                override fun onFailure(call: Call<AppointmentResponse>, t: Throwable) {}
            })
        }

        btnGoToDashboard.setOnClickListener {

            val intent = Intent(
                this,
                PatientDashboardActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
        }
    }
}