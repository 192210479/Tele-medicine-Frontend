package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.SupportTicketResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyTicketsActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var adapter: SupportTicketAdapter

    private var userId: Int = -1
    private var role: String = "patient"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_tickets)

        userId = intent.getIntExtra("user_id", -1)
        role   = intent.getStringExtra("userRole") ?: "patient"

        // Fallback to session
        if (userId <= 0) {
            val session = SessionManager(this)
            userId = session.getUserId()
            if (role == "patient") role = session.getUserRole() ?: "patient"
        }

        rv          = findViewById(R.id.rvTickets)
        layoutEmpty = findViewById(R.id.layoutEmpty)

        adapter = SupportTicketAdapter { ticket ->
            val intent = Intent(this, AdminTicketDetailActivity::class.java)
            intent.putExtra("ticket_id", ticket.id)
            intent.putExtra("user_id",   userId)
            intent.putExtra("role",      role)
            startActivity(intent)
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        loadTickets()
    }

    override fun onResume() {
        super.onResume()
        loadTickets()
    }

    private fun loadTickets() {
        val call: Call<List<SupportTicketResponse>> = if (role == "admin") {
            ApiClient.instance.getAdminSupportTickets()
        } else {
            ApiClient.instance.getMySupportTickets(userId, role)
        }

        call.enqueue(object : Callback<List<SupportTicketResponse>> {
            override fun onResponse(
                call: Call<List<SupportTicketResponse>>,
                response: Response<List<SupportTicketResponse>>
            ) {
                if (response.isSuccessful) {
                    val data = response.body()
                    adapter.submitList(data)
                    showEmptyIfNeeded(data.isNullOrEmpty())
                } else {
                    showEmptyIfNeeded(true)
                    Toast.makeText(this@MyTicketsActivity, "Failed to load tickets", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<SupportTicketResponse>>, t: Throwable) {
                showEmptyIfNeeded(true)
                Toast.makeText(this@MyTicketsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showEmptyIfNeeded(empty: Boolean) {
        rv.visibility          = if (empty) View.GONE else View.VISIBLE
        layoutEmpty.visibility = if (empty) View.VISIBLE else View.GONE
    }
}