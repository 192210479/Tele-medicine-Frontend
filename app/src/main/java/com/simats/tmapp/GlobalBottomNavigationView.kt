package com.simats.tmapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class GlobalBottomNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val sessionManager = SessionManager.getInstance(context)
    private val userRole = sessionManager.getUserRole()

    init {
        orientation = HORIZONTAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.surface_bg))
        elevation = 12f * resources.displayMetrics.density

        val role = userRole?.lowercase() ?: ""
        val layoutRes = when {
            role.contains("doctor") -> R.layout.layout_doctor_bottom_nav_content
            role.contains("admin") -> R.layout.layout_admin_bottom_nav_content
            else -> R.layout.layout_patient_bottom_nav_content
        }

        LayoutInflater.from(context).inflate(layoutRes, this, true)

        // Handle bottom safe area for gesture navigation
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }

        setupClickListeners()
        setupActiveTab()
    }

    private fun setupClickListeners() {
        val role = userRole?.lowercase() ?: ""
        when {
            role.contains("doctor") -> {
                findViewById<View>(R.id.navHome)?.setOnClickListener { navigateTo(DoctorDashboardActivity::class.java) }
                findViewById<View>(R.id.navAppts)?.setOnClickListener { navigateTo(DoctorAppointmentsActivity::class.java) }
                findViewById<View>(R.id.navProfile)?.setOnClickListener { navigateTo(DoctorProfileSettingsActivity::class.java) }
            }
            role.contains("admin") -> {
                findViewById<View>(R.id.navDashboard)?.setOnClickListener { navigateTo(AdminDashboardActivity::class.java) }
                findViewById<View>(R.id.navProfile)?.setOnClickListener { navigateTo(AdminProfileActivity::class.java) }
                findViewById<View>(R.id.navHome)?.setOnClickListener { navigateTo(AdminDashboardActivity::class.java) }
            }
            else -> { // Patient
                findViewById<View>(R.id.navHome)?.setOnClickListener { navigateTo(PatientDashboardActivity::class.java) }
                findViewById<View>(R.id.navBook)?.setOnClickListener { navigateTo(SelectDoctorActivity::class.java) }
                findViewById<View>(R.id.navHistory)?.setOnClickListener { navigateTo(AppointmentsActivity::class.java) }
                findViewById<View>(R.id.navProfile)?.setOnClickListener { navigateTo(ProfileActivity::class.java) }
            }
        }
    }

    private fun navigateTo(targetActivity: Class<*>) {
        val currentActivity = context as? Activity
        if (currentActivity != null && currentActivity.javaClass != targetActivity) {
            val intent = Intent(currentActivity, targetActivity)
            
            if (targetActivity == PatientDashboardActivity::class.java || 
                targetActivity == DoctorDashboardActivity::class.java || 
                targetActivity == AdminDashboardActivity::class.java) {
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            } else {
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            
            currentActivity.startActivity(intent)
        }
    }

    private fun setupActiveTab() {
        val currentActivity = (context as? Activity)?.javaClass?.simpleName ?: return
        val role = userRole?.lowercase() ?: ""

        val activeTab = when {
            role.contains("doctor") -> when (currentActivity) {
                "DoctorDashboardActivity" -> "Home"
                "DoctorAppointmentsActivity", "AppointmentsActivity" -> "Appts"
                else -> if (currentActivity.contains("Profile") || currentActivity.contains("Settings")) "Profile" else "Home"
            }
            role.contains("admin") -> when (currentActivity) {
                "AdminDashboardActivity" -> "Dashboard"
                else -> if (currentActivity.contains("Profile")) "Profile" else "Dashboard"
            }
            else -> when (currentActivity) { // Patient
                "PatientDashboardActivity" -> "Home"
                "SelectDoctorActivity", "SelectTimeActivity", "BookingConfirmedActivity", "DoctorProfileActivity" -> "Book"
                "AppointmentsActivity", "ConsultationHistoryActivity", "PrescriptionActivity", "MedicationRemindersActivity", "MedicalRecordsActivity" -> "History"
                else -> if (currentActivity.contains("Profile") || currentActivity.contains("Settings")) "Profile" else "Home"
            }
        }

        val primaryBlue = ContextCompat.getColor(context, R.color.primary_blue)
        val textGrey = ContextCompat.getColor(context, R.color.text_grey)

        val indicatorMap = mapOf(
            "Home" to Triple(R.id.indicatorHome, R.id.iconHome, R.id.textHome),
            "Book" to Triple(R.id.indicatorBook, R.id.iconBook, R.id.textBook),
            "History" to Triple(R.id.indicatorHistory, R.id.iconHistory, R.id.textHistory),
            "Profile" to Triple(R.id.indicatorProfile, R.id.iconProfile, R.id.textProfile),
            "Appts" to Triple(R.id.indicatorAppts, R.id.iconAppts, R.id.textAppts),
            "Dashboard" to Triple(R.id.indicatorDashboard, R.id.iconDashboard, R.id.textDashboard)
        )

        indicatorMap.forEach { (tab, ids) ->
            val indicator = findViewById<View>(ids.first)
            val icon = findViewById<ImageView>(ids.second)
            val text = findViewById<TextView>(ids.third)

            if (tab == activeTab) {
                indicator?.setBackgroundColor(primaryBlue)
                icon?.setColorFilter(primaryBlue)
                text?.setTextColor(primaryBlue)
                text?.setTypeface(null, Typeface.BOLD)
                indicator?.visibility = View.VISIBLE
            } else {
                indicator?.setBackgroundColor(Color.TRANSPARENT)
                icon?.setColorFilter(textGrey)
                text?.setTextColor(textGrey)
                text?.setTypeface(null, Typeface.NORMAL)
                indicator?.visibility = if (indicator != null) View.INVISIBLE else View.GONE
            }
        }
    }
}
