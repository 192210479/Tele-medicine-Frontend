package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.DoctorResponse
import com.simats.Tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorProfileActivity : AppCompatActivity() {

    private var doctorId: Int = -1
    private var doctorNameStr: String = ""
    private var doctorFeeValue: Double = 0.0
    private lateinit var tvDoctorName: TextView
    private lateinit var tvSpecialization: TextView
    private lateinit var tvExperience: TextView
    private lateinit var tvFee: TextView
    private lateinit var tvLanguages: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvReviews: TextView
    private lateinit var tvProfileRating: TextView
    private lateinit var ivDoctorImage: ImageView
    private lateinit var sessionManager: SessionManager

    private var currentSymptoms: String? = null
    private var currentPriority: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_profile)

        sessionManager = SessionManager.getInstance(this)

        // Read extras
        doctorId = intent.getIntExtra("doctor_id", -1)
        currentSymptoms = intent.getStringExtra("symptoms")
        currentPriority = intent.getStringExtra("priority")
        doctorNameStr = intent.getStringExtra("doctor_name") ?: ""
        doctorFeeValue = intent.getDoubleExtra("doctor_fee", 0.0)

        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvSpecialization = findViewById(R.id.tvSpecialization)
        tvExperience = findViewById(R.id.tvExperience)
        tvFee = findViewById(R.id.tvFee)
        tvLanguages = findViewById(R.id.tvLanguages)
        tvBio = findViewById(R.id.tvBio)
        tvReviews = findViewById(R.id.tvReviews)
        tvProfileRating = findViewById(R.id.tvProfileRating)
        ivDoctorImage = findViewById(R.id.ivDoctorImage)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val btnBookAppointment = findViewById<MaterialButton>(R.id.btnBookAppointment)

        ivBack.setOnClickListener {
            finish()
        }

        btnBookAppointment.setOnClickListener {
            val intent = Intent(this, SelectTimeActivity::class.java)
            intent.putExtra("doctor_id", doctorId)

            // Pass doctor info for booking + payment continuity
            intent.putExtra("doctor_name", doctorNameStr)
            intent.putExtra("doctor_fee", doctorFeeValue)

            val doctorImageTag = ivDoctorImage.tag as? String
            if (!doctorImageTag.isNullOrEmpty()) {
                intent.putExtra("doctor_photo", doctorImageTag)
            }

            if (currentSymptoms != null) {
                intent.putExtra("symptoms", currentSymptoms)
                intent.putExtra("priority", currentPriority)
            }

            startActivity(intent)
        }

        // Setup Bottom Navigation
        findViewById<com.simats.Tmapp.GlobalBottomNavigationView>(R.id.bottomNav)

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
                        doctorNameStr = it.name ?: ""
                        doctorFeeValue = it.fee ?: 0.0

                        tvDoctorName.text = it.name
                        tvSpecialization.text = it.specialization
                        tvExperience.text = "${it.experience ?: 0} Years"
                        tvFee.text = "₹${it.fee ?: 0.0}"
                        tvLanguages.text = it.languages ?: "English"
                        tvBio.text = it.bio ?: "Certified specialist."

                        val c = it.reviewsCount ?: 0
                        val r = if (c > 0) it.rating ?: 0.0f else 0.0f
                        tvProfileRating.text = "${"%.1f".format(r)}"
                        tvReviews.text = if (c > 0) "($c reviews)" else "(No reviews yet)"
                        
                        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
                        val avatarVersion = sessionManager.getAvatarVersion()
                        val avatarUrl = "$baseUrl/api/profile/image/${it.id}?role=doctor&v=$avatarVersion"

                        Glide.with(this@DoctorProfileActivity)
                            .load(avatarUrl)
                            .dontAnimate()
                            .placeholder(R.drawable.bg_circle_soft_blue)
                            .error(R.drawable.bg_circle_soft_blue)
                            .circleCrop()
                            .into(ivDoctorImage)

                        val finalDoctorImage = when {
                            !it.profileImage.isNullOrEmpty() && it.profileImage!!.startsWith("http") -> it.profileImage
                            !it.profileImage.isNullOrEmpty() && it.profileImage!!.startsWith("/") -> "$baseUrl${it.profileImage}"
                            it.id != null -> "$baseUrl/api/profile/image/${it.id}?role=doctor"
                            else -> null
                        }

                        ivDoctorImage.tag = finalDoctorImage

                        AvatarUtils.loadAvatar(
                            imageView = ivDoctorImage,
                            imageUrl = finalDoctorImage,
                            name = it.name ?: "Doctor"
                        )
                    }
                }
            }

            override fun onFailure(call: Call<DoctorResponse>, t: Throwable) {
                // If API fails, we already have data from extras, so just log or silent fail
            }
        })
    }
}
