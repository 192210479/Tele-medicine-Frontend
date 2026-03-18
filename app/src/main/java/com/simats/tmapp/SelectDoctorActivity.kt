package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.tmapp.api.DoctorResponse
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SelectDoctorActivity : AppCompatActivity() {

    private lateinit var rvDoctors: RecyclerView
    private lateinit var adapter: DoctorAdapter

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

        // Fetch doctors from backend
        fetchDoctors()

        setupBottomNavigation()
    }

    private fun fetchDoctors() {
        ApiClient.instance.getDoctors().enqueue(object : Callback<List<DoctorResponse>> {
            override fun onResponse(call: Call<List<DoctorResponse>>, response: Response<List<DoctorResponse>>) {
                if (response.isSuccessful) {
                    val doctors = response.body() ?: emptyList()
                    adapter.updateDoctors(doctors)
                } else {
                    Toast.makeText(this@SelectDoctorActivity, "Failed to load doctors", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<DoctorResponse>>, t: Throwable) {
                Toast.makeText(this@SelectDoctorActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToDoctor(doc: DoctorResponse) {
        val intent = Intent(this, DoctorProfileActivity::class.java)
        intent.putExtra("doctor_id", doc.id)
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
