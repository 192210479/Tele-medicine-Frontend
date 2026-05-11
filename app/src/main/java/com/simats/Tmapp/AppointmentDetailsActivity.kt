package com.simats.Tmapp

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.simats.Tmapp.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

class AppointmentDetailsActivity : AppCompatActivity(), PaymentResultWithDataListener {

    private lateinit var tvPatientName: TextView
    private lateinit var tvDoctorName: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvConsultationStatus: TextView
    private lateinit var btnPayNow: View
    private lateinit var btnCancelAppointment: View
    private lateinit var cvCancellationDetails: View
    private lateinit var tvCancelledBy: TextView
    private lateinit var tvCancellationReason: TextView
    private lateinit var btnShareReports: View
    private lateinit var sessionManager: SessionManager

    private var appointmentId: Int = -1
    private var doctorId: Int = -1
    private var consultationFee: Double = 0.0

    // Store pending payment state for verification
    private var reminderScheduled = false
    private var pendingOrderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment_details)

        Checkout.preload(applicationContext)
        sessionManager = SessionManager.getInstance(this)

        tvPatientName = findViewById(R.id.tvPatientName)
        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvDate = findViewById(R.id.tvDate)
        tvTime = findViewById(R.id.tvTime)
        tvStatus = findViewById(R.id.tvStatus)
        tvConsultationStatus = findViewById(R.id.tvConsultationStatus)
        btnPayNow = findViewById(R.id.btnPayNow)
        btnCancelAppointment = findViewById(R.id.btnCancelAppointment)
        cvCancellationDetails = findViewById(R.id.cvCancellationDetails)
        tvCancelledBy = findViewById(R.id.tvCancelledBy)
        tvCancellationReason = findViewById(R.id.tvCancellationReason)
        btnShareReports = findViewById(R.id.btnShareReports)

        if (sessionManager.getUserRole()?.lowercase()?.contains("doctor") == true) {
            btnShareReports.visibility = View.GONE
        }

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnShareReports.setOnClickListener {
            if (appointmentId != -1 && doctorId != -1) {
                val intent = android.content.Intent(this, ShareMedicalRecordsActivity::class.java)
                intent.putExtra("appointment_id", appointmentId)
                intent.putExtra("doctor_id", doctorId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Doctor information not available", Toast.LENGTH_SHORT).show()
            }
        }

        btnPayNow.setOnClickListener {
            startPaymentFlow()
        }

        btnCancelAppointment.setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel this appointment?")
                .setPositiveButton("Yes") { _, _ ->
                    performCancelAppointment()
                }
                .setNegativeButton("No", null)
                .show()
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
                    android.util.Log.d("APPT_DEBUG", "FULL RESPONSE = $appt")
                    android.util.Log.d("APPT_DEBUG", "patientName=${appt.patientName}")
                    android.util.Log.d("APPT_DEBUG", "doctorName=${appt.doctorName}")
                    android.util.Log.d("APPT_DEBUG", "date=${appt.date}")
                    android.util.Log.d("APPT_DEBUG", "time=${appt.time}")
                    android.util.Log.d("APPT_DEBUG", "localTime=${appt.localTime}")
                    android.util.Log.d("APPT_DEBUG", "utcTime=${appt.utcTime}")
                    android.util.Log.d("APPT_DEBUG", "status=${appt.status}")
                    android.util.Log.d("APPT_DEBUG", "consultationStatus=${appt.consultationStatus}")
                    doctorId = appt.doctorId ?: -1
                    consultationFee = appt.consultationFee ?: 0.0

                    // --- Resolve best available values safely ---
                    val resolvedPatientName = appt.patientName?.takeIf { it.isNotBlank() }
                    val resolvedDoctorName = appt.doctorName?.takeIf { it.isNotBlank() }
                    val resolvedDate = appt.date?.takeIf { it.isNotBlank() }
                    val resolvedLocalTime = appt.localTime?.takeIf { it.isNotBlank() }
                    val resolvedTime = appt.time?.takeIf { it.isNotBlank() }

                    // --- Bind names ---
                    tvPatientName.text = resolvedPatientName ?: "--"

                    tvDoctorName.text = when {
                        !resolvedDoctorName.isNullOrBlank() -> "Dr. $resolvedDoctorName"
                        else -> "--"
                    }

                    // --- Bind date/time with priority: utcTime > localTime > time ---
                    if (!appt.utcTime.isNullOrEmpty()) {
                        tvDate.text = TimeUtils.convertIsoUtcToLocal(
                            appt.utcTime,
                            outputPattern = "dd MMM yyyy"
                        )
                        tvTime.text = TimeUtils.convertIsoUtcToLocal(
                            appt.utcTime,
                            outputPattern = "hh:mm a"
                        )
                    } else if (!resolvedLocalTime.isNullOrEmpty()) {
                        tvDate.text = resolvedDate ?: "--"
                        tvTime.text = formatTime(resolvedLocalTime)
                    } else if (!resolvedTime.isNullOrEmpty()) {
                        tvDate.text = resolvedDate ?: "--"
                        tvTime.text = formatTime(resolvedTime)
                    } else {
                        tvDate.text = resolvedDate ?: "--"
                        tvTime.text = "--"
                    }

                    val apptStatus = appt.status?.takeIf { it.isNotBlank() } ?: "Scheduled"
                    val consStatus = appt.consultationStatus?.takeIf { it.isNotBlank() } ?: "Pending"
                    val payStatus = when {
                        !appt.paymentStatus.isNullOrBlank() -> appt.paymentStatus
                        !appt.paymentInfo?.status.isNullOrBlank() -> appt.paymentInfo?.status
                        else -> "unpaid"
                    }

                    tvStatus.text = apptStatus
                    // ===== CANCEL BUTTON VISIBILITY =====
                    val normalizedStatus = (appt.status ?: "").trim().lowercase()
                    val consultationStatus = (appt.consultationStatus ?: "").trim().lowercase()

                    val canCancel =
                        normalizedStatus != "cancelled" &&
                        normalizedStatus != "completed" &&
                        normalizedStatus != "missed" &&
                        consultationStatus != "completed"

                    btnCancelAppointment.visibility = if (canCancel) View.VISIBLE else View.GONE

                    // --- Cancellation Details Logic ---
                    if (normalizedStatus == "cancelled") {
                        cvCancellationDetails.visibility = View.VISIBLE
                        val canceller = appt.cancelledBy?.trim()?.lowercase() ?: "unknown"
                        val reason = appt.cancellationReason ?: "No reason provided"
                        
                        val cancellationText = when (canceller) {
                            "patient" -> "Cancelled by: Patient"
                            "doctor" -> "Cancelled by: Doctor (You)"
                            "admin" -> "Cancelled by: Administrator"
                            else -> "Cancelled by: Unknown"
                        }
                        tvCancelledBy.text = cancellationText
                        tvCancellationReason.text = "Reason: $reason"
                    } else {
                        cvCancellationDetails.visibility = View.GONE
                    }

                    // STRICT PAYMENT BUTTON LOGIC (aligned with backend)
                    val bookingStatus = appt.bookingStatus?.takeIf { it.isNotBlank() } ?: ""
                    val normalizedPayStatus = payStatus?.trim()?.lowercase() ?: ""
                    val normalizedApptStatus = apptStatus.trim().lowercase()
                    val normalizedBookingStatus = bookingStatus.trim().lowercase()

                    val isCancelled = normalizedApptStatus == "cancelled"
                    val isCompleted = normalizedApptStatus == "completed"
                    val isMissed = normalizedApptStatus == "missed"

                    val isPaid = normalizedPayStatus == "paid" || normalizedPayStatus == "success"
                    val isPendingOrRecoverablePayment =
                        normalizedPayStatus == "unpaid" ||
                        normalizedPayStatus == "pending" ||
                        normalizedPayStatus == "failed"

                    val isRecoverableBooking =
                        normalizedBookingStatus.isBlank() ||
                        normalizedBookingStatus == "pending" ||
                        normalizedBookingStatus == "payment_pending" ||
                        normalizedBookingStatus == "payment pending"

                    val needsPayment =
                        !isCancelled &&
                        !isCompleted &&
                        !isMissed &&
                        !isPaid &&
                        isPendingOrRecoverablePayment &&
                        isRecoverableBooking

                    btnPayNow.visibility = if (needsPayment) View.VISIBLE else View.GONE
                    if (
                        !reminderScheduled &&
                        (payStatus.equals("paid", ignoreCase = true) ||
                        payStatus.equals("success", ignoreCase = true))
                    ) {
                        reminderScheduled = true

                        AppointmentReminderScheduler.scheduleAppointmentReminders(
                            context = this@AppointmentDetailsActivity,
                            appointmentId = appointmentId,
                            doctorName = resolvedDoctorName ?: "Doctor",
                            date = resolvedDate,
                            time = resolvedLocalTime ?: resolvedTime ?: appt.time
                        )
                    }

                    val consultationLabel = findViewById<TextView>(R.id.tvConsultationStatusLabel)

                    if (consStatus.equals("Pending", ignoreCase = true) ||
                        consStatus.equals("Scheduled", ignoreCase = true) ||
                        consStatus.equals(apptStatus, ignoreCase = true)
                    ) {
                        tvConsultationStatus.visibility = View.GONE
                        consultationLabel?.visibility = View.GONE
                    } else {
                        tvConsultationStatus.visibility = View.VISIBLE
                        consultationLabel?.visibility = View.VISIBLE
                        tvConsultationStatus.text = consStatus
                    }
                } else {
                    Toast.makeText(this@AppointmentDetailsActivity, "Failed to load details", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AppointmentResponse>, t: Throwable) {
                Toast.makeText(this@AppointmentDetailsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startPaymentFlow() {
        if (appointmentId == -1 || doctorId == -1 || consultationFee <= 0.0) {
            Toast.makeText(this, "Payment details unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = android.content.Intent(this, PaymentsActivity::class.java)
        intent.putExtra("trigger_payment", true)
        intent.putExtra("doctor_id", doctorId)
        intent.putExtra("appointment_id", appointmentId)
        intent.putExtra("amount", consultationFee)
        intent.putExtra("doctor_name", tvDoctorName.text.toString().replace("Dr. ", ""))
        startActivity(intent)
    }

    private fun launchRazorpay(orderId: String, razorpayKey: String, amountPaise: Int) {
        val checkout = Checkout()
        checkout.setKeyID(razorpayKey)

        try {
            val options = JSONObject()
            options.put("name", "Telemedicine App")
            options.put("description", "Consultation Fee")
            options.put("currency", "INR")
            options.put("amount", amountPaise)
            options.put("order_id", orderId)

            val prefill = JSONObject()
            prefill.put("email", sessionManager.getUserEmail() ?: "")
            prefill.put("contact", sessionManager.getUserPhone() ?: "")
            options.put("prefill", prefill)

            val retryObj = JSONObject()
            retryObj.put("enabled", true)
            retryObj.put("max_count", 4)
            options.put("retry", retryObj)

            val theme = JSONObject()
            theme.put("color", "#2563EB")
            options.put("theme", theme)

            // IMPORTANT:
            // Do NOT restrict methods like UPI-only / card-only.
            // Let Razorpay show all enabled payment methods automatically.

            checkout.open(this, options)

        } catch (e: Exception) {
            Toast.makeText(this, "Error launching payment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val orderId = paymentData?.orderId ?: pendingOrderId
        val signature = paymentData?.signature
        val userId = sessionManager.getUserId()

        if (orderId.isNullOrBlank() || razorpayPaymentId.isNullOrBlank() || signature.isNullOrBlank()) {
            Toast.makeText(this, "Payment verification failed: incomplete data", Toast.LENGTH_SHORT).show()
            return
        }

        val request = VerifyPaymentRequest(
            patientId = userId,
            doctorId = doctorId,
            appointmentId = appointmentId,
            orderId = orderId,
            razorpayPaymentId = razorpayPaymentId,
            razorpaySignature = signature,
            amount = consultationFee.takeIf { it > 0 } ?: 500.0,
            timezone = java.util.TimeZone.getDefault().id
        )

        ApiClient.instance.verifyPaymentFull(request)
            .enqueue(object : Callback<VerifyPaymentResponse> {
                override fun onResponse(
                    call: Call<VerifyPaymentResponse>,
                    response: Response<VerifyPaymentResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.success == true) {
                        Toast.makeText(
                            this@AppointmentDetailsActivity,
                            "✅ Payment Successful! Invoice: ${body.invoiceNumber ?: ""}",
                            Toast.LENGTH_LONG
                        ).show()

                        pendingOrderId = null
                        fetchAppointmentDetails(appointmentId)
                    } else {
                        val errMsg = body?.error ?: body?.message ?: "Verification failed"
                        Toast.makeText(this@AppointmentDetailsActivity, errMsg, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<VerifyPaymentResponse>, t: Throwable) {
                    Toast.makeText(
                        this@AppointmentDetailsActivity,
                        "Verification error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        val msg = when (code) {
            0 -> "Network error"
            1 -> "Payment cancelled"
            2 -> "Invalid options"
            else -> "Payment failed (code $code)"
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun formatTime(timeStr: String?): String {
        if (timeStr.isNullOrEmpty()) return "--:--"
        return try {
            val parser = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val parsed = parser.parse(timeStr)
            if (parsed != null) formatter.format(parsed) else {
                val parser2 = SimpleDateFormat("HH:mm", Locale.getDefault())
                val parsed2 = parser2.parse(timeStr)
                if (parsed2 != null) formatter.format(parsed2) else timeStr
            }
        } catch (e: Exception) {
            timeStr ?: "--:--"
        }
    }
    private fun performCancelAppointment() {

        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole()

        if (userId == null || role == null) {
            Toast.makeText(this, "User session error", Toast.LENGTH_SHORT).show()
            return
        }

        val body = mapOf(
            "user_id" to userId,
            "role" to role
        )

        ApiClient.instance.cancelAppointment(appointmentId, body)
            .enqueue(object : Callback<GenericResponse> {

                override fun onResponse(
                    call: Call<GenericResponse>,
                    response: Response<GenericResponse>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@AppointmentDetailsActivity,
                            "Appointment cancelled successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Refresh screen
                        fetchAppointmentDetails(appointmentId)

                    } else {
                        Toast.makeText(
                            this@AppointmentDetailsActivity,
                            "Failed to cancel appointment",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(
                        this@AppointmentDetailsActivity,
                        "Network error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
