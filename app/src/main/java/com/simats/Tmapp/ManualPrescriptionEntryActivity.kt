package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManualPrescriptionEntryActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private var prescriptionId: Int = -1
    private var appointmentId: Int = -1
    private lateinit var llMedicineList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_prescription_entry)

        sessionManager = SessionManager.getInstance(this)
        prescriptionId = intent.getIntExtra("prescription_id", -1)
        appointmentId = intent.getIntExtra("appointment_id", -1)

        if (appointmentId == -1) {
            Toast.makeText(this, "Empty Appointment Session", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        llMedicineList = findViewById(R.id.llMedicineList)
        val etName = findViewById<EditText>(R.id.etMedicineName)
        val etDosage = findViewById<EditText>(R.id.etDosage)
        val etFrequency = findViewById<EditText>(R.id.etFrequency)
        val etDuration = findViewById<EditText>(R.id.etDuration)
        val etInstructions = findViewById<EditText>(R.id.etInstructions)
        val btnAdd = findViewById<MaterialButton>(R.id.btnAddMedicine)
        val btnSend = findViewById<MaterialButton>(R.id.btnSendPrescription)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()
            val dosage = etDosage.text.toString().trim()
            val frequency = etFrequency.text.toString().trim()
            val duration = etDuration.text.toString().trim()
            val instructions = etInstructions.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Required"
                return@setOnClickListener
            }

            addMedicineToAPI(name, dosage, frequency, duration, instructions)
        }

        btnSend.setOnClickListener {
            sendPrescription()
        }
    }

    private fun addMedicineToAPI(name: String, dosage: String, frequency: String, duration: String, instructions: String) {
        if (appointmentId == -1) {
            Toast.makeText(this, "No appointment ID", Toast.LENGTH_SHORT).show()
            return
        }

        if (prescriptionId == -1) {
            // Step 1: Create Prescription first
            val doctorId = sessionManager.getUserId()
            val body = mapOf(
                "user_id" to doctorId,
                "role" to "doctor",
                "diagnosis" to "Clinical Diagnosis", // Placeholders as per UI limitation
                "advice" to "Follow-up required"
            )

            ApiClient.instance.createPrescriptionLegacy(appointmentId, body)
                .enqueue(object : Callback<CreatePrescriptionResponse> {
                    override fun onResponse(call: Call<CreatePrescriptionResponse>, response: Response<CreatePrescriptionResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            prescriptionId = response.body()!!.prescriptionId
                            // Step 2: Add Medicine
                            performAddMedicine(name, dosage, frequency, duration, instructions)
                        } else {
                            Log.e("PrescriptionAPI", "Create failed: " + response.errorBody()?.string())
                            Toast.makeText(this@ManualPrescriptionEntryActivity, "Creation failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<CreatePrescriptionResponse>, t: Throwable) {
                        Toast.makeText(this@ManualPrescriptionEntryActivity, "Network Error", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            // Step 2: Add Medicine directly
            performAddMedicine(name, dosage, frequency, duration, instructions)
        }
    }

    private fun performAddMedicine(name: String, dosage: String, frequency: String, duration: String, instructions: String) {
        val userId = sessionManager.getUserId()
        val role = "doctor"
        val request = AddMedicineRequest(userId, role, name, dosage, frequency, duration, instructions)

        ApiClient.instance.addMedicine(prescriptionId, request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    addMedicineToUI(name, dosage, frequency, duration, instructions)
                    clearInputs()
                } else {
                    Toast.makeText(this@ManualPrescriptionEntryActivity, "Add Medicine failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ManualPrescriptionEntryActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addMedicineToUI(name: String, dosage: String, frequency: String, duration: String, instructions: String) {
        val view = layoutInflater.inflate(R.layout.item_medicine_card, llMedicineList, false)
        view.findViewById<TextView>(R.id.tvMedicineName).text = name
        view.findViewById<TextView>(R.id.tvDosageFreq).text = "$dosage • $frequency"
        view.findViewById<TextView>(R.id.tvDuration).text = duration
        view.findViewById<TextView>(R.id.tvInstructions).text = instructions
        
        view.findViewById<ImageView>(R.id.ivDelete).setOnClickListener {
            llMedicineList.removeView(view)
        }
        
        llMedicineList.addView(view)
    }

    private fun clearInputs() {
        findViewById<EditText>(R.id.etMedicineName).setText("")
        findViewById<EditText>(R.id.etDosage).setText("")
        findViewById<EditText>(R.id.etFrequency).setText("")
        findViewById<EditText>(R.id.etDuration).setText("")
        findViewById<EditText>(R.id.etInstructions).setText("")
    }

    private fun sendPrescription() {
        if (prescriptionId == -1) {
            Toast.makeText(this, "Add at least one medicine", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = sessionManager.getUserId()
        val body = mapOf(
            "user_id" to userId,
            "role" to "doctor"
        )
        ApiClient.instance.markPrescriptionReady(prescriptionId, body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ManualPrescriptionEntryActivity, "Prescription sent to patient successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@ManualPrescriptionEntryActivity, ConsultationHistoryActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@ManualPrescriptionEntryActivity, "Finalizing failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ManualPrescriptionEntryActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
