package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SelectTimeActivity : AppCompatActivity() {
    private var doctorFee: Double = 0.0
    private var doctorNameStr: String = ""

    private var doctorId: Int = -1
    private var selectedAvailabilityId: Int = -1

    private var selectedDate: String = ""
    private var selectedTime: String = ""

    private var currentSymptoms: String? = null
    private var currentPriority: String? = null

    private var currentCalendar: java.util.Calendar = java.util.Calendar.getInstance()
    private var selectedCalendar: java.util.Calendar = java.util.Calendar.getInstance()
    private var allSlots: List<AvailabilityResponse> = emptyList()

    private lateinit var llCalendarBody: android.widget.LinearLayout
    private lateinit var tvCalendarMonth: TextView
    private lateinit var ivPrevMonth: ImageView
    private lateinit var ivNextMonth: ImageView

    private lateinit var sessionManager: SessionManager
    private lateinit var glSlots: GridLayout
    private lateinit var btnConfirmBooking: MaterialButton

    private lateinit var tvDoctorName: TextView
    private lateinit var tvSpeciality: TextView
    private lateinit var tvHospital: TextView
    private lateinit var ivDoctorImage: ImageView
    private lateinit var tvSlotRating: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_time)

        sessionManager = SessionManager.getInstance(this)

        doctorId = intent.getIntExtra("doctor_id", -1)
        currentSymptoms = intent.getStringExtra("symptoms")
        currentPriority = intent.getStringExtra("priority")

        doctorFee = intent.getDoubleExtra("doctor_fee", 0.0)
        doctorNameStr = intent.getStringExtra("doctor_name") ?: ""

        val ivBack = findViewById<ImageView>(R.id.ivBack)

        glSlots = findViewById(R.id.glSlots)
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking)
        btnConfirmBooking.text = "Continue to Payment"

        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvSpeciality = findViewById(R.id.tvSpeciality)
        tvHospital = findViewById(R.id.tvHospital)
        ivDoctorImage = findViewById(R.id.ivDoctorImage)
        tvSlotRating = findViewById(R.id.tvSlotRating)

        if (doctorId != -1) {
            fetchDoctorDetails()
        }

        llCalendarBody = findViewById(R.id.llCalendarBody)
        tvCalendarMonth = findViewById(R.id.tvMonth)

        ivNextMonth = findViewById(R.id.ivNextMonth)
        ivPrevMonth = findViewById(R.id.ivPrevMonth)


        ivNextMonth.setOnClickListener {
            currentCalendar.add(java.util.Calendar.MONTH, 1)
            populateCalendar()
        }
        ivPrevMonth.setOnClickListener {
            val now = java.util.Calendar.getInstance()
            if (currentCalendar.get(java.util.Calendar.YEAR) > now.get(java.util.Calendar.YEAR) ||
                currentCalendar.get(java.util.Calendar.MONTH) > now.get(java.util.Calendar.MONTH)) {
                currentCalendar.add(java.util.Calendar.MONTH, -1)
                populateCalendar()
            }
        }

        ivBack.setOnClickListener { finish() }

        btnConfirmBooking.isEnabled = false
        btnConfirmBooking.alpha = 0.5f

        btnConfirmBooking.setOnClickListener {
            bookAppointment()
        }

        fetchAvailability()
    }

    private fun initCalendar() {
        populateCalendar()
        // Today is selected by default
        updateSlotsForSelectedDate()
    }

    private fun populateCalendar() {
        val dateFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
        tvCalendarMonth.text = dateFormat.format(currentCalendar.time)

        llCalendarBody.removeAllViews()

        val tempCal = currentCalendar.clone() as java.util.Calendar
        tempCal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = tempCal.get(java.util.Calendar.DAY_OF_WEEK) - 1 // 0-indexed Su-Sa
        val daysInMonth = tempCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        var currentDay = 1
        for (i in 0 until 6) { // Max 6 rows
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                weightSum = 7f
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
            }

            for (j in 0 until 7) {
                val dayFrame = android.widget.FrameLayout(this).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                if ((i == 0 && j < firstDayOfWeek) || currentDay > daysInMonth) {
                    // Empty cell
                } else {
                    val dayTv = TextView(this).apply {
                        text = currentDay.toString()
                        gravity = android.view.Gravity.CENTER
                        textSize = 14f
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            (32 * resources.displayMetrics.density).toInt(),
                            (32 * resources.displayMetrics.density).toInt(),
                            android.view.Gravity.CENTER
                        )
                        
                        val dayDate = currentDay
                        val dayMonth = tempCal.get(java.util.Calendar.MONTH)
                        val dayYear = tempCal.get(java.util.Calendar.YEAR)

                        val isPast = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.let { todayCal ->
                            val checkCal = java.util.Calendar.getInstance().apply {
                                set(java.util.Calendar.YEAR, dayYear)
                                set(java.util.Calendar.MONTH, dayMonth)
                                set(java.util.Calendar.DAY_OF_MONTH, dayDate)
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }
                            checkCal.before(todayCal)
                        }

                        val isSelected = selectedCalendar.get(java.util.Calendar.DAY_OF_MONTH) == dayDate &&
                                         selectedCalendar.get(java.util.Calendar.MONTH) == dayMonth &&
                                         selectedCalendar.get(java.util.Calendar.YEAR) == dayYear

                        if (isPast) {
                            setTextColor(android.graphics.Color.LTGRAY)
                            isEnabled = false
                        } else {
                            setTextColor(android.graphics.Color.BLACK)
                            if (isSelected) {
                                setBackgroundResource(R.drawable.bg_circle_soft_blue_small)
                                setTextColor(android.graphics.Color.WHITE)
                                backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2F6FED"))
                            }

                            setOnClickListener {
                                selectedCalendar.set(java.util.Calendar.YEAR, dayYear)
                                selectedCalendar.set(java.util.Calendar.MONTH, dayMonth)
                                selectedCalendar.set(java.util.Calendar.DAY_OF_MONTH, dayDate)
                                populateCalendar()
                                updateSlotsForSelectedDate()
                            }
                        }
                    }
                    dayFrame.addView(dayTv)
                    currentDay++
                }
                row.addView(dayFrame)
            }
            llCalendarBody.addView(row)
            if (currentDay > daysInMonth) break
        }
    }

    private fun updateSlotsForSelectedDate() {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val dateStr = sdf.format(selectedCalendar.time)
        val filteredSlots = allSlots.filter { 
            val localizedDate = if (it.utcTime != null) TimeUtils.convertIsoUtcToLocalDate(it.utcTime) else it.date
            localizedDate == dateStr && it.status?.equals("Available", true) == true
        }
        displaySlots(filteredSlots)
        
        selectedDate = dateStr
        btnConfirmBooking.isEnabled = false
        btnConfirmBooking.alpha = 0.4f
        btnConfirmBooking.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#B0BEC5"))
    }

    private fun fetchDoctorDetails() {
        ApiClient.instance.getDoctorDetails(doctorId).enqueue(object : Callback<DoctorResponse> {
            override fun onResponse(call: Call<DoctorResponse>, response: Response<DoctorResponse>) {
                if (response.isSuccessful) {
                    val doctor = response.body()
                    doctor?.let {
                        tvDoctorName.text = it.name
                        tvSpeciality.text = it.specialization
                        tvHospital.text = "Simats Medical Center"
                        val slotEffectiveRating = if ((it.reviewsCount ?: 0) > 0) it.rating ?: 0.0f else 0.0f
                        tvSlotRating.text = "${"%.1f".format(slotEffectiveRating)}"

                        if ((it.fee ?: 0.0) > 0) doctorFee = it.fee ?: doctorFee
                        if (!it.name.isNullOrEmpty()) doctorNameStr = it.name!!

                        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
                        val finalDoctorImage = when {
                            !it.profileImage.isNullOrEmpty() && it.profileImage!!.startsWith("http") -> it.profileImage
                            !it.profileImage.isNullOrEmpty() && it.profileImage!!.startsWith("/") -> "$baseUrl${it.profileImage}"
                            doctorId != -1 -> "$baseUrl/api/profile/image/$doctorId?role=doctor"
                            else -> null
                        }

                        AvatarUtils.loadAvatar(
                            imageView = ivDoctorImage,
                            imageUrl = finalDoctorImage,
                            name = it.name ?: "Doctor"
                        )
                    }
                }
            }
            override fun onFailure(call: Call<DoctorResponse>, t: Throwable) {}
        })
    }

    private fun fetchAvailability() {

        if (doctorId == -1) {
            Toast.makeText(this, "Invalid doctor", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.instance.getDoctorAvailability(doctorId)
            .enqueue(object : Callback<List<AvailabilityResponse>> {

                override fun onResponse(
                    call: Call<List<AvailabilityResponse>>,
                    response: Response<List<AvailabilityResponse>>
                ) {

                    if (response.isSuccessful) {
                        val today = java.time.LocalDate.now()
                        allSlots = (response.body() ?: emptyList()).filter { slot ->
                            val localizedDateStr = if (slot.utcTime != null) {
                                TimeUtils.convertIsoUtcToLocalDate(slot.utcTime)
                            } else {
                                slot.date
                            }
                            val slotDate = try { java.time.LocalDate.parse(localizedDateStr) } catch (e: Exception) { null }
                            slotDate != null && (slotDate.isEqual(today) || slotDate.isAfter(today))
                        }
                        initCalendar()
                    } else {

                        Toast.makeText(
                            this@SelectTimeActivity,
                            "Failed to load slots",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<List<AvailabilityResponse>>,
                    t: Throwable
                ) {

                    Toast.makeText(
                        this@SelectTimeActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun displaySlots(slots: List<AvailabilityResponse>) {

        glSlots.removeAllViews()

        if (slots.isEmpty()) {

            val emptyText = TextView(this)
            emptyText.text = "No available slots"
            glSlots.addView(emptyText)

            return
        }

        for (slot in slots) {

            val slotView =
                layoutInflater.inflate(R.layout.item_slot, glSlots, false) as TextView

            val displayTime = if (slot.utcTime != null) {
                TimeUtils.convertIsoUtcToLocal(slot.utcTime)
            } else {
                slot.time
            }

            slotView.text = displayTime
            slotView.isSelected = (slot.id == selectedAvailabilityId)

            slotView.setOnClickListener {

                for (i in 0 until glSlots.childCount) {
                    val child = glSlots.getChildAt(i)
                    child.isSelected = false
                }

                slotView.isSelected = true
                selectedAvailabilityId = slot.id
                selectedDate = slot.date ?: ""
                selectedTime = slot.time ?: ""

                Toast.makeText(this, "Slot selected", Toast.LENGTH_SHORT).show()

                btnConfirmBooking.isEnabled = true
                btnConfirmBooking.alpha = 1f
                btnConfirmBooking.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E7DFF"))
            }

            glSlots.addView(slotView)
        }
    }

    private fun bookAppointment() {
        val userId = sessionManager.getUserId()

        if (userId == -1) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedAvailabilityId == -1 || selectedDate.isBlank() || selectedTime.isBlank()) {
            Toast.makeText(this, "Please select a valid slot", Toast.LENGTH_SHORT).show()
            return
        }

        btnConfirmBooking.isEnabled = false
        btnConfirmBooking.alpha = 0.5f

        val request = BookAppointmentRequest(
            userId = userId,
            role = "patient",
            availabilityId = selectedAvailabilityId,
            symptoms = currentSymptoms,
            priority = currentPriority ?: "low"
        )

        ApiClient.instance.bookAppointment(request).enqueue(object : Callback<BookingResponse> {
            override fun onResponse(call: Call<BookingResponse>, response: Response<BookingResponse>) {
                btnConfirmBooking.isEnabled = true
                btnConfirmBooking.alpha = 1f

                val body = response.body()

                if (response.isSuccessful && body != null && body.appointmentId != null) {
                    val appointmentId = body.appointmentId
                    val doctorIdResp = body.doctorId ?: doctorId
                    val doctorNameResp = body.doctorName ?: doctorNameStr
                    val paymentRequired = body.paymentRequired ?: true

                    if (paymentRequired) {
                        Toast.makeText(
                            this@SelectTimeActivity,
                            "Slot reserved temporarily. Complete payment to confirm.",
                            Toast.LENGTH_LONG
                        ).show()

                        val intent = Intent(this@SelectTimeActivity, PaymentsActivity::class.java)
                        intent.putExtra("trigger_payment", true)
                        intent.putExtra("doctor_id", doctorIdResp)
                        intent.putExtra("appointment_id", appointmentId!!)
                        intent.putExtra("amount", doctorFee)
                        intent.putExtra("doctor_name", doctorNameResp)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@SelectTimeActivity,
                            body.message ?: "Appointment booked successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(this@SelectTimeActivity, BookingConfirmedActivity::class.java)
                        intent.putExtra("appointment_id", appointmentId!!)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Toast.makeText(
                        this@SelectTimeActivity,
                        body?.error ?: body?.message ?: "Booking failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<BookingResponse>, t: Throwable) {
                btnConfirmBooking.isEnabled = true
                btnConfirmBooking.alpha = 1f
                Toast.makeText(this@SelectTimeActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        setupSocketListeners()
    }

    private fun setupSocketListeners() {
        val socket = SocketService.socket ?: return
        socket.on("new_appointment") {
            runOnUiThread { fetchAvailability() }
        }
        socket.on("appointment_updated") {
            runOnUiThread { fetchAvailability() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val socket = SocketService.socket ?: return
        socket.off("new_appointment")
        socket.off("appointment_updated")
    }
}
