package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class PrivacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        val userRole = intent.getStringExtra("userRole") ?: "patient"

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        findViewById<LinearLayout>(R.id.llChangePassword).setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.llLoginActivity).setOnClickListener {
            val intent = Intent(this, LoginHistoryActivity::class.java)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.llDeviceManagement)?.setOnClickListener {
            val intent = Intent(this, DeviceManagementActivity::class.java)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.llDataPrivacyPolicy)?.setOnClickListener {
            val intent = Intent(this, DataPrivacyActivity::class.java)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.llDeleteAccount)?.setOnClickListener {
            val intent = Intent(this, DeleteAccountActivity::class.java)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
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
