package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.simats.tmapp.api.DoctorResponse
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorProfileActivity : AppCompatActivity() {

    private var doctorId: Int = -1
    private lateinit var tvDoctorName: TextView
    private lateinit var tvSpecialization: TextView
    private lateinit var tvExperience: TextView
    private lateinit var tvFee: TextView
    private lateinit var tvLanguages: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvReviews: TextView
    private lateinit var ivDoctorImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_profile)

        // Read extras
        doctorId = intent.getIntExtra("doctor_id", -1)

        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvSpecialization = findViewById(R.id.tvSpecialization)
        tvExperience = findViewById(R.id.tvExperience)
        tvFee = findViewById(R.id.tvFee)
        tvLanguages = findViewById(R.id.tvLanguages)
        tvBio = findViewById(R.id.tvBio)
        tvReviews = findViewById(R.id.tvReviews)
        ivDoctorImage = findViewById(R.id.ivDoctorImage)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val btnBookAppointment = findViewById<MaterialButton>(R.id.btnBookAppointment)

        ivBack.setOnClickListener {
            finish()
        }

        btnBookAppointment.setOnClickListener {
            val intent = Intent(this, SelectTimeActivity::class.java)
            intent.putExtra("doctor_id", doctorId)
            startActivity(intent)
        }

        // Setup Bottom Navigation
        findViewById<com.simats.tmapp.GlobalBottomNavigationView>(R.id.bottomNav)

        if (doctorId != -1) {
            fetchDoctorDetails()
        }
    }

    private fun fetchDoctorDetails() {
        ApiClient.instance.getDoctorDetails(doctorId).enqueue(object : Callback<DoctorResponse> {
            override fun onResponse(call: Call<DoctorResponse>, response: Response<DoctorResponse>) {
                if (response.isSuccessful) {
                    val doctor = response.body()
                    doctor?.let {
                        tvDoctorName.text = it.name
                        tvSpecialization.text = it.specialization
                        tvExperience.text = "${it.experience ?: 0} Years"
                        tvFee.text = "$${it.fee ?: 0.0}"
                        tvLanguages.text = it.languages ?: "English"
                        tvBio.text = it.bio ?: "Certified specialist."
                        
                        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
                        val avatarUrl = "$baseUrl/api/profile/image/${it.id}"
                        Glide.with(this@DoctorProfileActivity)
                            .load(avatarUrl)
                            .placeholder(R.drawable.bg_circle_soft_blue)
                            .error(R.drawable.bg_circle_soft_blue)
                            .circleCrop()
                            .into(ivDoctorImage)
                    }
                }
            }

            override fun onFailure(call: Call<DoctorResponse>, t: Throwable) {
                // If API fails, we already have data from extras, so just log or silent fail
            }
        })
    }
}
