package com.simats.Tmapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.AvailabilityResponse
import com.simats.Tmapp.api.GenericResponse
import com.simats.Tmapp.api.PostAvailabilityRequest
import com.simats.Tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class DoctorAvailabilityActivity : AppCompatActivity() {

    private lateinit var btnDatePicker: MaterialButton
    private lateinit var btnStartTimePicker: MaterialButton
    private lateinit var btnSaveSlot: MaterialButton
    private lateinit var rvSlots: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var llEmptyState: LinearLayout
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: AvailabilityAdapter

    private var selectedDate: String = ""
    private var startTime: String = ""
    private var doctorId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_availability)

        sessionManager = SessionManager.getInstance(this)
        doctorId = sessionManager.getUserId()

        initViews()
        setupListeners()
        setupRecyclerView()
        loadSlots()
    }

    private fun initViews() {
        btnDatePicker = findViewById(R.id.btnDatePicker)
        btnStartTimePicker = findViewById(R.id.btnStartTimePicker)
        btnSaveSlot = findViewById(R.id.btnSaveSlot)
        rvSlots = findViewById(R.id.rvSlots)
        progressBar = findViewById(R.id.progressBar)
        llEmptyState = findViewById(R.id.llEmptyState)
    }

    private fun setupListeners() {
        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }

        btnDatePicker.setOnClickListener { showDatePicker() }
        btnStartTimePicker.setOnClickListener { showTimePicker() }
        btnSaveSlot.setOnClickListener { saveSlot() }
    }

    private fun setupRecyclerView() {
        adapter = AvailabilityAdapter(emptyList()) { slot ->
            deleteSlot(slot)
        }
        rvSlots.adapter = adapter
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
            btnDatePicker.text = selectedDate
        }, year, month, day).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, h, m ->
            val time = String.format(Locale.US, "%02d:%02d", h, m)
            startTime = time
            btnStartTimePicker.text = "Time: $time"
        }, hour, minute, true).show()
    }

    private fun loadSlots() {
        progressBar.visibility = View.VISIBLE
        ApiClient.instance.getDoctorAvailability(doctorId).enqueue(object : Callback<List<AvailabilityResponse>> {
            override fun onResponse(call: Call<List<AvailabilityResponse>>, response: Response<List<AvailabilityResponse>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val allSlots = response.body() ?: emptyList()
                    val availableSlots = allSlots.filter { it.is_booked == 0 }
                    adapter.updateList(availableSlots)
                    llEmptyState.visibility = if (availableSlots.isEmpty()) View.VISIBLE else View.GONE
                }
            }

            override fun onFailure(call: Call<List<AvailabilityResponse>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@DoctorAvailabilityActivity, "Error loading slots", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveSlot() {
        if (selectedDate.isEmpty() || startTime.isEmpty()) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
            return
        }

        val request = PostAvailabilityRequest(
            userId = doctorId,
            role = "doctor",
            date = selectedDate,
            timeSlot = startTime,
            timezone = java.util.TimeZone.getDefault().id
        )

        ApiClient.instance.postAvailability(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@DoctorAvailabilityActivity, "Slot Created Successfully", Toast.LENGTH_SHORT).show()
                    loadSlots()
                    resetInputs()
                } else {
                    Toast.makeText(this@DoctorAvailabilityActivity, "Failed to add slots", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@DoctorAvailabilityActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteSlot(slot: AvailabilityResponse) {
        ApiClient.instance.deleteAvailability(slot.id, doctorId, "doctor").enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    loadSlots()
                } else {
                    Toast.makeText(this@DoctorAvailabilityActivity, "Failed to delete slot", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@DoctorAvailabilityActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun resetInputs() {
        selectedDate = ""
        startTime = ""
        btnDatePicker.text = "Select Date"
        btnStartTimePicker.text = "Select Time"
    }
}
