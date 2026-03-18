package com.simats.tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.simats.tmapp.api.ApiClient
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simats.tmapp.api.Patient

class PatientAdapter(
    private val onItemClick: (Patient) -> Unit
) : ListAdapter<Patient, PatientAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val patient = getItem(position)
        holder.bind(patient, onItemClick)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvPatientName)
        private val tvRecordCount: TextView = view.findViewById(R.id.tvRecordCount)
        private val tvLastAppt: TextView = view.findViewById(R.id.tvLastAppt)
        private val ivAvatar: ImageView = view.findViewById(R.id.ivPatientAvatar)
        private val btnOpenRecords: View = view.findViewById(R.id.btnOpenRecords)

        fun bind(patient: Patient, onClick: (Patient) -> Unit) {
            tvName.text = patient.name
            tvRecordCount.text = "${patient.totalRecords} records shared"
            tvLastAppt.text = TimeUtils.formatSimpleDate(patient.lastAppointment)
            
            val avatarUrl = "${ApiClient.BASE_URL}api/profile/image/${patient.id}?role=patient"
            Glide.with(itemView.context)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_user_placeholder)
                .error(R.drawable.ic_user_placeholder)
                .circleCrop()
                .into(ivAvatar)
                
            btnOpenRecords.setOnClickListener { onClick(patient) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Patient>() {
        override fun areItemsTheSame(oldItem: Patient, newItem: Patient) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Patient, newItem: Patient) = oldItem == newItem
    }
}
