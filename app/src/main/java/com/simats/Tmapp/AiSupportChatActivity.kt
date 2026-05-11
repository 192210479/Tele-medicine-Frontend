package com.simats.Tmapp

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.simats.Tmapp.api.AiChatMessage
import com.simats.Tmapp.api.AiSupportRequest
import com.simats.Tmapp.api.AiSupportResponse
import com.simats.Tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AiSupportChatActivity : AppCompatActivity() {

    private lateinit var recyclerChat: RecyclerView
    private lateinit var etMessage: TextInputEditText
    private lateinit var btnSend: MaterialButton
    private lateinit var chatAdapter: AiSupportChatAdapter

    private val messages = mutableListOf<AiChatMessage>()

    private var userId: Int = -1
    private var userRole: String = "patient"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_support_chat)

        val sessionManager = SessionManager(this)

        userId = intent.getIntExtra("user_id", -1)
        if (userId <= 0) {
            userId = sessionManager.getUserId()
        }

        userRole = intent.getStringExtra("userRole")
            ?: sessionManager.getUserRole()
            ?: "patient"

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        recyclerChat = findViewById(R.id.recyclerChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        ivBack.setOnClickListener { finish() }

        chatAdapter = AiSupportChatAdapter(messages)
        recyclerChat.layoutManager = LinearLayoutManager(this)
        recyclerChat.adapter = chatAdapter

        addBotMessage("Hi! I'm TMApp AI Support. Tell me your issue and I’ll try to help.")

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            addUserMessage(text)
            etMessage.setText("")
            sendMessageToAi(text)
        }
    }

    private fun sendMessageToAi(message: String) {
        btnSend.isEnabled = false

        ApiClient.instance.sendAiSupportMessage(
            AiSupportRequest(
                user_id = userId,
                role = userRole,
                message = message
            )
        ).enqueue(object : Callback<AiSupportResponse> {
            override fun onResponse(
                call: Call<AiSupportResponse>,
                response: Response<AiSupportResponse>
            ) {
                btnSend.isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    val aiReply = response.body()!!.reply
                    addBotMessage(aiReply)
                } else {
                    addBotMessage("Sorry, I couldn't process that right now. Please try again.")
                }
            }

            override fun onFailure(call: Call<AiSupportResponse>, t: Throwable) {
                btnSend.isEnabled = true
                addBotMessage("Server error: ${t.message}")
            }
        })
    }

    private fun addUserMessage(text: String) {
        messages.add(AiChatMessage(text, true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerChat.scrollToPosition(messages.size - 1)
    }

    private fun addBotMessage(text: String) {
        messages.add(AiChatMessage(text, false))
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerChat.scrollToPosition(messages.size - 1)
    }
}
