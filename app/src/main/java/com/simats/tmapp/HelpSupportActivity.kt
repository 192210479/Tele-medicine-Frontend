package com.simats.tmapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class HelpSupportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_support)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener { finish() }

        val cardLiveChat = findViewById<MaterialCardView>(R.id.cardLiveChat)
        cardLiveChat.setOnClickListener {
            Toast.makeText(this, "Opening Live Chat...", Toast.LENGTH_SHORT).show()
        }

        val cardEmailUs = findViewById<MaterialCardView>(R.id.cardEmailUs)
        cardEmailUs.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@tmapp.com")
                putExtra(Intent.EXTRA_SUBJECT, "Support Request")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }

        val btnCallNow = findViewById<MaterialButton>(R.id.btnCallNow)
        btnCallNow.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:+1234567890")
            }
            startActivity(intent)
        }

        // Bottom Nav
        val userRole = intent.getStringExtra("userRole") ?: "doctor"

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val dashboardIntent = when (userRole) {
                "admin" -> Intent(this, AdminDashboardActivity::class.java)
                "doctor" -> Intent(this, DoctorDashboardActivity::class.java)
                else -> Intent(this, PatientDashboardActivity::class.java)
            }
            dashboardIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(dashboardIntent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navAppts).setOnClickListener {
            if (userRole == "admin") {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            } else if (userRole == "doctor") {
                startActivity(Intent(this, DoctorAppointmentsActivity::class.java))
            } else {
                startActivity(Intent(this, SelectDoctorActivity::class.java))
            }
            finish()
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            val profileIntent = when (userRole) {
                "admin" -> Intent(this, AdminProfileActivity::class.java)
                "doctor" -> Intent(this, DoctorProfileSettingsActivity::class.java)
                else -> Intent(this, ProfileActivity::class.java)
            }
            profileIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(profileIntent)
            finish()
        }
    }
}
