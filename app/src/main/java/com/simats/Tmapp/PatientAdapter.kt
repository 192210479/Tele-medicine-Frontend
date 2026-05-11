package com.simats.Tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.simats.Tmapp.api.ApiClient
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.Patient

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
            tvName.text = patient.fullName
            tvRecordCount.text = "${patient.totalRecords} records shared"
            tvLastAppt.text = if (!patient.lastAppointment.isNullOrBlank()) {
                "Last Appointment: ${TimeUtils.formatSimpleDate(patient.lastAppointment)}"
            } else {
                "Last Appointment: Not available"
            }

            val baseUrl = ApiClient.BASE_URL.removeSuffix("/")

            val finalPatientImage = when {
                !patient.profileImage.isNullOrEmpty() && patient.profileImage!!.startsWith("http") -> patient.profileImage
                !patient.profileImage.isNullOrEmpty() && patient.profileImage!!.startsWith("/") -> "$baseUrl${patient.profileImage}"
                patient.id != null -> "$baseUrl/api/profile/image/${patient.id}?role=patient"
                else -> null
            }

            AvatarUtils.loadAvatar(
                imageView = ivAvatar,
                imageUrl = finalPatientImage,
                name = patient.fullName ?: "Patient"
            )

            val clickAction = View.OnClickListener {
                android.util.Log.d("PATIENT_CLICK", "Opening records for patientId=${patient.id}, name=${patient.fullName}")
                onClick(patient)
            }

            // Make all major parts clickable
            itemView.setOnClickListener(clickAction)
            btnOpenRecords.setOnClickListener(clickAction)
            tvName.setOnClickListener(clickAction)
            tvRecordCount.setOnClickListener(clickAction)
            tvLastAppt.setOnClickListener(clickAction)
            ivAvatar.setOnClickListener(clickAction)

            itemView.isClickable = true
            itemView.isFocusable = true
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Patient>() {
        override fun areItemsTheSame(oldItem: Patient, newItem: Patient) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Patient, newItem: Patient) = oldItem == newItem
    }
}
