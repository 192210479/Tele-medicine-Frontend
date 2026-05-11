package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.PrescriptionResponse
import com.simats.Tmapp.api.MedicineResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PrescriptionActivity : AppCompatActivity() {
    private var appointmentId: Int = -1
    private var consultationId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prescription)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener { finish() }

        // Bottom nav
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navAppts = findViewById<LinearLayout>(R.id.navAppts)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        val sessionManager = SessionManager.getInstance(this)
        val role = sessionManager.getUserRole().lowercase()

        navHome.setOnClickListener {
            val dest = if (role.contains("doctor")) DoctorDashboardActivity::class.java else PatientDashboardActivity::class.java
            val intent = Intent(this, dest)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        navAppts.setOnClickListener {
            val dest = if (role.contains("doctor")) DoctorAppointmentsActivity::class.java else AppointmentsActivity::class.java
            val intent = Intent(this, dest)
            startActivity(intent)
            finish()
        }
        
        navProfile.setOnClickListener {
            val dest = if (role.contains("doctor")) DoctorProfileSettingsActivity::class.java else ProfileActivity::class.java
            val intent = Intent(this, dest)
            startActivity(intent)
            finish()
        }

        appointmentId = intent.getIntExtra("appointment_id", -1)
        consultationId = intent.getIntExtra("consultation_id", -1)

        if (consultationId != -1 || appointmentId != -1) {
            setupMedicineRecyclerView()
            fetchPrescription()
        } else {
            Toast.makeText(this, "Invalid Reference ID", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var rvMedicines: RecyclerView

    private fun setupMedicineRecyclerView() {
        val scrollView = findViewById<View>(R.id.vHeaderDivider).parent as ViewGroup
        var targetScrollView: android.widget.ScrollView? = null
        for (i in 0 until scrollView.childCount) {
            val child = scrollView.getChildAt(i)
            if (child is android.widget.ScrollView) {
                targetScrollView = child
                break
            }
        }
        val linearLayout = targetScrollView?.getChildAt(0) as? LinearLayout
        
        if (linearLayout != null) {
            val viewsToRemove = mutableListOf<View>()
            var insertIndex = -1
            
            for (i in 0 until linearLayout.childCount) {
                val child = linearLayout.getChildAt(i)
                if (child is TextView && child.text == "Medicines") {
                    insertIndex = i + 1
                }
                if (child is com.google.android.material.card.MaterialCardView) {
                    val cardChild = child.getChildAt(0)
                    if (cardChild is LinearLayout && cardChild.orientation == LinearLayout.HORIZONTAL) {
                        viewsToRemove.add(child)
                    }
                }
            }

            viewsToRemove.forEach { linearLayout.removeView(it) }

            rvMedicines = RecyclerView(this).apply {
                layoutManager = LinearLayoutManager(this@PrescriptionActivity)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 24 }
            }

            if (insertIndex != -1) {
                linearLayout.addView(rvMedicines, insertIndex)
            } else {
                linearLayout.addView(rvMedicines)
            }
        }
    }

    private fun fetchPrescription() {
        val sessionManager = SessionManager.getInstance(this)
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()

        val call = if (consultationId != -1) {
            ApiClient.instance.getPrescriptionByConsultation(consultationId)
        } else {
            ApiClient.instance.getPrescription(appointmentId, userId, role)
        }

        if (consultationId != -1) {
            (call as Call<com.simats.Tmapp.api.PrescriptionDetailResponse>).enqueue(object : Callback<com.simats.Tmapp.api.PrescriptionDetailResponse> {
                override fun onResponse(call: Call<com.simats.Tmapp.api.PrescriptionDetailResponse>, response: Response<com.simats.Tmapp.api.PrescriptionDetailResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        populateUI(body.diagnosis, body.advice, body.medicines.map { 
                            com.simats.Tmapp.api.MedicineResponse(it.name, it.dosage, it.frequency, it.duration, "")
                        })
                    } else {
                        Toast.makeText(this@PrescriptionActivity, "Prescription not found", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<com.simats.Tmapp.api.PrescriptionDetailResponse>, t: Throwable) {
                    Toast.makeText(this@PrescriptionActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            (call as Call<PrescriptionResponse>).enqueue(object : Callback<PrescriptionResponse> {
                override fun onResponse(call: Call<PrescriptionResponse>, response: Response<PrescriptionResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val p = response.body()!!
                        populateUI(p.diagnosis, p.advice, p.medicines)
                    } else {
                        Toast.makeText(this@PrescriptionActivity, "Prescription not found", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<PrescriptionResponse>, t: Throwable) {
                    Toast.makeText(this@PrescriptionActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun populateUI(diagnosis: String?, advice: String?, medicines: List<com.simats.Tmapp.api.MedicineResponse>?) {
        val scrollView = findViewById<View>(R.id.vHeaderDivider).parent as ViewGroup
        var linearLayout: LinearLayout? = null
        for (i in 0 until scrollView.childCount) {
            val child = scrollView.getChildAt(i)
            if (child is android.widget.ScrollView) {
                linearLayout = child.getChildAt(0) as? LinearLayout
                break
            }
        }

        if (linearLayout != null) {
            var diagnosisCardSet = false
            var adviceCardSet = false

            for (i in 0 until linearLayout.childCount) {
                val child = linearLayout.getChildAt(i)
                if (child is com.google.android.material.card.MaterialCardView) {
                    val cardContent = child.getChildAt(0) as? LinearLayout
                    if (cardContent != null && cardContent.orientation == LinearLayout.VERTICAL) {
                        val firstText = cardContent.getChildAt(0) as? TextView
                        val secondText = cardContent.getChildAt(1) as? TextView
                        
                        if (firstText?.text == "Doctor's Advice") {
                            secondText?.text = advice ?: ""
                            adviceCardSet = true
                        } else if (!diagnosisCardSet && firstText?.text != "Cardiologist • MBBS, MD") {
                            firstText?.text = diagnosis ?: ""
                            secondText?.text = ""
                            diagnosisCardSet = true
                        }
                    }
                }
            }
        }
        
        rvMedicines.adapter = MedicinesAdapter(medicines ?: emptyList())
    }

    private class MedicinesAdapter(private val medicines: List<MedicineResponse>) : RecyclerView.Adapter<MedicinesAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(1)
            val tvDoseFreq: TextView = view.findViewById(2)
            val tvDuration: TextView = view.findViewById(3)
            val tvInstructions: TextView = view.findViewById(4)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val context = parent.context
            
            // Programmatically building the material card view as we can't create or change XML layout files safely without risking missing definitions.
            val card = com.google.android.material.card.MaterialCardView(context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 40 }
                setCardBackgroundColor(android.graphics.Color.WHITE)
                radius = 48f
                cardElevation = 0f
                strokeWidth = 3
                strokeColor = android.graphics.Color.parseColor("#F1F5F9")
            }

            val outerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(60, 60, 60, 60)
            }

            val iconFrame = android.widget.FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(144, 144)
                background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_rounded_soft_green_8dp)
            }

            val icon = android.widget.ImageView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(90, 90).apply {
                    gravity = android.view.Gravity.CENTER
                }
                setImageResource(R.drawable.ic_pill)
                imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#10B981"))
            }
            iconFrame.addView(icon)
            outerLayout.addView(iconFrame)

            // Text Layout
            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 48
                }
            }

            val tvName = TextView(context).apply {
                id = 1
                setTextColor(android.graphics.Color.BLACK)
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, 12)
            }

            val tvDoseFreq = TextView(context).apply {
                id = 2
                setTextColor(android.graphics.Color.GRAY)
                textSize = 14f
                setPadding(0, 0, 0, 36)
            }

            val tagsLayout = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            
            val tvDuration = TextView(context).apply {
                id = 3
                background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_rounded_soft_gray_12dp)
                setPadding(36, 18, 36, 18)
                setTextColor(android.graphics.Color.BLACK)
                textSize = 12f
            }
            val tvInstructions = TextView(context).apply {
                id = 4
                background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_rounded_soft_gray_12dp)
                setPadding(36, 18, 36, 18)
                setTextColor(android.graphics.Color.BLACK)
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginStart = 24
                }
            }
            tagsLayout.addView(tvDuration)
            tagsLayout.addView(tvInstructions)

            textLayout.addView(tvName)
            textLayout.addView(tvDoseFreq)
            textLayout.addView(tagsLayout)

            outerLayout.addView(textLayout)
            card.addView(outerLayout)

            return ViewHolder(card)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val med = medicines[position]
            holder.tvName.text = med.name
            holder.tvDoseFreq.text = "${med.dosage} • ${med.frequency}"
            holder.tvDuration.text = med.duration
            holder.tvInstructions.text = med.instructions
        }

        override fun getItemCount() = medicines.size
    }
}
