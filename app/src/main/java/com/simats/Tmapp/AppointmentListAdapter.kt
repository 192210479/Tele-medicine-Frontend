package com.simats.Tmapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.AdminAppointmentResponse
import java.util.Locale
import java.util.TimeZone
import java.text.SimpleDateFormat

class AppointmentListAdapter(
    private var items: List<AdminAppointmentResponse>,
    private val onAction: (AdminAppointmentResponse, String) -> Unit
) : RecyclerView.Adapter<AppointmentListAdapter.AppointmentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_list_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<AdminAppointmentResponse>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class AppointmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvInitials: TextView = view.findViewById(R.id.tvInitials)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvInfo: TextView = view.findViewById(R.id.tvInfo)
        private val tvStatusChip: TextView = view.findViewById(R.id.tvStatusChip)
        private val btnCancel: TextView = view.findViewById(R.id.btnCancel)
        private val btnReassign: TextView = view.findViewById(R.id.btnReassign)
        private val llActions: LinearLayout = view.findViewById(R.id.llActions)
        private val vDivider: View = view.findViewById(R.id.vDivider)

        fun bind(appt: AdminAppointmentResponse) {
            val patientName = if (!appt.patientName.isNullOrBlank()) appt.patientName else "Unknown Patient"
            val isPatientUnknown = patientName == "Unknown Patient"
            tvName.text = patientName
            
            val doctorName = if (!appt.doctorName.isNullOrBlank()) appt.doctorName else "Unknown Doctor"
            val isDoctorUnknown = doctorName == "Unknown Doctor"
            val safeDoctorName = if (isDoctorUnknown) doctorName else {
                if (doctorName.startsWith("Dr.", ignoreCase = true)) doctorName else "Dr. $doctorName"
            }
            
            val safeDate = appt.date?.takeIf { it.isNotBlank() } ?: "TBD"
            val safeTime = if (!appt.utcTime.isNullOrEmpty()) {
                convertIsoUtcToLocal(appt.utcTime)
            } else {
                appt.time?.takeIf { it.isNotBlank() } ?: "N/A"
            }
            tvInfo.text = "$safeDoctorName • $safeDate | $safeTime"
            
            val initials = if (isPatientUnknown) "AP" else {
                patientName.split(" ")
                    .filter { it.isNotEmpty() }
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .joinToString("").take(2)
            }
            tvInitials.text = if (initials.isNotBlank()) initials else "AP"

            val safeStatus = appt.status?.takeIf { it.isNotBlank() }?.lowercase() ?: "pending"
            tvStatusChip.text = safeStatus.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            
            when (safeStatus) {
                "upcoming", "confirmed" -> {
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_blue)
                    tvStatusChip.setTextColor(Color.parseColor("#3B82F6"))
                    tvStatusChip.text = "Upcoming"
                }
                "completed" -> {
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_green)
                    tvStatusChip.setTextColor(Color.parseColor("#10B981"))
                    tvStatusChip.text = "Completed"
                }
                "cancelled" -> {
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_red)
                    tvStatusChip.setTextColor(Color.parseColor("#EF4444"))
                    tvStatusChip.text = "Cancelled"
                }
                else -> {
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_orange)
                    tvStatusChip.setTextColor(Color.parseColor("#F59E0B"))
                    tvStatusChip.text = "Pending"
                }
            }
            
            val showActions = safeStatus == "upcoming" || safeStatus == "pending"
            llActions.visibility = if (showActions) View.VISIBLE else View.GONE
            vDivider.visibility = if (showActions) View.VISIBLE else View.GONE

            btnCancel.setOnClickListener { onAction(appt, "cancel") }
            btnReassign.setOnClickListener { onAction(appt, "reassign") }
            itemView.setOnClickListener { onAction(appt, "click") }
        }

        private fun convertIsoUtcToLocal(utc: String?): String {
            if (utc.isNullOrEmpty()) return "N/A"
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(utc)
                val output = SimpleDateFormat("hh:mm a", Locale.getDefault())
                output.timeZone = TimeZone.getDefault()
                output.format(date!!)
            } catch (e: Exception) {
                utc
            }
        }
    }
}
