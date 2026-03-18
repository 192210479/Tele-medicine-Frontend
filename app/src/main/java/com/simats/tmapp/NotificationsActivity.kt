package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.simats.tmapp.api.*

class NotificationsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: NotificationViewModel
    private lateinit var llNotificationsContainer: LinearLayout
    private var allNotifications: List<NotificationResponse> = emptyList()
    private var currentFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        sessionManager = SessionManager.getInstance(this)
        viewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
        
        llNotificationsContainer = findViewById(R.id.llNotificationsContainer)

        // Header Back Arrow
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Mark All Read
        findViewById<TextView>(R.id.tvMarkAllRead).setOnClickListener {
            viewModel.markAllAsRead()
        }

        // Tabs
        setupTabs()

        // Observe ViewModel
        viewModel.notifications.observe(this) { list ->
            allNotifications = list ?: emptyList()
            filterAndDisplay()
        }

        viewModel.unreadCount.observe(this) { count ->
            // Update UI components that depend on unread count if any on this screen
        }

        viewModel.error.observe(this) { msg ->
            if (msg.isNotEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loadNotifications()
    }

    private fun setupTabs() {
        val tabAll = findViewById<TextView>(R.id.tabAll)
        val tabAppointments = findViewById<TextView>(R.id.tabAppointments)
        val tabPrescriptions = findViewById<TextView>(R.id.tabPrescriptions)
        val tabMessages = findViewById<TextView>(R.id.tabMessages)
        val tabPayments = findViewById<TextView>(R.id.tabPayments)

        val tabs = listOf(tabAll, tabAppointments, tabPrescriptions, tabMessages, tabPayments)

        fun updateTabsUI(selectedTab: TextView) {
            for (tab in tabs) {
                tab.setBackgroundResource(R.drawable.bg_tab_unselected)
                tab.setTextColor(ContextCompat.getColor(this, R.color.text_grey))
            }
            selectedTab.setBackgroundResource(R.drawable.bg_tab_selected)
            selectedTab.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        tabAll.setOnClickListener { 
            updateTabsUI(tabAll)
            currentFilter = null
            filterAndDisplay()
        }
        tabAppointments.setOnClickListener { 
            updateTabsUI(tabAppointments)
            currentFilter = "Appointment"
            filterAndDisplay()
        }
        tabPrescriptions.setOnClickListener { 
            updateTabsUI(tabPrescriptions)
            currentFilter = "Prescription"
            filterAndDisplay()
        }
        tabMessages.setOnClickListener { 
            updateTabsUI(tabMessages)
            currentFilter = "Message"
            filterAndDisplay()
        }
        tabPayments.setOnClickListener { 
            updateTabsUI(tabPayments)
            currentFilter = "Payment"
            filterAndDisplay()
        }
    }

    private fun filterAndDisplay() {
        val filteredList = if (currentFilter == null) {
            allNotifications
        } else {
            allNotifications.filter { it.type.contains(currentFilter!!, ignoreCase = true) }
        }
        displayNotifications(filteredList)
    }

    private fun displayNotifications(notifications: List<NotificationResponse>) {
        llNotificationsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        if (notifications.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = "No notifications found"
            emptyText.gravity = android.view.Gravity.CENTER
            emptyText.setPadding(0, 100, 0, 0)
            llNotificationsContainer.addView(emptyText)
            return
        }

        notifications.forEach { notif ->
            val view = inflater.inflate(R.layout.item_notification, llNotificationsContainer, false)
            
            view.findViewById<TextView>(R.id.tvNotificationTitle).text = notif.title
            view.findViewById<TextView>(R.id.tvNotificationDesc).text = notif.description
            view.findViewById<TextView>(R.id.tvNotificationTime).text = formatTime(notif.createdAt)
            
            val dot = view.findViewById<View>(R.id.vUnreadDot)
            dot.visibility = if (notif.isRead) View.GONE else View.VISIBLE

            view.setOnClickListener {
                if (!notif.isRead) {
                    viewModel.markAsRead(notif.id)
                }

                if (notif.type.equals("RecordRequest", ignoreCase = true)) {
                    val intent = Intent(this, MedicalRecordsActivity::class.java)
                    intent.putExtra("appointment_id", notif.referenceId ?: -1)
                    intent.putExtra("patient_id", sessionManager.getUserId())
                    startActivity(intent)
                }
            }

            // Icon Mapping
            val iconView = view.findViewById<ImageView>(R.id.ivNotificationIcon)
            val container = view.findViewById<View>(R.id.flIconContainer)
            
            when (notif.type.lowercase()) {
                "appointment" -> {
                    iconView.setImageResource(R.drawable.ic_calendar_blue)
                    container.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_blue_light)
                }
                "prescription" -> {
                    iconView.setImageResource(R.drawable.ic_document)
                    iconView.setColorFilter(ContextCompat.getColor(this, R.color.green_success))
                    container.backgroundTintList = ContextCompat.getColorStateList(this, R.color.icon_bg_teal)
                }
                "payment" -> {
                    iconView.setImageResource(R.drawable.ic_credit_card)
                    iconView.setColorFilter(ContextCompat.getColor(this, R.color.orange_warning))
                    container.backgroundTintList = ContextCompat.getColorStateList(this, R.color.bg_badge_orange_light)
                }
                else -> {
                    iconView.setImageResource(R.drawable.ic_bell_thin)
                }
            }

            llNotificationsContainer.addView(view)
        }
    }

    private fun formatTime(timestamp: String): String {
        return try {
            val parts = timestamp.split(" ")
            if (parts.size >= 2) parts[1].take(5) else "Just now"
        } catch (e: Exception) {
            "Just now"
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadNotifications()
    }
}
