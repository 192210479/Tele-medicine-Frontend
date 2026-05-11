package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import androidx.activity.viewModels

class DeleteAccountActivity : AppCompatActivity() {
    private val viewModel: ProfileViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delete_account)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { 
            finish() 
        }

        findViewById<MaterialButton>(R.id.btnDeleteAccount).setOnClickListener {
            // Find the EditText in the hierarchy (MaterialCardView -> LinearLayout -> EditText)
            // It's the only EditText in the activity_delete_account scrollview content
            val scrollView = findViewById<android.view.View>(R.id.vHeaderDivider).parent as android.view.ViewGroup
            val content = (scrollView.getChildAt(2) as? android.view.ViewGroup)
            var etConfirm: android.widget.EditText? = null
            
            // Breadth-first search for the only EditText
            fun findEditText(view: android.view.View): android.widget.EditText? {
                if (view is android.widget.EditText) return view
                if (view is android.view.ViewGroup) {
                    for (i in 0 until view.childCount) {
                        val found = findEditText(view.getChildAt(i))
                        if (found != null) return found
                    }
                }
                return null
            }
            
            etConfirm = findEditText(window.decorView)

            val input = etConfirm?.text?.toString()?.trim()
            if (input == "DELETE") {
                val sessionManager = SessionManager.getInstance(this@DeleteAccountActivity)
                val userId = sessionManager.getUserId()
                
                viewModel.deleteAccount(userId, sessionManager.getUserRole() ?: "patient")
            } else {
                Toast.makeText(this@DeleteAccountActivity, "Please type DELETE to confirm", Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.actionStatus.observe(this) { (success, error) ->
            if (success) {
                Toast.makeText(this@DeleteAccountActivity, "Account Deleted", Toast.LENGTH_SHORT).show()
                val sessionManager = SessionManager.getInstance(this@DeleteAccountActivity)
                sessionManager.logout()
                val intent = Intent(this@DeleteAccountActivity, AuthWelcomeActivity::class.java)
                startActivity(intent)
                finishAffinity()
            } else {
                Toast.makeText(this@DeleteAccountActivity, error ?: "Failed to delete account", Toast.LENGTH_SHORT).show()
            }
        }

        val userRole = intent.getStringExtra("userRole") ?: "patient"

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
