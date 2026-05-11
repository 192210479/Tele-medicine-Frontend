package com.simats.Tmapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.simats.Tmapp.api.*
import java.util.*

class DoctorApprovalActivity : AppCompatActivity() {

    private val viewModel: DoctorApprovalViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    
    private lateinit var tabPending: TextView
    private lateinit var tabApproved: TextView
    private lateinit var tabRejected: TextView
    private lateinit var llApprovalsContent: LinearLayout
    private lateinit var tvPendingCount: TextView
    private lateinit var tvApprovedCount: TextView
    private lateinit var tvRejectedCount: TextView
    
    private var currentTab = "Pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_approvals)
        sessionManager = SessionManager.getInstance(this)

        initializeViews()
        setupObservers()
        setupSocketListeners()
        
        viewModel.loadDoctors()
    }

    private fun initializeViews() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        tabPending = findViewById(R.id.tabPending)
        tabApproved = findViewById(R.id.tabApproved)
        tabRejected = findViewById(R.id.tabRejected)
        llApprovalsContent = findViewById(R.id.llApprovalsContent)

        tvPendingCount = findViewById(R.id.tvPendingCount)
        tvApprovedCount = findViewById(R.id.tvApprovedCount)
        tvRejectedCount = findViewById(R.id.tvRejectedCount)

        tabPending.setOnClickListener { updateTabs("Pending") }
        tabApproved.setOnClickListener { updateTabs("Approved") }
        tabRejected.setOnClickListener { updateTabs("Rejected") }

        findViewById<LinearLayout>(R.id.navDashboard).setOnClickListener {
            val intent = Intent(this, AdminDashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.pendingDoctors.observe(this) { list ->
            tvPendingCount.text = list.size.toString()
            if (currentTab == "Pending") renderList(list)
        }

        viewModel.approvedDoctors.observe(this) { list ->
            tvApprovedCount.text = list.size.toString()
            if (currentTab == "Approved") renderList(list)
        }

        viewModel.rejectedDoctors.observe(this) { list ->
            tvRejectedCount.text = list.size.toString()
            if (currentTab == "Rejected") renderList(list)
        }
    }

    private fun updateTabs(selectedTab: String) {
        currentTab = selectedTab
        
        val fontMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        val fontBold = Typeface.create("sans-serif", Typeface.BOLD)

        // Reset all tabs UI
        listOf(tabPending, tabApproved, tabRejected).forEach {
            it.setBackgroundColor(Color.TRANSPARENT)
            it.setTextColor(Color.parseColor("#64748B"))
            it.typeface = fontMedium
            it.elevation = 0f
        }

        val selected = when (selectedTab) {
            "Pending" -> tabPending
            "Approved" -> tabApproved
            "Rejected" -> tabRejected
            else -> tabPending
        }
        selected.setBackgroundResource(R.drawable.bg_tab_selected_light)
        selected.setTextColor(ContextCompat.getColor(this, R.color.primary_blue))
        selected.typeface = fontBold
        selected.elevation = 4f

        refreshList()
    }

    private fun refreshList() {
        when (currentTab) {
            "Pending" -> renderList(viewModel.pendingDoctors.value ?: emptyList())
            "Approved" -> renderList(viewModel.approvedDoctors.value ?: emptyList())
            "Rejected" -> renderList(viewModel.rejectedDoctors.value ?: emptyList())
        }
    }

    private fun renderList(doctors: List<Doctor>) {
        llApprovalsContent.removeAllViews()
        if (doctors.isEmpty()) {
            val emptyView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    600
                )
            }
            
            val tv = TextView(this)
            tv.text = "No doctors in this list"
            tv.gravity = Gravity.CENTER
            tv.setTextColor(Color.parseColor("#94A3B8"))
            tv.textSize = 16f
            tv.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            
            emptyView.addView(tv)
            llApprovalsContent.addView(emptyView)
            return
        }

        doctors.forEach { doc ->
            val view = layoutInflater.inflate(R.layout.item_doctor_approval, llApprovalsContent, false)
            populateCardView(view, doc)

            val llActions = view.findViewById<LinearLayout>(R.id.llActions)
            val status = (doc.status ?: "pending").lowercase()
            llActions.visibility = if (status == "pending") View.VISIBLE else View.GONE

            view.findViewById<View>(R.id.btnApprove).setOnClickListener {
                viewModel.approveDoctor(doc)
                Toast.makeText(this, "Approving Dr. ${doc.name}", Toast.LENGTH_SHORT).show()
            }
            view.findViewById<View>(R.id.btnReject).setOnClickListener {
                viewModel.rejectDoctor(doc)
                Toast.makeText(this, "Rejecting Dr. ${doc.name}", Toast.LENGTH_SHORT).show()
            }
            
            view.findViewById<View>(R.id.btnViewLicense).setOnClickListener {
                openDocument(doc.id, "license")
            }
            view.findViewById<View>(R.id.btnViewCertificate).setOnClickListener {
                openDocument(doc.id, "medical")
            }

            llApprovalsContent.addView(view)
        }
    }

    private fun populateCardView(view: View, doc: Doctor) {
        val tvDoctorName = view.findViewById<TextView>(R.id.tvDoctorName)
        tvDoctorName.text = if (!doc.name.isNullOrEmpty()) "Dr. ${doc.name}" else "Unknown Doctor"
        
        val initials = (doc.name ?: "UD").split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("").take(2)

        val tvInitials = view.findViewById<TextView>(R.id.tvInitials)
        tvInitials.text = initials
        view.findViewById<TextView>(R.id.tvDoctorSpecialty).text = doc.specialization ?: "General Surgeon"

        val ivDoctorAvatar = view.findViewById<ImageView?>(R.id.ivDoctorAvatar)
        if (ivDoctorAvatar != null) {
            val baseUrl = ApiClient.BASE_URL.removeSuffix("/")

            val finalDoctorImage = when {
                !doc.profileImage.isNullOrEmpty() && doc.profileImage!!.startsWith("http") -> doc.profileImage
                !doc.profileImage.isNullOrEmpty() && doc.profileImage!!.startsWith("/") -> "$baseUrl${doc.profileImage}"
                doc.id != null -> "$baseUrl/api/profile/image/${doc.id}?role=doctor"
                else -> null
            }

            AvatarUtils.loadAvatar(
                imageView = ivDoctorAvatar,
                imageUrl = finalDoctorImage,
                name = doc.name ?: "Doctor"
            )

            tvInitials.visibility = View.GONE
        } else {
            tvInitials.visibility = View.VISIBLE
        }
        
        // Use real license number from backend
        val licNum = doc.licenseNumber ?: "N/A"
        view.findViewById<TextView>(R.id.tvDoctorLic).text = "Lic: $licNum   •   Recent"

        val tvStatusBadge = view.findViewById<TextView>(R.id.tvStatusBadge)
        val statusText = doc.status ?: "Pending"
        tvStatusBadge.text = statusText.replaceFirstChar { it.uppercase() }
        
        when (statusText.lowercase()) {
            "approved" -> {
                tvStatusBadge.setTextColor(Color.parseColor("#10B981"))
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_green)
            }
            "rejected" -> {
                tvStatusBadge.setTextColor(Color.parseColor("#EF4444"))
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_red_soft)
            }
            else -> {
                tvStatusBadge.setTextColor(Color.parseColor("#EA580C"))
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_orange)
            }
        }
    }

    private fun setupSocketListeners() {
        val refresh = { runOnUiThread { viewModel.loadDoctors() } }
        SocketService.socket?.on("doctor_status_updated") { refresh() }
        SocketService.socket?.on("appointment_updated") { refresh() }
        SocketService.socket?.on("patient_updated") { refresh() }
    }

    private fun openDocument(doctorId: Int, type: String) {
        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        val url = "$baseUrl/api/doctor/document/$doctorId/$type"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketService.socket?.off("doctor_status_updated")
    }

    // Extension property for Int to SP
    private val Int.sp: Float get() = this * resources.displayMetrics.scaledDensity
}
