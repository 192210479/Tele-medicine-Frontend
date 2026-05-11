package com.simats.Tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.AppointmentResponse

class DoctorScheduleAdapter(
    private val onItemClick: (AppointmentResponse) -> Unit,
    private val onStartConsultationClick: ((AppointmentResponse) -> Unit)? = null,
    private val onViewReportsClick: ((Int, Int) -> Unit)? = null
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
        private val tvName: TextView = view.findViewById(R.id.tvPatientName)
        private val tvConsultType: TextView = view.findViewById(R.id.tvConsultationType)
        private val tvDateTime: TextView = view.findViewById(R.id.tvApptDateTime)
        private val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        val btnStartConsultation: com.google.android.material.button.MaterialButton =
            view.findViewById(R.id.btnStartConsultation)
        val btnViewReports: com.google.android.material.button.MaterialButton =
            view.findViewById(R.id.btnViewReports)

        fun bind(appt: AppointmentResponse) {
            tvName.text = DoctorAppointmentHelper.cleanText(appt.patientName, "Patient")
            tvConsultType.text = DoctorAppointmentHelper.cleanText(appt.specialization, "Video Consultation")
            val date = DoctorAppointmentHelper.cleanText(appt.date)
            val time = DoctorAppointmentHelper.cleanText(appt.localTime)
            tvDateTime.text = if (time.isNotEmpty()) "$date • $time" else date

            val statusEnum = DoctorAppointmentHelper.getStatus(appt)
            val consultationStatus = appt.consultationStatus?.lowercase() ?: ""
            val status = appt.status?.lowercase() ?: ""

            when {
                consultationStatus == "waiting" || consultationStatus == "ready" -> {
                    tvStatusBadge.text = "READY"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_green)
                    tvStatusBadge.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#DCFCE7"))
                    tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#16A34A"))
                }
                statusEnum == DoctorAppointmentHelper.AppointmentStatus.COMPLETED -> {
                    tvStatusBadge.text = "COMPLETED"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_green)
                    tvStatusBadge.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#DCFCE7"))
                    tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#16A34A"))
                }
                statusEnum == DoctorAppointmentHelper.AppointmentStatus.CANCELLED -> {
                    tvStatusBadge.text = "CANCELLED"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_red)
                    tvStatusBadge.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEE2E2"))
                    tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                }
                statusEnum == DoctorAppointmentHelper.AppointmentStatus.MISSED -> {
                    tvStatusBadge.text = "MISSED"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_orange)
                    tvStatusBadge.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEF3C7"))
                    tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#D97706"))
                }
                else -> {
                    tvStatusBadge.text = "UPCOMING"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_blue)
                    tvStatusBadge.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#DBEAFE"))
                    tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#2563EB"))
                }
            }

            val canStart =
                consultationStatus.equals("pending", ignoreCase = true) ||
                consultationStatus.equals("ready", ignoreCase = true) ||
                consultationStatus.equals("waiting", ignoreCase = true) ||
                status.equals("scheduled", ignoreCase = true) ||
                status.equals("upcoming", ignoreCase = true)
            btnStartConsultation.visibility =
                if (canStart && !status.equals("completed", ignoreCase = true) && !consultationStatus.equals("completed", ignoreCase = true)) View.VISIBLE else View.GONE

            btnViewReports.visibility =
                if (status.equals("completed", ignoreCase = true) || consultationStatus.equals("completed", ignoreCase = true)) View.VISIBLE else View.GONE
            btnViewReports.text = "View Details"
        }
    }
}
