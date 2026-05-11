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
import com.simats.Tmapp.api.GenericResponse
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import android.util.Log
import com.simats.Tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class CreateSlotActivity : AppCompatActivity() {

    private lateinit var btnDatePicker: MaterialButton
    private lateinit var btnStartTimePicker: MaterialButton
    private lateinit var btnSaveSlot: MaterialButton
    
    private lateinit var btnBulkDatePicker: MaterialButton
    private lateinit var btnBulkStartTime: MaterialButton
    private lateinit var btnBulkEndTime: MaterialButton
    private lateinit var btnBulkGenerate: MaterialButton
    private lateinit var etDuration: com.google.android.material.textfield.TextInputEditText

    private lateinit var rvSlots: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var llEmptyState: LinearLayout
    private lateinit var sessionManager: SessionManager
    private lateinit var slotAdapter: DoctorSlotAdapter

    private var selectedDate: String = ""
    private var startTime: String = ""
    
    private var bulkDate: String = ""
    private var bulkStart: String = ""
    private var bulkEnd: String = ""
    
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
        
        btnBulkDatePicker = findViewById(R.id.btnBulkDatePicker)
        btnBulkStartTime = findViewById(R.id.btnBulkStartTime)
        btnBulkEndTime = findViewById(R.id.btnBulkEndTime)
        btnBulkGenerate = findViewById(R.id.btnBulkGenerate)
        etDuration = findViewById(R.id.etDuration)

        rvSlots = findViewById(R.id.rvSlots)
        progressBar = findViewById(R.id.progressBar)
        llEmptyState = findViewById(R.id.llEmptyState)
    }

    private fun setupListeners() {
        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }

        btnDatePicker.setOnClickListener { showDatePicker { date -> 
            selectedDate = date
            btnDatePicker.text = date
        }}
        btnStartTimePicker.setOnClickListener { showTimePicker { time -> 
            startTime = time
            btnStartTimePicker.text = time
        }}
        btnSaveSlot.setOnClickListener { saveSlot() }

        btnBulkDatePicker.setOnClickListener { showDatePicker { date -> 
            bulkDate = date
            btnBulkDatePicker.text = date
        }}
        btnBulkStartTime.setOnClickListener { showTimePicker { time -> 
            bulkStart = time
            btnBulkStartTime.text = "Start: $time"
        }}
        btnBulkEndTime.setOnClickListener { showTimePicker { time -> 
            bulkEnd = time
            btnBulkEndTime.text = "End: $time"
        }}
        btnBulkGenerate.setOnClickListener { generateBlockSlots() }
    }

    private fun setupRecyclerView() {
        slotAdapter = DoctorSlotAdapter(
            onDeleteSlot = { slot -> showSlotActionConfirmation(slot, "delete") },
            onCancelSlot = { slot -> showSlotActionConfirmation(slot, "cancel") }
        )
        rvSlots.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvSlots.adapter = slotAdapter
    }

    private fun showDatePicker(onSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            val date = String.format("%04d-%02d-%02d", y, m + 1, d)
            onSelected(date)
        }, year, month, day).show()
    }

    private fun showTimePicker(onSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, h, m ->
            val time = String.format("%02d:%02d", h, m)
            onSelected(time)
        }, hour, minute, true).show()
    }

    private fun loadSlots() {
        progressBar.visibility = View.VISIBLE
        ApiClient.instance.getDoctorAvailability(doctorId).enqueue(object : Callback<List<com.simats.Tmapp.api.AvailabilityResponse>> {
            override fun onResponse(call: Call<List<com.simats.Tmapp.api.AvailabilityResponse>>, response: Response<List<com.simats.Tmapp.api.AvailabilityResponse>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val slots = response.body() ?: emptyList()
                    val today = java.time.LocalDate.now()
                    val filteredSlots = slots.filter { slot ->
                        val slotDate = try { java.time.LocalDate.parse(slot.date) } catch (e: Exception) { null }
                        slotDate != null && (slotDate.isEqual(today) || slotDate.isAfter(today))
                    }
                    slotAdapter.updateList(filteredSlots)
                    llEmptyState.visibility = if (filteredSlots.isEmpty()) View.VISIBLE else View.GONE
                }
            }

            override fun onFailure(call: Call<List<com.simats.Tmapp.api.AvailabilityResponse>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@CreateSlotActivity, "Error loading slots", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveSlot() {
        if (selectedDate.isEmpty() || startTime.isEmpty()) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
            return
        }

        val request = com.simats.Tmapp.api.PostAvailabilityRequest(
            userId = doctorId,
            role = "doctor",
            date = selectedDate,
            timeSlot = startTime,
            timezone = java.util.TimeZone.getDefault().id
        )

        ApiClient.instance.postAvailability(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@CreateSlotActivity, "Slot Created Successfully", Toast.LENGTH_SHORT).show()
                    loadSlots()
                    resetInputs()
                } else {
                    Toast.makeText(this@CreateSlotActivity, "Failed to add slot", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@CreateSlotActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun generateBlockSlots() {
        val duration = etDuration.text.toString().toIntOrNull() ?: 30
        if (bulkDate.isEmpty() || bulkStart.isEmpty() || bulkEnd.isEmpty()) {
            Toast.makeText(this, "Please fill all bulk generation fields", Toast.LENGTH_SHORT).show()
            return
        }

        val request = com.simats.Tmapp.api.PostBulkAvailabilityRequest(
            userId = doctorId,
            role = "doctor",
            date = bulkDate,
            startTime = bulkStart,
            endTime = bulkEnd,
            slotDuration = duration,
            timezone = java.util.TimeZone.getDefault().id
        )

        progressBar.visibility = View.VISIBLE
        ApiClient.instance.postBulkAvailability(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@CreateSlotActivity, "Block Generated Successfully", Toast.LENGTH_SHORT).show()
                    loadSlots()
                    resetBulkInputs()
                } else {
                    Toast.makeText(this@CreateSlotActivity, "Failed to generate block", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@CreateSlotActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteSlot(slot: com.simats.Tmapp.api.AvailabilityResponse) {
        ApiClient.instance.deleteAvailability(slot.id, doctorId, "doctor").enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    loadSlots()
                } else {
                    Toast.makeText(this@CreateSlotActivity, "Failed to delete slot", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@CreateSlotActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showSlotActionConfirmation(slot: com.simats.Tmapp.api.AvailabilityResponse, action: String) {
        val title = "Confirm Action"
        val message = "Are you sure you want to $action this slot?"
        
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->
                performSlotAction(slot, action)
            }
            .show()
    }

    private fun performSlotAction(slot: com.simats.Tmapp.api.AvailabilityResponse, action: String) {
        val isBooked = (slot.is_booked == 1 || slot.status?.equals("Booked", true) == true)
        
        deleteSlot(slot)
        
        // Success Popup/Toast
        val successMsg = if (action == "delete") "Slot deleted successfully." else "Slot cancelled successfully."
        Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show()

        // Socket Emission if it was booked
        if (isBooked) {
            try {
                val socket = SocketService.socket
                val data = JSONObject()
                data.put("appointment_id", slot.appointment_id ?: -1)
                data.put("doctor_id", doctorId)
                data.put("doctor_name", sessionManager.getUserName())
                data.put("reason", "Cancelled by Doctor")
                socket?.emit("appointment_cancelled", data)
            } catch (e: Exception) {
                Log.e("CreateSlotActivity", "Error emitting cancellation: ${e.message}")
            }
        }

        // Undo Option
        val rootView = findViewById<View>(android.R.id.content)
        Snackbar.make(rootView, if (action == "delete") "Slot deleted" else "Slot cancelled", Snackbar.LENGTH_LONG)
            .setAction("Undo") {
                undoSlotAction(slot)
            }
            .setDuration(5000)
            .show()
    }

    private fun undoSlotAction(slot: com.simats.Tmapp.api.AvailabilityResponse) {
        val request = com.simats.Tmapp.api.PostAvailabilityRequest(
            userId = doctorId,
            role = "doctor",
            date = slot.date,
            timeSlot = slot.time,
            timezone = java.util.TimeZone.getDefault().id
        )
        
        ApiClient.instance.postAvailability(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@CreateSlotActivity, "Slot restored", Toast.LENGTH_SHORT).show()
                    loadSlots()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@CreateSlotActivity, "Failed to restore slot", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun resetInputs() {
        selectedDate = ""
        startTime = ""
        btnDatePicker.text = "Select Date"
        btnStartTimePicker.text = "Select Time"
    }

    private fun resetBulkInputs() {
        bulkDate = ""
        bulkStart = ""
        bulkEnd = ""
        btnBulkDatePicker.text = "Select Date"
        btnBulkStartTime.text = "Start"
        btnBulkEndTime.text = "End"
    }
}
