package com.simats.Tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.SupportMessageResponse

class TicketChatAdapter(
    private val currentUserId: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER  = 0
        private const val TYPE_ADMIN = 1
    }

    private val list = mutableListOf<SupportMessageResponse>()

    fun submitList(data: List<SupportMessageResponse>) {
        list.clear()
        list.addAll(data)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (list[position].sender_role == "admin") TYPE_ADMIN else TYPE_USER
    }

    inner class UserVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTime: TextView    = view.findViewById(R.id.tvTime)
        val tvLabel: TextView   = view.findViewById(R.id.tvSenderLabel)
    }

    inner class AdminVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTime: TextView    = view.findViewById(R.id.tvTime)
        val tvLabel: TextView   = view.findViewById(R.id.tvSenderLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ADMIN) {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ticket_msg_right, parent, false)
            AdminVH(v)
        } else {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ticket_msg_left, parent, false)
            UserVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg  = list[position]
        val time = formatTime(msg.created_at)

        when (holder) {
            is UserVH -> {
                holder.tvMessage.text = msg.message
                holder.tvTime.text    = time
                holder.tvLabel.text   = msg.sender_role.replaceFirstChar { it.uppercaseChar() }
            }
            is AdminVH -> {
                holder.tvMessage.text = msg.message
                holder.tvTime.text    = time
                holder.tvLabel.text   = "Support Team"
            }
        }
    }

    override fun getItemCount() = list.size

    private fun formatTime(raw: String?): String {
        if (raw.isNullOrEmpty()) return ""
        return raw.take(16).replace("T", " ")
    }
}