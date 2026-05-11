package com.simats.Tmapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.SupportTicketResponse

class SupportTicketAdapter(
    private val onClick: (SupportTicketResponse) -> Unit
) : RecyclerView.Adapter<SupportTicketAdapter.VH>() {

    private val list = mutableListOf<SupportTicketResponse>()

    fun submitList(data: List<SupportTicketResponse>?) {
        list.clear()
        if (data != null) list.addAll(data)
        notifyDataSetChanged()
    }

    fun isEmpty() = list.isEmpty()

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvIssueType: TextView = view.findViewById(R.id.tvIssueType)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]

        holder.tvTitle.text = item.title
        holder.tvStatus.text = item.status.replaceFirstChar { it.uppercaseChar() }
        holder.tvIssueType.text = "Issue: ${item.issue_type}"

        // Format date: "2025-04-10T12:34:56" → "Apr 10, 2025"
        val rawDate = item.created_at ?: ""
        holder.tvDate.text = if (rawDate.isNotEmpty()) {
            "Opened: ${rawDate.take(10)}"
        } else {
            ""
        }

        // Status chip color
        val chipBg = holder.tvStatus.background?.mutate()
        val color = when (item.status.lowercase()) {
            "open"       -> Color.parseColor("#2979FF")
            "closed", "resolved" -> Color.parseColor("#4CAF50")
            "escalated"  -> Color.parseColor("#F44336")
            "pending"    -> Color.parseColor("#FF9800")
            else         -> Color.parseColor("#9E9E9E")
        }
        chipBg?.setTint(color)
        holder.tvStatus.background = chipBg

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = list.size
}
