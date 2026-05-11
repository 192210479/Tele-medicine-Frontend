package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.TextView
import com.simats.Tmapp.api.DeviceResponse

class DeviceManagementActivity : AppCompatActivity() {
    private val viewModel: ProfileViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_management)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Setup RecyclerView programmatically
        val root = findViewById<View>(R.id.vHeaderDivider).parent as ViewGroup
        var scrollView: androidx.core.widget.NestedScrollView? = null
        var legacyScrollView: android.widget.ScrollView? = null
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is androidx.core.widget.NestedScrollView) scrollView = child
            if (child is android.widget.ScrollView) legacyScrollView = child
        }
        val linearLayout = (scrollView?.getChildAt(0) ?: legacyScrollView?.getChildAt(0)) as? LinearLayout
        
        val sessionManager = SessionManager.getInstance(this@DeviceManagementActivity)
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole() ?: "patient"
        
        if (linearLayout != null) {
            // Keep the security tip card (last child)
            val securityTip = linearLayout.getChildAt(linearLayout.childCount - 1)
            linearLayout.removeAllViews()
            
            val recyclerView = RecyclerView(this).apply {
                layoutManager = LinearLayoutManager(this@DeviceManagementActivity)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 24 }
            }
            linearLayout.addView(recyclerView)
            if (securityTip != null) {
                linearLayout.addView(securityTip)
            }

            val adapter = DeviceAdapter { deviceId ->
                viewModel.deleteDevice(deviceId, userId, role)
            }
            recyclerView.adapter = adapter

            viewModel.fetchDevices(userId, role)
            viewModel.devices.observe(this) { devices ->
                adapter.submitList(devices ?: emptyList())
            }
            
            viewModel.actionStatus.observe(this) { (success, error) ->
                if (success) {
                    Toast.makeText(this@DeviceManagementActivity, "Device action successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@DeviceManagementActivity, error ?: "Action failed", Toast.LENGTH_SHORT).show()
                }
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

    private class DeviceAdapter(private val onDelete: (Int) -> Unit) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
        private var devices = listOf<DeviceResponse>()

        fun submitList(newDevices: List<DeviceResponse>) {
            devices = newDevices
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = devices[position]
            holder.tvName.text = device.deviceName
            holder.tvIpAndLogin.text = "${device.ipAddress} • Last Login: ${device.lastLogin}"
            holder.ivDelete.setOnClickListener { onDelete(device.id) }
        }

        override fun getItemCount() = devices.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvDeviceName)
            val tvIpAndLogin: TextView = view.findViewById(R.id.tvIpAndLogin)
            val ivDelete: ImageView = view.findViewById(R.id.ivDeleteDevice)
        }
    }
}
