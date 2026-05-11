package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.TextView
import com.simats.Tmapp.api.LoginHistoryResponse

class LoginHistoryActivity : AppCompatActivity() {
    private val viewModel: ProfileViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_history)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val sessionManager = SessionManager.getInstance(this)
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole() ?: "patient"
        if (userId == -1) return

        val btnLogOutAll = findViewById<MaterialButton>(R.id.btnLogOutAll)
        btnLogOutAll.setOnClickListener {
            viewModel.logoutAllDevices(userId, role)
        }

        viewModel.actionStatus.observe(this) { (success, error) ->
            if (success) {
                Toast.makeText(this@LoginHistoryActivity, "Logged out of all devices", Toast.LENGTH_SHORT).show()
                sessionManager.logout()
                val intent = Intent(this@LoginHistoryActivity, LoginActivity::class.java) // Redirect to login
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finishAffinity() // "finishAffinity()" as requested
            } else {
                Toast.makeText(this@LoginHistoryActivity, error ?: "Logout failed", Toast.LENGTH_SHORT).show()
            }
        }

        val rvLoginHistory = findViewById<RecyclerView>(R.id.rvLoginHistory)
        val adapter = LoginHistoryAdapter()
        rvLoginHistory.adapter = adapter
        rvLoginHistory.layoutManager = LinearLayoutManager(this)

        viewModel.loginHistory.observe(this) { history ->
            adapter.submitList(history ?: emptyList())
        }

        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        val sessionManager = SessionManager.getInstance(this)
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole() ?: "patient"
        if (userId != -1) {
            viewModel.fetchLoginHistory(userId, role)
        }
    }

    private fun setupNavigation() {
        val userRole = (intent.getStringExtra("userRole") ?: SessionManager.getInstance(this).getUserRole()).lowercase()

        findViewById<LinearLayout>(R.id.navHome)?.setOnClickListener {
            val dashboardIntent = when {
                userRole.contains("admin") -> Intent(this, AdminDashboardActivity::class.java)
                userRole.contains("doctor") -> Intent(this, DoctorDashboardActivity::class.java)
                else -> Intent(this, PatientDashboardActivity::class.java)
            }
            dashboardIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(dashboardIntent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navBook)?.setOnClickListener {
            when {
                userRole.contains("admin") -> startActivity(Intent(this, AdminDashboardActivity::class.java))
                userRole.contains("doctor") -> startActivity(Intent(this, DoctorAppointmentsActivity::class.java))
                else -> startActivity(Intent(this, SelectDoctorActivity::class.java))
            }
        }

        findViewById<LinearLayout>(R.id.navHistory)?.setOnClickListener {
            when {
                userRole.contains("admin") -> startActivity(Intent(this, AdminDashboardActivity::class.java))
                !userRole.contains("doctor") -> startActivity(Intent(this, ConsultationHistoryActivity::class.java))
            }
        }

        findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            val profileIntent = when {
                userRole.contains("admin") -> Intent(this, AdminProfileActivity::class.java)
                userRole.contains("doctor") -> Intent(this, DoctorProfileSettingsActivity::class.java)
                else -> Intent(this, ProfileActivity::class.java)
            }
            profileIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(profileIntent)
            finish()
        }
    }

    private class LoginHistoryAdapter : RecyclerView.Adapter<LoginHistoryAdapter.ViewHolder>() {
        private var history = listOf<LoginHistoryResponse>()

        fun submitList(newHistory: List<LoginHistoryResponse>) {
            history = newHistory
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_login_activity, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = history[position]
            holder.tvTitle.text = item.device
            holder.tvLocation.text = item.location
            
            // Dynamic icon based on device name
            if (item.device.lowercase().contains("mac") || item.device.lowercase().contains("windows") || item.device.lowercase().contains("laptop")) {
                holder.ivIcon.setImageResource(R.drawable.ic_laptop)
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_device_mobile)
            }

            if (item.isCurrent) {
                holder.tvDate.text = "Active Now"
                holder.tvDate.setTextColor(android.graphics.Color.parseColor("#10B981")) // Green for active
                holder.tvCurrentBadge.visibility = View.VISIBLE
                
                // Extra styling for current badge to match design exactly
                holder.tvCurrentBadge.setBackgroundResource(R.drawable.bg_badge_green)
                holder.tvCurrentBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D1FAE5"))
            } else {
                holder.tvDate.text = item.date
                holder.tvDate.setTextColor(android.graphics.Color.parseColor("#64748B")) // Default gray
                holder.tvCurrentBadge.visibility = View.GONE
            }
        }

        override fun getItemCount() = history.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvDeviceTitle)
            val tvLocation: TextView = view.findViewById(R.id.tvLocation)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvCurrentBadge: TextView = view.findViewById(R.id.tvCurrentBadge)
            val ivIcon: ImageView = view.findViewById(R.id.ivDeviceIcon)
        }
    }
}
