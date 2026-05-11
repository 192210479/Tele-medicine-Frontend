package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookingConfirmedActivity : AppCompatActivity() {
    private var doctorId: Int = -1
    private var doctorFee: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_confirmed)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        val tvDoctorName = findViewById<TextView>(R.id.tvDoctorName)
        val tvSpeciality = findViewById<TextView>(R.id.tvSpeciality)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val tvTime = findViewById<TextView>(R.id.tvTime)

        val btnGoToDashboard =
            findViewById<MaterialButton>(R.id.btnGoToDashboard)
        val btnPayNow = findViewById<MaterialButton?>(R.id.btnPayNow)

        val doctorName = intent.getStringExtra("DOCTOR_NAME")
        val specialization = intent.getStringExtra("SPECIALITY")
        val date = intent.getStringExtra("DATE")
        val time = intent.getStringExtra("TIME")
        val appointmentId = intent.getIntExtra("appointment_id", -1)

        doctorId = intent.getIntExtra("doctor_id", -1)
        doctorFee = intent.getDoubleExtra("doctor_fee", 0.0)

        fun convertUTCToLocal(utc: String?): String {
            if (utc.isNullOrEmpty()) return ""
            return try {
                val input = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                input.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val date = input.parse(utc)
                val output = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                output.timeZone = java.util.TimeZone.getDefault()
                output.format(date!!)
            } catch (e: Exception) {
                utc
            }
        }

        fun updateUI(dName: String?, spec: String?, d: String?, t: String?, utcTime: String?, doctorId: Int?, doctorPhoto: String?) {
            tvDoctorName.text = dName ?: "Doctor"
            tvSpeciality.text = spec ?: ""
            tvDate.text = when {
                !d.isNullOrBlank() -> d
                else -> "Date not available"
            }

            tvTime.text = when {
                !utcTime.isNullOrEmpty() -> convertUTCToLocal(utcTime)
                !t.isNullOrBlank() -> t
                else -> "Time not available"
            }

            // Image Loading
            val doctorImage = findViewById<ImageView>(R.id.ivDoctorImage)
            if (doctorImage != null) {
                val baseUrl = ApiClient.BASE_URL.removeSuffix("/")

                val finalDoctorImage = when {
                    !doctorPhoto.isNullOrEmpty() && doctorPhoto.startsWith("http") -> doctorPhoto
                    !doctorPhoto.isNullOrEmpty() && doctorPhoto.startsWith("/") -> "$baseUrl$doctorPhoto"
                    doctorId != null && doctorId != -1 -> "$baseUrl/api/profile/image/$doctorId?role=doctor"
                    else -> null
                }

                AvatarUtils.loadAvatar(
                    imageView = doctorImage,
                    imageUrl = finalDoctorImage,
                    name = dName ?: "Doctor"
                )
            }
        }

        fun updatePaymentUI(paymentStatus: String?) {
            btnPayNow?.let {
                if (paymentStatus.equals("paid", ignoreCase = true)) {
                    it.text = "Payment Done"
                    it.isEnabled = false
                    it.visibility = android.view.View.VISIBLE
                } else {
                    if (doctorFee > 0) {
                        it.text = "Pay ₹$doctorFee"
                        it.visibility = android.view.View.VISIBLE
                        it.isEnabled = true
                    } else {
                        it.visibility = android.view.View.GONE
                    }
                }
            }
        }

        updateUI(doctorName, specialization, date, time, null, doctorId, null)
        updatePaymentUI(null)

        if (appointmentId != -1) {
            ApiClient.instance.getAppointmentDetails(appointmentId).enqueue(object : Callback<AppointmentResponse> {
                override fun onResponse(call: Call<AppointmentResponse>, response: Response<AppointmentResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val appt = response.body()!!

                        updateUI(
                            appt.doctorName ?: doctorName,
                            appt.specialization,
                            appt.date ?: date,
                            appt.localTime ?: time,
                            appt.utcTime,
                            appt.doctorId,
                            appt.doctorImage
                        )

                        doctorId = appt.doctorId ?: doctorId
                        doctorFee = appt.fee ?: doctorFee

                        updatePaymentUI(appt.paymentStatus)
                        if (appt.paymentStatus.equals("paid", ignoreCase = true)) {
                            AppointmentReminderScheduler.scheduleAppointmentReminders(
                                context = this@BookingConfirmedActivity,
                                appointmentId = appointmentId,
                                doctorName = (appt.doctorName ?: doctorName ?: "Doctor").replace("Dr. ", ""),
                                date = appt.date ?: date,
                                time = appt.localTime ?: appt.time ?: time
                            )
                        }
                    }
                }
                override fun onFailure(call: Call<AppointmentResponse>, t: Throwable) {}
            })
        }

        btnPayNow?.setOnClickListener {

            if (appointmentId == -1 || doctorId == -1) {
                android.widget.Toast.makeText(this, "Invalid appointment data", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val paymentIntent = Intent(this, PaymentsActivity::class.java)
            paymentIntent.putExtra("trigger_payment", true)
            paymentIntent.putExtra("doctor_id", doctorId)
            paymentIntent.putExtra("appointment_id", appointmentId)
            paymentIntent.putExtra("amount", doctorFee)
            paymentIntent.putExtra("doctor_name", doctorName ?: "")

            startActivity(paymentIntent)
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
