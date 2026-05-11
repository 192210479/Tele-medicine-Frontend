package com.simats.Tmapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class DataPrivacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_privacy)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { 
            finish() 
        }

        findViewById<MaterialButton>(R.id.btnIUnderstand).setOnClickListener { 
            finish() 
        }

        val userRole = intent.getStringExtra("userRole") ?: "patient"
        val tvAccountSpecific = findViewById<TextView>(R.id.tvAccountSpecific)

        if (userRole == "doctor") {
            tvAccountSpecific.text = "As a Doctor, you have specific access to manage your doctor profile, specialization, availability, and appointment schedule. You can access and manage consultation notes, issue prescriptions, view uploaded reports/documents, and communicate securely with your patients."
        } else if (userRole == "admin") {
            tvAccountSpecific.text = "As an Admin, you have platform-level access for user management, appointment oversight, verification workflows, system monitoring, complaint/support handling, and security auditing."
        } else {
            tvAccountSpecific.text = "As a Patient, you have access to your profile data, appointments, prescriptions, consultation messages, medical records, payments, reminders, and notifications."
        }
    }
}
