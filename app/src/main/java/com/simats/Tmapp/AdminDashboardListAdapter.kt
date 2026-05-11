package com.simats.Tmapp

import android.graphics.Color
import java.util.Locale
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.AdminAppointmentResponse
import com.simats.Tmapp.api.Doctor
import com.simats.Tmapp.api.Patient

class AdminDashboardListAdapter(
    private var items: List<Any>,
    private val onAction: (Any, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DOCTOR = 1
        private const val TYPE_PATIENT = 2
        private const val TYPE_APPOINTMENT = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Doctor -> TYPE_DOCTOR
            is Patient -> TYPE_PATIENT
            else -> TYPE_APPOINTMENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DOCTOR -> DoctorViewHolder(inflater.inflate(R.layout.item_admin_list_doctor, parent, false))
            TYPE_PATIENT -> PatientViewHolder(inflater.inflate(R.layout.item_admin_list_patient, parent, false))
            else -> AppointmentViewHolder(inflater.inflate(R.layout.item_admin_list_appointment, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is DoctorViewHolder -> holder.bind(item as Doctor)
            is PatientViewHolder -> holder.bind(item as Patient)
            is AppointmentViewHolder -> holder.bind(item as AdminAppointmentResponse)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class DoctorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvInitials: TextView = view.findViewById(R.id.tvInitials)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvInfo: TextView = view.findViewById(R.id.tvInfo)
        private val tvStatusChip: TextView = view.findViewById(R.id.tvStatusChip)
        private val ivMore: ImageView = view.findViewById(R.id.ivMore)

        fun bind(doctor: Doctor) {
            val name = if (!doctor.name.isNullOrBlank()) doctor.name else "Unknown Doctor"
            val isUnknown = name == "Unknown Doctor"
            
            tvName.text = if (isUnknown) name else {
                if (name.startsWith("Dr.", ignoreCase = true)) name else "Dr. $name"
            }
            
            val safeSpecialization = doctor.specialization?.takeIf { it.isNotBlank() } ?: "General"
            val expYears = doctor.experience ?: 0
            tvInfo.text = "$safeSpecialization • $expYears Years Exp"
            
            val initials = if (isUnknown) "DR" else {
                name.split(" ")
                    .filter { it.isNotEmpty() && !it.equals("Dr.", true) }
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .joinToString("").take(2)
            }
            tvInitials.text = if (initials.isNotBlank()) initials else "DR"

            val safeStatus = doctor.status?.takeIf { it.isNotBlank() }?.lowercase() ?: "pending"
            tvStatusChip.text = safeStatus.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            
            when (safeStatus) {
                "active", "approved" -> {
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_green)
                    tvStatusChip.setTextColor(Color.parseColor("#10B981"))
                    tvStatusChip.text = "Active"
                }
                "pending" -> {
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_orange)
                    tvStatusChip.setTextColor(Color.parseColor("#F59E0B"))
                    tvStatusChip.text = "Pending"
                }
                else -> {
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_red)
                    tvStatusChip.setTextColor(Color.parseColor("#EF4444"))
                    tvStatusChip.text = if (safeStatus == "rejected") "Rejected" else "Inactive"
                }
            }
            
            ivMore.setOnClickListener { onAction(doctor, "menu") }
            itemView.setOnClickListener { onAction(doctor, "click") }
        }
    }

    inner class PatientViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvInitials: TextView = view.findViewById(R.id.tvInitials)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvInfo: TextView = view.findViewById(R.id.tvInfo)
        private val tvStatusChip: TextView = view.findViewById(R.id.tvStatusChip)
        private val ivMore: ImageView = view.findViewById(R.id.ivMore)

        fun bind(patient: Patient) {
            val name = if (!patient.fullName.isNullOrBlank()) patient.fullName else "Unknown Patient"
            val isUnknown = name == "Unknown Patient"
            
            tvName.text = name
            
            val safeLastVisit = patient.lastAppointment?.takeIf { it.isNotBlank() } ?: "Recent"
            tvInfo.text = "Age: ${patient.age ?: "N/A"} • Last Visit: $safeLastVisit"
            
            val initials = if (isUnknown) "PA" else {
                name.split(" ")
                    .filter { it.isNotEmpty() }
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .joinToString("").take(2)
            }
            tvInitials.text = if (initials.isNotBlank()) initials else "PA"

            // Patient model doesn't have status, default to Active
            tvStatusChip.text = "Active"
            tvStatusChip.setBackgroundResource(R.drawable.bg_badge_green)
            tvStatusChip.setTextColor(Color.parseColor("#10B981"))
            
            ivMore.setOnClickListener { onAction(patient, "menu") }
            itemView.setOnClickListener { onAction(patient, "click") }
        }
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
            val safeTime = appt.time?.takeIf { it.isNotBlank() } ?: "N/A"
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
    }
}
