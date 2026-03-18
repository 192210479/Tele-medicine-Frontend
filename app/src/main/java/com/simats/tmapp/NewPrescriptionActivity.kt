package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.simats.tmapp.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NewPrescriptionActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private var appointmentId: Int = -1
    private var patientId: Int = -1
    private val medicinesList = mutableListOf<MedicineRequest>()
    private lateinit var adapter: MedicineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_prescription)

        sessionManager = SessionManager.getInstance(this)
        appointmentId = intent.getIntExtra("appointment_id", -1)
        patientId = intent.getIntExtra("patient_id", -1)

        if (appointmentId == -1) {
            Toast.makeText(this, "Consultation record not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val etDiagnosis = findViewById<EditText>(R.id.etDiagnosis)
        val etAdvice = findViewById<EditText>(R.id.etAdvice)
        val btnSend = findViewById<MaterialButton>(R.id.btnSendPrescription)
        val btnAddMedicine = findViewById<MaterialButton>(R.id.btnAddMedicine)
        val rvMedicines = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvMedicines)

        // Setup RecyclerView
        adapter = MedicineAdapter(medicinesList)
        rvMedicines.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvMedicines.adapter = adapter

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        fetchPatientDetails()

        btnAddMedicine.setOnClickListener {
            showAddMedicineDialog()
        }

        btnSend.setOnClickListener {
            submitPrescription()
        }
    }

    private fun fetchPatientDetails() {
        ApiClient.instance.getAppointmentDetails(appointmentId).enqueue(object : Callback<AppointmentResponse> {
            override fun onResponse(call: Call<AppointmentResponse>, response: Response<AppointmentResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val appt = response.body()!!
                    patientId = appt.patientId ?: -1
                    
                    val pName = intent.getStringExtra("patient_name") ?: appt.patientName ?: "Unknown Patient"
                    val pAge = intent.getIntExtra("patient_age", -1).let { if (it == -1) appt.patientAge else it }
                    val pGender = intent.getStringExtra("patient_gender") ?: appt.patientGender ?: "--"

                    findViewById<TextView>(R.id.tvPatientName).text = pName
                    findViewById<TextView>(R.id.tvPatientId).text = "Patient ID: #${patientId}"
                    findViewById<TextView>(R.id.tvPatientDetails).text = "Age: ${pAge ?: "--"} | Gender: $pGender"                    
                    
                    val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
                    com.bumptech.glide.Glide.with(this@NewPrescriptionActivity)
                        .load("$baseUrl/api/profile/image/${patientId}?role=patient")
                        .placeholder(R.drawable.bg_circle_soft_blue)
                        .circleCrop()
                        .into(findViewById(R.id.ivPatientAvatar))
                }
            }

            override fun onFailure(call: Call<AppointmentResponse>, t: Throwable) {
                Toast.makeText(this@NewPrescriptionActivity, "Error loading patient info", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddMedicineDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_medicine, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnAdd).setOnClickListener {
            val name = dialogView.findViewById<EditText>(R.id.etMedicineName).text.toString().trim()
            val dosage = dialogView.findViewById<EditText>(R.id.etDosage).text.toString().trim()
            val freq = dialogView.findViewById<EditText>(R.id.etFrequency).text.toString().trim()
            val dur = dialogView.findViewById<EditText>(R.id.etDuration).text.toString().trim()
            val inst = dialogView.findViewById<EditText>(R.id.etInstructions).text.toString().trim()

            if (name.isNotEmpty()) {
                medicinesList.add(MedicineRequest(name, dosage, freq, dur, inst))
                adapter.notifyItemInserted(medicinesList.size - 1)
                dialog.dismiss()
            } else {
                dialogView.findViewById<EditText>(R.id.etMedicineName).error = "Required"
            }
        }
        dialog.show()
    }

    private fun submitPrescription() {
        if (appointmentId == -1 || patientId == -1) {
            Toast.makeText(this, "Invalid appointment details", Toast.LENGTH_SHORT).show()
            return
        }

        if (medicinesList.isEmpty()) {
            Toast.makeText(this, "Add at least one medicine", Toast.LENGTH_SHORT).show()
            return
        }

        val etDiagnosis = findViewById<EditText>(R.id.etDiagnosis)
        val etAdvice = findViewById<EditText>(R.id.etAdvice)
        val doctorId = sessionManager.getUserId()

        // 1. Create Prescription
        val createBody = mapOf(
            "user_id" to doctorId,
            "role" to "doctor",
            "diagnosis" to etDiagnosis.text.toString(),
            "advice" to etAdvice.text.toString()
        )

        ApiClient.instance.createPrescriptionLegacy(appointmentId, createBody)
            .enqueue(object : Callback<CreatePrescriptionResponse> {
                override fun onResponse(call: Call<CreatePrescriptionResponse>, response: Response<CreatePrescriptionResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val prescriptionId = response.body()!!.prescriptionId
                        addMedicinesSequentially(prescriptionId, 0)
                    } else {
                        Toast.makeText(this@NewPrescriptionActivity, "Prescription creation failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CreatePrescriptionResponse>, t: Throwable) {
                    Toast.makeText(this@NewPrescriptionActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun addMedicinesSequentially(prescriptionId: Int, index: Int) {
        if (index >= medicinesList.size) {
            markPrescriptionReady(prescriptionId)
            return
        }

        val med = medicinesList[index]
        val doctorId = sessionManager.getUserId()
        val request = AddMedicineRequest(
            userId = doctorId,
            role = "doctor",
            name = med.name,
            dosage = med.dosage,
            frequency = med.frequency,
            duration = med.duration,
            instructions = med.instructions
        )

        ApiClient.instance.addMedicine(prescriptionId, request)
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        addMedicinesSequentially(prescriptionId, index + 1)
                    } else {
                        Toast.makeText(this@NewPrescriptionActivity, "Failed to add medicine ${med.name}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@NewPrescriptionActivity, "Error adding medicine: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun markPrescriptionReady(prescriptionId: Int) {
        val doctorId = sessionManager.getUserId()
        val readyBody = mapOf(
            "user_id" to doctorId,
            "role" to "doctor"
        )

        ApiClient.instance.markPrescriptionReady(prescriptionId, readyBody)
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@NewPrescriptionActivity, "Prescription sent successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@NewPrescriptionActivity, DoctorDashboardActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@NewPrescriptionActivity, "Finalizing failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@NewPrescriptionActivity, "Error finalizing: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
