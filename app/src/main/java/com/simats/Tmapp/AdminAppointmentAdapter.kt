package com.simats.Tmapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.simats.Tmapp.api.ApiClient
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.AdminAppointmentResponse

class AdminAppointmentAdapter(
    private var items: List<Any>,
    private val onCancel: (AdminAppointmentResponse) -> Unit,
    private val onReassign: (AdminAppointmentResponse) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    class AppointmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPatientAvatar: ImageView? = view.findViewById(R.id.ivPatientAvatar)
        val ivDoctorAvatar: ImageView? = view.findViewById(R.id.ivDoctorAvatar)

        val tvPatientName: TextView = view.findViewById(R.id.tvPatientName)
        val tvDoctorName: TextView = view.findViewById(R.id.tvDoctorName)
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        val btnCancel: View = view.findViewById(R.id.btnCancel)
        val btnReassign: View = view.findViewById(R.id.btnReassign)
        val llActions: View = view.findViewById(R.id.llActions)
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvSpecialty: TextView = view.findViewById(R.id.tvSpecialty)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_admin_doctor_mini, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_admin_appointment, parent, false)
            AppointmentViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            val doctorName = items[position] as String
            holder.tvName.text = "Dr. $doctorName"
            holder.tvSpecialty.text = "All Appointments"
            holder.tvStatus.visibility = View.GONE
            holder.itemView.setBackgroundColor(Color.parseColor("#F3F4F6")) // Light grey background for header
        } else if (holder is AppointmentViewHolder) {
            val appt = items[position] as AdminAppointmentResponse
            holder.tvPatientName.text = appt.patientName ?: "Unknown Patient"
            holder.tvDoctorName.text = "Dr. ${appt.doctorName ?: "Unknown"}${if (appt.specialization != null) " (${appt.specialization})" else ""}"

            val baseUrl = ApiClient.BASE_URL.removeSuffix("/")

            // PATIENT IMAGE
            if (holder.ivPatientAvatar != null) {
                val patientImageUrl = if (appt.patientId != null)
                    "$baseUrl/api/profile/image/${appt.patientId}?role=patient"
                else null

                AvatarUtils.loadAvatar(
                    imageView = holder.ivPatientAvatar,
                    imageUrl = patientImageUrl,
                    name = appt.patientName ?: "Patient"
                )
            }

            // DOCTOR IMAGE
            if (holder.ivDoctorAvatar != null) {
                val doctorImageUrl = if (appt.doctorId != null)
                    "$baseUrl/api/profile/image/${appt.doctorId}?role=doctor"
                else null

                AvatarUtils.loadAvatar(
                    imageView = holder.ivDoctorAvatar,
                    imageUrl = doctorImageUrl,
                    name = appt.doctorName ?: "Doctor"
                )
            }
            holder.tvDateTime.text = if (appt.time != null) "${appt.date ?: ""} | ${appt.time}" else appt.date ?: ""
            
            val status = appt.status?.lowercase() ?: ""
            holder.tvStatusBadge.text = appt.status ?: "Unknown"

            when (status) {
                "scheduled", "upcoming" -> {
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_blue)
                    holder.tvStatusBadge.setTextColor(Color.parseColor("#3B82F6"))
                }
                "missed", "pending" -> {
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_orange)
                    holder.tvStatusBadge.setTextColor(Color.parseColor("#F59E0B"))
                }
                "completed" -> {
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_green)
                    holder.tvStatusBadge.setTextColor(Color.parseColor("#10B981"))
                }
                "cancelled" -> {
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_red)
                    holder.tvStatusBadge.setTextColor(Color.parseColor("#EF4444"))
                }
                else -> {
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_blue)
                    holder.tvStatusBadge.setTextColor(Color.parseColor("#3B82F6"))
                }
            }

            val showActions = status == "scheduled" || status == "upcoming"
            holder.llActions.visibility = if (showActions) View.VISIBLE else View.GONE

            holder.btnCancel.setOnClickListener { onCancel(appt) }
            holder.btnReassign.setOnClickListener {
                onReassign(appt)
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateGroupedData(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }
}
