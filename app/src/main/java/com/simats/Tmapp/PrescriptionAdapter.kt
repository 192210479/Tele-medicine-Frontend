package com.simats.Tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.PrescriptionHistoryResponse

class PrescriptionAdapter(
    private var prescriptions: List<PrescriptionHistoryResponse>,
    private val onItemClick: (PrescriptionHistoryResponse) -> Unit
) : RecyclerView.Adapter<PrescriptionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDoctorName: TextView = view.findViewById(R.id.tvDoctorName)
        val tvSpecialization: TextView = view.findViewById(R.id.tvSpecialization)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val btnView: TextView = view.findViewById(R.id.btnView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prescription_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = prescriptions[position]
        holder.tvDoctorName.text = if (!item.doctor_name.isNullOrEmpty()) "Dr. ${item.doctor_name}" else "Doctor"
        holder.tvSpecialization.text = item.specialization ?: "Medical Specialist"
        holder.tvDate.text = item.date ?: ""
        
        // Finalized or Ready status
        holder.tvStatus.text = "Ready"
        holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_green)
        holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#10B981"))

        holder.btnView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = prescriptions.size

    fun updateList(newList: List<PrescriptionHistoryResponse>) {
        prescriptions = newList
        notifyDataSetChanged()
    }
}
