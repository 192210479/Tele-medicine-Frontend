package com.simats.Tmapp

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreatePrescriptionActivity : AppCompatActivity() {
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
        val patientIdStr = intent.getStringExtra("patient_id")
        patientId = patientIdStr?.toIntOrNull() ?: intent.getIntExtra("patient_id", -1)
        val consultationId = intent.getIntExtra("consultation_id", -1)

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
        val patientName = intent.getStringExtra("patient_name") ?: "Patient"
        val patientAge = intent.getStringExtra("patient_age") ?: intent.getIntExtra("patient_age", -1).let { if (it == -1) "--" else it.toString() }
        val patientGender = intent.getStringExtra("patient_gender") ?: "--"
        val pId = intent.getStringExtra("patient_id") ?: intent.getIntExtra("patient_id", -1).let { if (it == -1) "--" else it.toString() }

        findViewById<TextView>(R.id.tvPatientName).text = patientName
        findViewById<TextView>(R.id.tvPatientId).text = "Patient ID: #$pId"
        findViewById<TextView>(R.id.tvPatientDetails).text = "Age: $patientAge | Gender: $patientGender"

        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        com.bumptech.glide.Glide.with(this@CreatePrescriptionActivity)
            .load("$baseUrl/api/profile/image/${pId}?role=patient")
            .placeholder(R.drawable.bg_circle_soft_blue)
            .circleCrop()
            .into(findViewById(R.id.ivPatientAvatar))
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
        val consultationId = intent.getIntExtra("consultation_id", -1)
        if (consultationId == -1 && appointmentId == -1) {
            Toast.makeText(this, "Invalid consultation session", Toast.LENGTH_SHORT).show()
            return
        }

        if (medicinesList.isEmpty()) {
            Toast.makeText(this, "Add at least one medicine", Toast.LENGTH_SHORT).show()
            return
        }

        val etDiagnosis = findViewById<EditText>(R.id.etDiagnosis)
        val etAdvice = findViewById<EditText>(R.id.etAdvice)

        val request = com.simats.Tmapp.api.CreatePrescriptionRequestV2(
            consultationId = consultationId,
            diagnosis = etDiagnosis.text.toString(),
            advice = etAdvice.text.toString(),
            medicines = medicinesList
        )

        ApiClient.instance.createPrescriptionV3(request)
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CreatePrescriptionActivity, "Prescription sent successfully", Toast.LENGTH_SHORT).show()
                        setResult(android.app.Activity.RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@CreatePrescriptionActivity, "Prescription creation failed", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@CreatePrescriptionActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

// Methods addMedicinesSequentially and markPrescriptionReady removed as they are now handled by the aggregated endpoint.
}
