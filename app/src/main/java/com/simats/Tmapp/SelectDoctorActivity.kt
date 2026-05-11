package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.DoctorResponse
import com.simats.Tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SelectDoctorActivity : AppCompatActivity() {

    private lateinit var rvDoctors: RecyclerView
    private lateinit var adapter: DoctorAdapter

    private var currentSymptoms: String? = null
    private var currentPriority: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_doctor)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        rvDoctors = findViewById(R.id.rvDoctors)
        rvDoctors.layoutManager = LinearLayoutManager(this)
        
        adapter = DoctorAdapter(emptyList(), { doc ->
            navigateToDoctor(doc)
        }, { doc ->
            navigateToDoctor(doc)
        })
        rvDoctors.adapter = adapter
        
        ivBack.setOnClickListener {
            finish()
        }

        val etSymptoms = findViewById<android.widget.EditText>(R.id.etSymptoms)
        val btnCheckAi = findViewById<MaterialButton>(R.id.btnCheckAi)
        val cvAiResult = findViewById<androidx.cardview.widget.CardView>(R.id.cvAiResult)
        val tvAiConditions = findViewById<TextView>(R.id.tvAiConditions)
        val tvAiSpecialist = findViewById<TextView>(R.id.tvAiSpecialist)
        val tvAiPriority = findViewById<TextView>(R.id.tvAiPriority)
        val tvAiDisclaimer = findViewById<TextView>(R.id.tvAiDisclaimer)

        btnCheckAi.setOnClickListener {
            val symptoms = etSymptoms.text.toString().trim()
            if (symptoms.isEmpty()) {
                etSymptoms.error = "Please enter your symptoms"
                return@setOnClickListener
            }
            
            btnCheckAi.isEnabled = false
            btnCheckAi.text = "Loading..."

            val request = com.simats.Tmapp.api.AITriageRequest(symptoms)
            ApiClient.instance.analyzeSymptoms(request).enqueue(object : Callback<com.simats.Tmapp.api.AITriageResponse> {
                override fun onResponse(call: Call<com.simats.Tmapp.api.AITriageResponse>, response: Response<com.simats.Tmapp.api.AITriageResponse>) {
                    btnCheckAi.isEnabled = true
                    btnCheckAi.text = "Check\nwith AI"
                    if (response.isSuccessful && response.body() != null) {
                        val aiResult = response.body()!!
                        cvAiResult.visibility = View.VISIBLE
                        tvAiConditions.text = "Possible Conditions: ${aiResult.conditions?.joinToString(", ") ?: "N/A"}"
                        tvAiSpecialist.text = "Recommended Specialist: ${aiResult.specialization ?: "General"}"
                        tvAiPriority.text = "Priority: ${aiResult.priority ?: "low"}"
                        tvAiDisclaimer.text = aiResult.disclaimer ?: "AI guidance. Not a final diagnosis."
                        
                        currentSymptoms = symptoms
                        currentPriority = aiResult.priority ?: "low"
                        
                        val spec = aiResult.specialization ?: "General Physician"
                        fetchRecommendedDoctors(spec)
                    } else {
                        Toast.makeText(this@SelectDoctorActivity, "AI Analysis failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<com.simats.Tmapp.api.AITriageResponse>, t: Throwable) {
                    btnCheckAi.isEnabled = true
                    btnCheckAi.text = "Check\nwith AI"
                    Toast.makeText(this@SelectDoctorActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // Fetch doctors from backend
        fetchDoctors()

        setupBottomNavigation()
    }

    private fun fetchDoctors() {
        val call = ApiClient.instance.getDoctors()
        android.util.Log.d("DoctorAPI", "Request URL: ${call.request().url}")

        call.enqueue(object : Callback<List<DoctorResponse>> {
            override fun onResponse(call: Call<List<DoctorResponse>>, response: Response<List<DoctorResponse>>) {
                try {
                    val statusCode = response.code()
                    android.util.Log.d("DoctorAPI", "Response status: $statusCode")

                    if (!response.isSuccessful || statusCode != 200) {
                        android.util.Log.e("DoctorAPI", "Response error JSON: ${response.errorBody()?.string()}")
                        Toast.makeText(this@SelectDoctorActivity, "Failed to load doctors", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val doctors = response.body()
                    android.util.Log.d("DoctorAPI", "Response JSON: $doctors")

                    val data = doctors ?: emptyList()
                    if (data.isNotEmpty()) {
                        adapter.updateDoctors(data)
                    } else {
                        // Empty array is correctly treated as not an error
                        adapter.updateDoctors(emptyList())
                        Toast.makeText(this@SelectDoctorActivity, "No doctors available", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DoctorAPI", "Catch block error: ${e.message}", e)
                    Toast.makeText(this@SelectDoctorActivity, "Failed to load doctors", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<DoctorResponse>>, t: Throwable) {
                android.util.Log.e("DoctorAPI", "Network error: ${t.message}", t)
                // Only network errors should trigger "Failed to load doctors"
                Toast.makeText(this@SelectDoctorActivity, "Failed to load doctors", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchRecommendedDoctors(specialization: String) {
        val request = com.simats.Tmapp.api.AIRecommendRequest(specialization)
        ApiClient.instance.recommendDoctor(request).enqueue(object: Callback<List<DoctorResponse>> {
            override fun onResponse(call: Call<List<DoctorResponse>>, response: Response<List<DoctorResponse>>) {
                if (response.isSuccessful) {
                    val recommendedDoctors = response.body() ?: emptyList()
                    if (recommendedDoctors.isNotEmpty()) {
                        adapter.updateDoctors(recommendedDoctors)
                        Toast.makeText(this@SelectDoctorActivity, "Filtered doctors by $specialization", Toast.LENGTH_SHORT).show()
                    } else {
                        adapter.updateDoctors(emptyList())
                        Toast.makeText(this@SelectDoctorActivity, "No doctors found for $specialization", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@SelectDoctorActivity, "Failed to load recommended doctors", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<DoctorResponse>>, t: Throwable) {
                Toast.makeText(this@SelectDoctorActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToDoctor(doc: DoctorResponse) {
        val intent = Intent(this, DoctorProfileActivity::class.java)
        intent.putExtra("doctor_id", doc.id)

        // Pass doctor payment-related info safely forward
        intent.putExtra("doctor_name", doc.name ?: "")
        intent.putExtra("doctor_fee", doc.fee ?: 0.0)

        val etSymptoms = findViewById<android.widget.EditText>(R.id.etSymptoms)
        val symptomsText = etSymptoms.text.toString().trim()
        val finalSymptoms = if (symptomsText.isNotEmpty()) symptomsText else currentSymptoms

        if (!finalSymptoms.isNullOrEmpty()) {
            intent.putExtra("symptoms", finalSymptoms)
            intent.putExtra("priority", currentPriority ?: "low")
        }

        startActivity(intent)
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, PatientDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navHistory)?.setOnClickListener {
            startActivity(Intent(this, ConsultationHistoryActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
