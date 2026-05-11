package com.simats.Tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.ConsultationChatMessageResponse

class ChatAdapterV2(
    private val messages: List<ConsultationChatMessageResponse>,
    private val currentUserId: Int
) : RecyclerView.Adapter<ChatAdapterV2.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val llSent: LinearLayout = view.findViewById(R.id.llSentMessage)
        val tvSentText: TextView = view.findViewById(R.id.tvSentText)
        val tvSentTime: TextView = view.findViewById(R.id.tvSentTime)

        val llReceived: LinearLayout = view.findViewById(R.id.llReceivedMessage)
        val tvReceivedText: TextView = view.findViewById(R.id.tvReceivedText)
        val tvReceivedTime: TextView = view.findViewById(R.id.tvReceivedTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_msg, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]
        
        if (msg.senderId == currentUserId) {
            holder.llSent.visibility = View.VISIBLE
            holder.llReceived.visibility = View.GONE
            holder.tvSentText.text = msg.message
            holder.tvSentTime.text = formatTime(msg.timestamp)
        } else {
            holder.llSent.visibility = View.GONE
            holder.llReceived.visibility = View.VISIBLE
            holder.tvReceivedText.text = msg.message
            holder.tvReceivedTime.text = formatTime(msg.timestamp)
        }
    }

    override fun getItemCount() = messages.size

    private fun formatTime(timestamp: String?): String {
        return try {
            if (timestamp?.contains(" ") == true) {
                timestamp.split(" ")[1]
            } else {
                timestamp ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
