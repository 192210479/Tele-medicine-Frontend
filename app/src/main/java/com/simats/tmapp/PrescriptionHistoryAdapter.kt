package com.simats.tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.tmapp.api.PrescriptionHistoryResponse
import com.google.android.material.button.MaterialButton
import com.simats.tmapp.api.ApiClient
import com.bumptech.glide.Glide
import android.widget.ImageView

class PrescriptionHistoryAdapter(
    private var prescriptions: List<PrescriptionHistoryResponse>,
    private val onClick: (PrescriptionHistoryResponse) -> Unit
) : RecyclerView.Adapter<PrescriptionHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDoctorName: TextView = view.findViewById(R.id.tvDoctorName)
        val tvDiagnosis: TextView = view.findViewById(R.id.tvDiagnosis)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val btnViewDetails: MaterialButton = view.findViewById(R.id.btnViewDetails)
        val ivDoctor: ImageView = view.findViewById(R.id.ivDoctor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prescription_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = prescriptions[position]
        
        // Task 6: Populate fields using API response
        holder.tvDoctorName.text = "Dr. ${item.doctor_name}"
        holder.tvDiagnosis.text = item.diagnosis
        holder.tvDate.text = item.date

        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        val avatarUrl = "$baseUrl/api/profile/image/${item.doctorId}"
        Glide.with(holder.itemView.context)
            .load(avatarUrl)
            .placeholder(R.drawable.bg_circle_soft_blue)
            .circleCrop()
            .into(holder.ivDoctor)

        holder.btnViewDetails.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount() = prescriptions.size

    fun updateList(newList: List<PrescriptionHistoryResponse>) {
        prescriptions = newList
        notifyDataSetChanged()
    }
}
