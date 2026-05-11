package com.simats.Tmapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutAppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_app)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvTerms).setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse("https://policies.google.com/terms")
            startActivity(i)
        }

        val userRole = intent.getStringExtra("userRole") ?: "patient"

        if (userRole == "doctor") {
            findViewById<TextView>(R.id.tvFeature3Title)?.text = "Doctor-Centered Experience"
            findViewById<TextView>(R.id.tvFeature3Desc)?.text = "Designed to simplify your schedule, patient records, and consultation management."
        } else if (userRole == "admin") {
            findViewById<TextView>(R.id.tvFeature3Title)?.text = "Admin-Centered Control"
            findViewById<TextView>(R.id.tvFeature3Desc)?.text = "Complete platform oversight, user management, and seamless administrative control."
        }

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val dashboardIntent = when (userRole) {
                "admin" -> Intent(this, AdminDashboardActivity::class.java)
                "doctor" -> Intent(this, DoctorDashboardActivity::class.java)
                else -> Intent(this, PatientDashboardActivity::class.java)
            }
            dashboardIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(dashboardIntent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navBook)?.setOnClickListener {
            if (userRole == "admin") {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            } else if (userRole == "doctor") {
                startActivity(Intent(this, DoctorAppointmentsActivity::class.java))
            } else {
                startActivity(Intent(this, SelectDoctorActivity::class.java))
            }
        }

        findViewById<LinearLayout>(R.id.navHistory)?.setOnClickListener {
            if (userRole == "admin") {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            } else if (userRole != "doctor") {
                startActivity(Intent(this, ConsultationHistoryActivity::class.java))
            }
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            val profileIntent = when (userRole) {
                "admin" -> Intent(this, AdminProfileActivity::class.java)
                "doctor" -> Intent(this, DoctorProfileSettingsActivity::class.java)
                else -> Intent(this, ProfileActivity::class.java)
            }
            profileIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(profileIntent)
            finish()
        }
    }
}
