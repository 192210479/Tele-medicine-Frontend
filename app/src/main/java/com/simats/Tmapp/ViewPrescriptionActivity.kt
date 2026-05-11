package com.simats.Tmapp

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.MedicineResponse
import com.simats.Tmapp.api.PrescriptionResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class ViewPrescriptionActivity : AppCompatActivity() {

    private var appointmentId: Int = -1
    private lateinit var rvMedicines: RecyclerView
    private lateinit var tvDoctorName: TextView
    private lateinit var tvDoctorSpecialty: TextView
    private lateinit var tvDiagnosis: TextView
    private lateinit var tvAdvice: TextView
    private lateinit var tvNoMedicines: TextView
    
    private var currentPrescription: PrescriptionResponse? = null
    private var doctorName: String = "Doctor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_prescription)

        appointmentId = intent.getIntExtra("appointment_id", -1)
        val doctorNameStr = intent.getStringExtra("doctor_name")
        doctorName = if (!doctorNameStr.isNullOrEmpty()) "Dr. $doctorNameStr" else "Doctor"
        val doctorSpecialization = intent.getStringExtra("doctor_specialization") ?: "Specialist"
        val doctorHospital = intent.getStringExtra("doctor_hospital") ?: ""
        val doctorPhoto = intent.getStringExtra("doctor_photo") ?: ""

        initViews(doctorSpecialization, doctorHospital, doctorPhoto)
        fetchPrescriptionDetails()
    }

    private fun initViews(doctorSpecialization: String, doctorHospital: String, doctorPhoto: String) {
        rvMedicines = findViewById(R.id.rvMedicines)
        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvDoctorSpecialty = findViewById(R.id.tvDoctorSpecialty)
        tvDiagnosis = findViewById(R.id.tvDiagnosis)
        tvAdvice = findViewById(R.id.tvAdvice)
        tvNoMedicines = findViewById(R.id.tvNoMedicines)

        tvDoctorName.text = doctorName
        tvDoctorSpecialty.text = if (doctorHospital.isNotEmpty()) "$doctorSpecialization • $doctorHospital" else doctorSpecialization
        
        val ivDoctor = findViewById<ImageView>(R.id.ivDoctorImage)
        if (ivDoctor != null) {
            val doctorId = intent.getIntExtra("doctor_id", -1)
            val baseUrl = ApiClient.BASE_URL.removeSuffix("/")

            val finalDoctorImage = when {
                doctorPhoto.isNotEmpty() && doctorPhoto.startsWith("http") -> doctorPhoto
                doctorPhoto.isNotEmpty() && doctorPhoto.startsWith("/") -> "$baseUrl$doctorPhoto"
                doctorId != -1 -> "$baseUrl/api/profile/image/$doctorId?role=doctor"
                else -> null
            }

            AvatarUtils.loadAvatar(
                imageView = ivDoctor,
                imageUrl = finalDoctorImage,
                name = doctorName
            )
        }
        
        rvMedicines.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        
        findViewById<View>(R.id.btnShare).setOnClickListener { sharePrescription() }
        findViewById<View>(R.id.btnDownload).setOnClickListener { downloadPDF() }

        val btnExplainAi = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExplainAi)
        val cvAiExplanation = findViewById<androidx.cardview.widget.CardView>(R.id.cvAiExplanation)
        val tvAiSummary = findViewById<TextView>(R.id.tvAiSummary)
        val llAiMedicinesContainer = findViewById<android.widget.LinearLayout>(R.id.llAiMedicinesContainer)

        btnExplainAi.setOnClickListener {
            val prescription = currentPrescription
            val meds = prescription?.medicines
            if (meds.isNullOrEmpty()) {
                Toast.makeText(this, "No medicines to explain.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnExplainAi.isEnabled = false
            btnExplainAi.text = "Explaining..."

            val medicineParams = meds.map { com.simats.Tmapp.api.MedicineParam(it.name ?: "", (it.dosage ?: "") + " " + (it.frequency ?: "")) }
            val request = com.simats.Tmapp.api.AIExplainPrescriptionRequest(medicineParams)

            ApiClient.instance.explainPrescription(request).enqueue(object : Callback<com.simats.Tmapp.api.AIExplainPrescriptionResponse> {
                override fun onResponse(call: Call<com.simats.Tmapp.api.AIExplainPrescriptionResponse>, response: Response<com.simats.Tmapp.api.AIExplainPrescriptionResponse>) {
                    btnExplainAi.isEnabled = true
                    btnExplainAi.text = "Simplify Prescription"
                    
                    if (response.isSuccessful && response.body() != null) {
                        val aiResult = response.body()!!
                        cvAiExplanation.visibility = View.VISIBLE
                        btnExplainAi.visibility = View.GONE // Hide button after success
                        
                        tvAiSummary.text = aiResult.summary ?: "Simplified explanation:"
                        
                        llAiMedicinesContainer.removeAllViews()
                        aiResult.medicines?.forEach { med ->
                            val medView = TextView(this@ViewPrescriptionActivity)
                            
                            val name = med.name ?: "Unknown"
                            val dosage = med.dosage ?: ""
                            val purpose = med.purpose ?: "No purpose provided"
                            val precaution = med.precaution ?: "No specific precautions"
                            
                            // Use HTML formatting for bold/italic text
                            val textStr = "<b>$name</b> ($dosage)<br/><i>Purpose:</i> $purpose<br/><i>Precaution:</i> $precaution<br/><br/>"
                            medView.text = android.text.Html.fromHtml(textStr, android.text.Html.FROM_HTML_MODE_COMPACT)
                            medView.setTextColor(Color.parseColor("#334155"))
                            medView.textSize = 14f
                            
                            llAiMedicinesContainer.addView(medView)
                        }
                    } else {
                        Toast.makeText(this@ViewPrescriptionActivity, "AI Explanation failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<com.simats.Tmapp.api.AIExplainPrescriptionResponse>, t: Throwable) {
                    btnExplainAi.isEnabled = true
                    btnExplainAi.text = "Simplify Prescription"
                    Toast.makeText(this@ViewPrescriptionActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun fetchPrescriptionDetails() {
        if (appointmentId == -1) {
            Toast.makeText(this, "Invalid Appointment ID", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionManager = SessionManager.getInstance(this)
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()

        ApiClient.instance.getPrescription(appointmentId, userId, role).enqueue(object : Callback<PrescriptionResponse> {
            override fun onResponse(call: Call<PrescriptionResponse>, response: Response<PrescriptionResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    currentPrescription = response.body()
                    populateUI(currentPrescription!!)
                } else {
                    showErrorDialog()
                }
            }

            override fun onFailure(call: Call<PrescriptionResponse>, t: Throwable) {
                showErrorDialog()
            }
        })
    }

    private fun showErrorDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage("Unable to load prescription. Please try again.")
            .setPositiveButton("Retry") { _, _ -> fetchPrescriptionDetails() }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun populateUI(prescription: PrescriptionResponse) {
        tvDiagnosis.text = prescription.diagnosis ?: "No diagnosis details provided"
        tvAdvice.text = prescription.advice ?: "No additional advice provided"

        val meds = prescription.medicines ?: emptyList()
        val btnExplainAi = findViewById<View>(R.id.btnExplainAi)
        if (meds.isEmpty()) {
            rvMedicines.visibility = View.GONE
            tvNoMedicines.visibility = View.VISIBLE
            btnExplainAi.visibility = View.GONE
        } else {
            rvMedicines.visibility = View.VISIBLE
            tvNoMedicines.visibility = View.GONE
            rvMedicines.adapter = MedicinesAdapter(meds)
            btnExplainAi.visibility = View.VISIBLE
        }
    }

    private fun sharePrescription() {
        val p = currentPrescription
        if (p == null) {
            Toast.makeText(this, "Prescription data unavailable for sharing", Toast.LENGTH_SHORT).show()
            return
        }

        val shareText = StringBuilder()
        shareText.append("Medical Prescription\n")
        shareText.append("Doctor: ${doctorName}\n\n")
        shareText.append("Diagnosis: ${p.diagnosis ?: "Not specified"}\n\n")
        shareText.append("Medicines:\n")
        
        val meds = p.medicines ?: emptyList()
        if (meds.isEmpty()) {
            shareText.append("No medicines prescribed\n")
        } else {
            meds.forEach {
                shareText.append("- ${it.name ?: "Unknown"} (${it.dosage ?: ""}): ${it.frequency ?: ""} for ${it.duration ?: ""}\n")
                if (!it.instructions.isNullOrEmpty()) shareText.append("  Instructions: ${it.instructions}\n")
            }
        }
        shareText.append("\nAdvice: ${p.advice ?: "No additional advice provided"}")

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, "Medical Prescription")
        intent.putExtra(Intent.EXTRA_TEXT, shareText.toString())
        startActivity(Intent.createChooser(intent, "Share via"))
    }

    private fun downloadPDF() {
        val p = currentPrescription
        if (p == null) {
            Toast.makeText(this, "Prescription data unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        paint.color = Color.BLACK
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Prescription", 200f, 50f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Doctor: $doctorName", 50f, 100f, paint)
        
        paint.isFakeBoldText = false
        canvas.drawText("Diagnosis: ${p.diagnosis ?: "Not specified"}", 50f, 130f, paint)
        
        paint.isFakeBoldText = true
        canvas.drawText("Medicines:", 50f, 170f, paint)
        
        paint.isFakeBoldText = false
        var yPos = 200f
        
        val meds = p.medicines ?: emptyList()
        if (meds.isEmpty()) {
            canvas.drawText("No medicines prescribed", 70f, yPos, paint)
            yPos += 20f
        } else {
            meds.forEach {
                canvas.drawText("${it.name ?: "Unknown"} - ${it.dosage ?: ""} (${it.frequency ?: ""})", 70f, yPos, paint)
                yPos += 20f
                if (!it.instructions.isNullOrEmpty()) {
                    canvas.drawText("Instr: ${it.instructions}", 90f, yPos, paint)
                    yPos += 20f
                }
            }
        }

        yPos += 30f
        paint.isFakeBoldText = true
        canvas.drawText("Doctor's Advice:", 50f, yPos, paint)
        paint.isFakeBoldText = false
        canvas.drawText(p.advice ?: "No additional advice provided", 70f, yPos + 25f, paint)

        pdfDocument.finishPage(page)


        val fileName = "Prescription_${appointmentId}.pdf"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this, "PDF Saved to Downloads", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error generating PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private inner class MedicinesAdapter(private val medicines: List<MedicineResponse>) :
        RecyclerView.Adapter<MedicinesAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvMedicineName)
            val tvDoseFreq: TextView = view.findViewById(R.id.tvDosageFreq)
            val tvDuration: TextView = view.findViewById(R.id.tvDuration)
            val tvInstructions: TextView = view.findViewById(R.id.tvInstructions)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medicine_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val med = medicines[position]
            holder.tvName.text = med.name ?: "Unknown"
            holder.tvDoseFreq.text = "${med.dosage ?: ""} • ${med.frequency ?: ""}".trim(' ', '•')
            holder.tvDuration.text = med.duration ?: ""
            holder.tvInstructions.text = if (med.instructions.isNullOrEmpty()) "General" else med.instructions
        }

        override fun getItemCount() = medicines.size
    }
}
