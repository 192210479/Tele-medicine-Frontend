package com.simats.Tmapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private var appointmentId: Int = -1
    private var consultationId: Int = -1
    private val messages = mutableListOf<ConsultationChatMessageResponse>()
    private lateinit var adapter: ChatAdapterV2
    private val handler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            fetchMessages()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        sessionManager = SessionManager.getInstance(this)
        appointmentId = intent.getIntExtra("appointment_id", -1)
        consultationId = intent.getIntExtra("consultation_id", -1)

        if (consultationId == -1 && appointmentId == -1) {
            Toast.makeText(this, "Empty Chat Session", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnSend)

        adapter = ChatAdapterV2(messages, sessionManager.getUserId())
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = adapter

        // Keyboard Aware Scrolling: Scroll to bottom when keyboard opens
        rvMessages.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                if (messages.isNotEmpty()) {
                    rvMessages.post {
                        rvMessages.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }
        }

        etMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && messages.isNotEmpty()) {
                rvMessages.postDelayed({
                    rvMessages.smoothScrollToPosition(messages.size - 1)
                }, 200)
            }
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                etMessage.text.clear()
            }
        }

        handler.post(pollRunnable)
    }

    private fun fetchMessages() {
        if (consultationId == -1) return
        
        ApiClient.instance.getConsultationChat(consultationId).enqueue(object : Callback<List<ConsultationChatMessageResponse>> {
            override fun onResponse(call: Call<List<ConsultationChatMessageResponse>>, response: Response<List<ConsultationChatMessageResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    val newMessages = response.body()!!
                    if (newMessages.size != messages.size || newMessages.lastOrNull()?.message != messages.lastOrNull()?.message) {
                        val oldSize = messages.size
                        messages.clear()
                        messages.addAll(newMessages)
                        adapter.notifyDataSetChanged()
                        
                        // Scroll to bottom if new messages arrived
                        if (newMessages.size > oldSize) {
                            findViewById<RecyclerView>(R.id.rvMessages).smoothScrollToPosition(messages.size - 1)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<List<ConsultationChatMessageResponse>>, t: Throwable) {}
        })
    }

    private fun sendMessage(text: String) {
        if (consultationId == -1) return
        val userId = sessionManager.getUserId()
        val request = ConsultationChatSendRequest(
            consultationId = consultationId,
            senderId = userId,
            message = text
        )

        ApiClient.instance.sendConsultationChat(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                fetchMessages()
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
    }
}
