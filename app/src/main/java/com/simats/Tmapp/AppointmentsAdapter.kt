package com.simats.Tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.AppointmentResponse
import com.simats.Tmapp.api.ApiClient
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class AppointmentsAdapter(
    private var appointments: List<AppointmentResponse>,
    private val isDoctor: Boolean,
    private val onStartConsultationClick: ((AppointmentResponse) -> Unit)? = null,
    private val onCancelClick: ((AppointmentResponse) -> Unit)? = null,
    private val onShareReportsClick: ((AppointmentResponse) -> Unit)? = null,
    private val onViewDetailsClick: (AppointmentResponse) -> Unit
) : RecyclerView.Adapter<AppointmentsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.ivAvatar)
        val name: TextView = view.findViewById(R.id.tvName)
        val subtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val missedMessage: TextView = view.findViewById(R.id.tvMissedMessage)
        val result: TextView = view.findViewById(R.id.tvResult)
        val date: TextView = view.findViewById(R.id.tvDate)
        val time: TextView = view.findViewById(R.id.tvTime)
        val llViewDetails: LinearLayout = view.findViewById(R.id.llViewDetails)
        val btnMore: ImageView? = view.findViewById(R.id.ivOptions)
        val btnStartConsultation: com.google.android.material.button.MaterialButton? = view.findViewById(R.id.btnStartConsultation)
        val btnShare: com.google.android.material.button.MaterialButton? = view.findViewById(R.id.btnShare)
        val btnPrescription: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btnPrescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        val status = appointment.status ?: ""
        val consStatus = appointment.consultationStatus ?: ""

        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        if (isDoctor) {
            holder.name.text = appointment.patientName ?: "Unknown Patient"
            holder.subtitle.visibility = View.GONE
            
            val patientImgUrl = if (!appointment.patientImage.isNullOrEmpty()) {
                if (appointment.patientImage.startsWith("http")) appointment.patientImage
                else "$baseUrl${appointment.patientImage}"
            } else "$baseUrl/api/profile/image/${appointment.patientId}?role=patient"
            
            val patientName = appointment.patientName ?: "Patient"
            AvatarUtils.loadAvatar(holder.avatar, patientImgUrl, patientName)
        } else {
            holder.name.text = if (appointment.doctorName?.lowercase() == "null" || appointment.doctorName.isNullOrEmpty()) "Doctor" else appointment.doctorName
            holder.subtitle.text = appointment.specialization ?: "Specialization"
            holder.subtitle.visibility = View.VISIBLE
            
            val doctorImgUrl = if (!appointment.doctorImage.isNullOrEmpty()) {
                if (appointment.doctorImage.startsWith("http")) appointment.doctorImage
                else "$baseUrl${appointment.doctorImage}"
            } else "$baseUrl/api/profile/image/${appointment.doctorId}?role=doctor"
            
            val doctorName = appointment.doctorName ?: "Doctor"
            AvatarUtils.loadAvatar(holder.avatar, doctorImgUrl, doctorName)
        }

        // Parse date and time
        val combinedUtc = "${appointment.date} ${appointment.localTime ?: ""}"
        holder.date.text = TimeUtils.convertUtcToLocal(combinedUtc, outputPattern = "MMM dd, yyyy")
        holder.time.text = TimeUtils.convertUtcToLocal(combinedUtc, outputPattern = "hh:mm a")

        // Status Display
        holder.result.visibility = View.VISIBLE
        if (!appointment.cancellationReason.isNullOrEmpty()) {
            holder.result.text = "Status: $status\nReason: ${appointment.cancellationReason}"
        } else {
            holder.result.text = status
        }

        holder.missedMessage.visibility = View.GONE

        holder.llViewDetails.setOnClickListener {
            onViewDetailsClick(appointment)
        }

        holder.btnMore?.setOnClickListener { view ->
            val popup = androidx.appcompat.widget.PopupMenu(view.context, view)
            if (status == "Upcoming") {
                popup.menu.add("Cancel Appointment")
            }
            popup.menu.add("View Details")
            
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Cancel Appointment" -> {
                        onCancelClick?.invoke(appointment)
                        true
                    }
                    "View Details" -> {
                        onViewDetailsClick(appointment)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        if (status == "Completed") {
            holder.btnPrescription.visibility = View.VISIBLE
            
            if (isDoctor) {
                holder.btnPrescription.text = "View Prescription"
                holder.btnPrescription.setOnClickListener {
                    val intent = android.content.Intent(holder.itemView.context, ViewPrescriptionActivity::class.java)
                    intent.putExtra("appointment_id", appointment.id)
                    intent.putExtra("doctor_id", appointment.doctorId)
                    intent.putExtra("doctor_name", appointment.doctorName)
                    intent.putExtra("doctor_specialization", appointment.specialization)
                    holder.itemView.context.startActivity(intent)
                }
            } else {
                // For Patient, still check if Ready
                ApiClient.instance.getPrescriptionStatus(appointment.id).enqueue(object : retrofit2.Callback<com.simats.Tmapp.api.PrescriptionStatusResponse> {
                    override fun onResponse(call: retrofit2.Call<com.simats.Tmapp.api.PrescriptionStatusResponse>, response: retrofit2.Response<com.simats.Tmapp.api.PrescriptionStatusResponse>) {
                        if (response.isSuccessful) {
                            val prescStatus = response.body()?.status
                            if (prescStatus == "Ready") {
                                holder.btnPrescription.visibility = View.VISIBLE
                                holder.btnPrescription.text = "View Prescription"
                                holder.btnPrescription.setOnClickListener {
                                val intent = android.content.Intent(holder.itemView.context, ViewPrescriptionActivity::class.java)
                                intent.putExtra("appointment_id", appointment.id)
                                intent.putExtra("doctor_id", appointment.doctorId)
                                intent.putExtra("doctor_name", appointment.doctorName)
                                intent.putExtra("doctor_specialization", appointment.specialization)
                                holder.itemView.context.startActivity(intent)
                                }
                            } else {
                                holder.btnPrescription.visibility = View.GONE
                            }
                        } else {
                            holder.btnPrescription.visibility = View.GONE
                        }
                    }
                    override fun onFailure(call: retrofit2.Call<com.simats.Tmapp.api.PrescriptionStatusResponse>, t: Throwable) {
                        holder.btnPrescription.visibility = View.GONE
                    }
                })
            }
        } else {
            holder.btnPrescription.visibility = View.GONE
        }

        // Show Join/Start Consultation button for relevant statuses
        if (status.equals("Upcoming", true) || status.equals("Scheduled", true) || status.equals("Booked", true)) {
            holder.btnStartConsultation?.visibility = View.VISIBLE
            holder.btnStartConsultation?.setOnClickListener {
                onStartConsultationClick?.invoke(appointment)
            }
            
            // Show Share Reports button for patients
            if (!isDoctor) {
                holder.btnShare?.visibility = View.VISIBLE
                holder.btnShare?.setOnClickListener {
                    onShareReportsClick?.invoke(appointment)
                }
            } else {
                holder.btnShare?.visibility = View.GONE
            }
        } else {
            holder.btnStartConsultation?.visibility = View.GONE
            holder.btnShare?.visibility = View.GONE
        }
    }

    override fun getItemCount() = appointments.size

    fun updateList(newList: List<AppointmentResponse>) {
        appointments = newList
        notifyDataSetChanged()
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "Unknown"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val parsed = parser.parse(dateStr)
            if (parsed != null) formatter.format(parsed) else dateStr
        } catch (e: ParseException) {
            dateStr
        }
    }

    private fun formatTime(timeStr: String?): String {
        if (timeStr.isNullOrEmpty()) return "Unknown"
        return try {
            val parser = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val parsed = parser.parse(timeStr)
            if (parsed != null) formatter.format(parsed) else {
                val parser2 = SimpleDateFormat("HH:mm", Locale.getDefault())
                val parsed2 = parser2.parse(timeStr)
                if(parsed2 != null) formatter.format(parsed2) else timeStr
            }
        } catch (e: ParseException) {
            timeStr
        }
    }
}
