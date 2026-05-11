package com.simats.Tmapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.GenericResponse
import com.simats.Tmapp.api.ReplySupportTicketRequest
import com.simats.Tmapp.api.SupportTicketDetailsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminTicketDetailActivity : AppCompatActivity() {

    private var ticketId: Int = -1
    private var currentUserId: Int = -1
    private var currentRole: String = "patient"

    private lateinit var tvToolbarTitle: TextView
    private lateinit var tvTicketStatus: TextView
    private lateinit var tvTicketTitle: TextView
    private lateinit var tvTicketDescription: TextView
    private lateinit var tvTicketMeta: TextView
    private lateinit var tvStatusChip: TextView
    private lateinit var rvMessages: RecyclerView
    private lateinit var tvNoMessages: TextView
    private lateinit var layoutReply: LinearLayout
    private lateinit var layoutStatusActions: LinearLayout
    private lateinit var etReply: TextInputEditText
    private lateinit var btnSend: MaterialButton
    private lateinit var btnMarkResolved: MaterialButton
    private lateinit var btnEscalate: MaterialButton

    private lateinit var chatAdapter: TicketChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_detail)

        ticketId      = intent.getIntExtra("ticket_id", -1)
        currentUserId = intent.getIntExtra("user_id", -1)
        currentRole   = intent.getStringExtra("role") ?: "patient"

        // Fallback to session if not passed
        if (currentUserId <= 0 || currentRole == "patient") {
            val session = SessionManager.getInstance(this)
            if (currentUserId <= 0) currentUserId = session.getUserId()
            if (currentRole == "patient") currentRole = session.getUserRole() ?: "patient"
        }

        bindViews()
        setupToolbar()
        setupRecyclerView()
        setupAdminControls()

        if (ticketId > 0) loadTicketDetails()
    }

    private fun bindViews() {
        tvToolbarTitle      = findViewById(R.id.tvToolbarTitle)
        tvTicketStatus      = findViewById(R.id.tvTicketStatus)
        tvTicketTitle       = findViewById(R.id.tvTicketTitle)
        tvTicketDescription = findViewById(R.id.tvTicketDescription)
        tvTicketMeta        = findViewById(R.id.tvTicketMeta)
        tvStatusChip        = findViewById(R.id.tvStatusChip)
        rvMessages          = findViewById(R.id.rvMessages)
        tvNoMessages        = findViewById(R.id.tvNoMessages)
        layoutReply         = findViewById(R.id.layoutReply)
        layoutStatusActions = findViewById(R.id.layoutStatusActions)
        etReply             = findViewById(R.id.etReply)
        btnSend             = findViewById(R.id.btnSend)
        btnMarkResolved     = findViewById(R.id.btnMarkResolved)
        btnEscalate         = findViewById(R.id.btnEscalate)
    }

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        chatAdapter = TicketChatAdapter(currentUserId)
        val lm = LinearLayoutManager(this)
        lm.stackFromEnd = true
        rvMessages.layoutManager = lm
        rvMessages.adapter = chatAdapter
    }

    private fun setupAdminControls() {
        if (currentRole == "admin") {
            layoutReply.visibility         = View.VISIBLE
            layoutStatusActions.visibility = View.VISIBLE

            btnSend.setOnClickListener {
                val msg = etReply.text?.toString()?.trim() ?: ""
                if (msg.isEmpty()) return@setOnClickListener
                sendReply(msg)
            }

            btnMarkResolved.setOnClickListener { updateStatus("closed") }
            btnEscalate.setOnClickListener     { updateStatus("escalated") }
        } else {
            layoutReply.visibility         = View.GONE
            layoutStatusActions.visibility = View.GONE
        }
    }

    private fun loadTicketDetails() {
        ApiClient.instance.getSupportTicketDetails(ticketId)
            .enqueue(object : Callback<SupportTicketDetailsResponse> {
                override fun onResponse(
                    call: Call<SupportTicketDetailsResponse>,
                    response: Response<SupportTicketDetailsResponse>
                ) {
                    if (!response.isSuccessful || response.body() == null) {
                        showError("Failed to load ticket details")
                        return
                    }

                    val body   = response.body()!!
                    val ticket = body.ticket
                    val msgs   = body.messages

                    // Populate header
                    tvToolbarTitle.text = "Ticket #${ticket.id}"
                    tvTicketStatus.text = ticket.status.replaceFirstChar { it.uppercaseChar() }
                    tvTicketTitle.text  = ticket.title
                    tvTicketDescription.text = ticket.description

                    val date = ticket.created_at?.take(10) ?: ""
                    tvTicketMeta.text = if (date.isNotEmpty()) "Opened: $date" else ""

                    applyStatusColor(ticket.status)

                    // Messages
                    if (msgs.isEmpty()) {
                        rvMessages.visibility    = View.GONE
                        tvNoMessages.visibility  = View.VISIBLE
                    } else {
                        rvMessages.visibility    = View.VISIBLE
                        tvNoMessages.visibility  = View.GONE
                        chatAdapter.submitList(msgs)
                        rvMessages.scrollToPosition(msgs.size - 1)
                    }
                }

                override fun onFailure(call: Call<SupportTicketDetailsResponse>, t: Throwable) {
                    showError("Error: ${t.message}")
                }
            })
    }

    private fun sendReply(message: String) {
        val session = SessionManager.getInstance(this)
        val request = ReplySupportTicketRequest(
            sender_id   = currentUserId,
            sender_role = currentRole,
            message     = message
        )

        btnSend.isEnabled = false

        ApiClient.instance.replySupportTicket(ticketId, request)
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(
                    call: Call<GenericResponse>,
                    response: Response<GenericResponse>
                ) {
                    btnSend.isEnabled = true
                    if (response.isSuccessful) {
                        etReply.text?.clear()
                        // Reload conversation to show new message
                        loadTicketDetails()
                    } else {
                        Toast.makeText(this@AdminTicketDetailActivity, "Failed to send reply", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    btnSend.isEnabled = true
                    Toast.makeText(this@AdminTicketDetailActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateStatus(newStatus: String) {
        val body = mapOf<String, Any>("status" to newStatus)
        ApiClient.instance.updateSupportTicketStatus(ticketId, body)
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(
                    call: Call<GenericResponse>,
                    response: Response<GenericResponse>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@AdminTicketDetailActivity,
                            "Ticket marked as $newStatus",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadTicketDetails()
                    } else {
                        Toast.makeText(this@AdminTicketDetailActivity, "Update failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@AdminTicketDetailActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun applyStatusColor(status: String) {
        val color = when (status.lowercase()) {
            "open"                  -> Color.parseColor("#2979FF")
            "closed", "resolved"    -> Color.parseColor("#4CAF50")
            "escalated"             -> Color.parseColor("#F44336")
            "pending"               -> Color.parseColor("#FF9800")
            else                    -> Color.parseColor("#9E9E9E")
        }
        tvStatusChip.text = status.replaceFirstChar { it.uppercaseChar() }
        tvStatusChip.background?.mutate()?.setTint(color)
        tvTicketStatus.setTextColor(color)
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
