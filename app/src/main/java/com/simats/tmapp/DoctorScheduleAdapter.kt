package com.simats.tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.simats.tmapp.api.AppointmentResponse
import java.text.SimpleDateFormat
import java.util.*

class DoctorScheduleAdapter(
    private val onItemClick: (AppointmentResponse) -> Unit,
    private val onStartConsultationClick: ((AppointmentResponse) -> Unit)? = null,
    private val onViewReportsClick: ((Int, Int) -> Unit)? = null // patientId, appointmentId
) : RecyclerView.Adapter<DoctorScheduleAdapter.ViewHolder>() {

    private var appointments: List<AppointmentResponse> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appt = appointments[position]
        holder.bind(appt)
        holder.itemView.setOnClickListener { onItemClick(appt) }
        holder.btnStartConsultation.setOnClickListener { onStartConsultationClick?.invoke(appt) }
        holder.btnViewReports.setOnClickListener { onViewReportsClick?.invoke(appt.patientId ?: -1, appt.id) }
    }

    override fun getItemCount(): Int = appointments.size

    fun updateList(newList: List<AppointmentResponse>) {
        appointments = newList
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTime: TextView = view.findViewById(R.id.tvApptTime)
        private val tvAMPM: TextView = view.findViewById(R.id.tvApptAMPM)
        private val tvName: TextView = view.findViewById(R.id.tvPatientName)
        private val tvConsultType: TextView = view.findViewById(R.id.tvConsultationType)
        private val tvApptId: TextView = view.findViewById(R.id.tvApptId)
        private val tvApptDate: TextView = view.findViewById(R.id.tvApptDate)
        private val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        val btnStartConsultation: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btnStartConsultation)
        val btnViewReports: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btnViewReports)

        fun bind(appt: AppointmentResponse) {
            tvName.text = appt.patientName ?: "Patient"
            tvConsultType.text = appt.specialization ?: "Video Consultation"
            tvApptId.text = "Appt ID: #${appt.id}"
            val combinedUtc = "${appt.date} ${appt.time}"
            tvApptDate.text = TimeUtils.convertUtcToLocal(combinedUtc, outputPattern = "MMM dd, yyyy")
            
            // Format time (09:00:00 or 09:00 -> 09:00 AM)
            tvTime.text = TimeUtils.convertUtcToLocal(combinedUtc, outputPattern = "hh:mm")
            tvAMPM.text = TimeUtils.convertUtcToLocal(combinedUtc, outputPattern = "a").uppercase()

            // Status Styling
            val status = appt.status?.lowercase() ?: ""
            val consultationStatus = appt.consultationStatus?.lowercase() ?: ""

            when {
                consultationStatus == "waiting" -> {
                    tvStatusBadge.text = "waiting"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_orange)
                    tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEF3C7"))
                    tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                }
                status == "scheduled" -> {
                    tvStatusBadge.text = "upcoming"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_blue)
                    tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#DBEAFE"))
                    tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#2563EB"))
                }
                else -> {
                    tvStatusBadge.text = "pending"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_grey)
                    tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E5E7EB"))
                    tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#6B7280"))
                }
            }

            // Show start consultation button if status is Pending or Ready
            if (consultationStatus == "pending" || consultationStatus == "ready") {
                btnStartConsultation.visibility = View.VISIBLE
            } else {
                btnStartConsultation.visibility = View.GONE
            }
        }
    }
}
