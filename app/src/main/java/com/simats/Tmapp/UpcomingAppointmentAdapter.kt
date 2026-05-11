package com.simats.Tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.AppointmentResponse
import com.simats.Tmapp.api.ApiClient
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class UpcomingAppointmentAdapter(
    private var appointments: List<AppointmentResponse>,
    private val onJoinClick: (AppointmentResponse) -> Unit,
    private val onDetailsClick: (AppointmentResponse) -> Unit
) : RecyclerView.Adapter<UpcomingAppointmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val doctorName: TextView = view.findViewById(R.id.tvDoctorName)
        val specialization: TextView = view.findViewById(R.id.tvSpecialization)
        val date: TextView = view.findViewById(R.id.tvDate)
        val time: TextView = view.findViewById(R.id.tvTime)
        val doctorImage: ImageView = view.findViewById(R.id.ivDoctor)
        val joinButton: MaterialButton = view.findViewById(R.id.join_btn)
        val detailsButton: MaterialButton = view.findViewById(R.id.btn_details)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_upcoming_appointment, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.doctorName.text = appointment.doctorName
        holder.specialization.text = appointment.specialization

        val localDateStr = if (appointment.utcTime != null) {
            TimeUtils.convertIsoUtcToLocalDate(appointment.utcTime)
        } else {
            appointment.date ?: ""
        }

        val displayTime = if (appointment.utcTime != null) {
            TimeUtils.convertIsoUtcToLocal(appointment.utcTime)
        } else {
            formatTime(appointment.localTime)
        }

        holder.time.text = displayTime

        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")

        val finalDoctorImage = when {
            !appointment.doctorImage.isNullOrEmpty() && appointment.doctorImage!!.startsWith("http") -> appointment.doctorImage
            !appointment.doctorImage.isNullOrEmpty() && appointment.doctorImage!!.startsWith("/") -> "$baseUrl${appointment.doctorImage}"
            appointment.doctorId != null -> "$baseUrl/api/profile/image/${appointment.doctorId}?role=doctor"
            else -> null
        }

        AvatarUtils.loadAvatar(
            imageView = holder.doctorImage,
            imageUrl = finalDoctorImage,
            name = appointment.doctorName ?: "Doctor"
        )

        // Status Chip and Button Logic
        val todayCalendar = Calendar.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(todayCalendar.time)
        
        val tomorrowCalendar = Calendar.getInstance()
        tomorrowCalendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tomorrowCalendar.time)

        val statusPill = holder.itemView.findViewById<TextView>(R.id.tvStatusPill)
        
        // Always show Join Consultation for all upcoming appointments
        holder.joinButton.visibility = View.VISIBLE
        holder.detailsButton.visibility = View.GONE
        holder.joinButton.setOnClickListener { onJoinClick(appointment) }

        when (localDateStr) {
            todayStr -> {
                statusPill.text = "Today"
                statusPill.setBackgroundResource(R.drawable.bg_pill_green)
                statusPill.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.green_success))
                holder.date.text = "Today"
            }
            tomorrowStr -> {
                statusPill.text = "Tomorrow"
                statusPill.setBackgroundResource(R.drawable.bg_pill_blue)
                statusPill.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.primary_blue))
                holder.date.text = "Tomorrow"
            }
            else -> {
                statusPill.text = "Scheduled"
                statusPill.setBackgroundResource(R.drawable.bg_pill_grey)
                statusPill.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_grey))
                holder.date.text = formatDate(localDateStr)
            }
        }
    }

    private fun isInConsultationWindow(appointment: AppointmentResponse): Boolean {
        try {
            val now = Calendar.getInstance()
            
            val startCal = if (appointment.utcTime != null) {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(appointment.utcTime) ?: return false
                Calendar.getInstance().apply { 
                    time = date
                    add(Calendar.MINUTE, -10) // 10 minutes buffer
                }
            } else {
                val timeParser = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val startTime = timeParser.parse(appointment.localTime ?: return false) ?: return false
                Calendar.getInstance().apply {
                    time = startTime
                    set(Calendar.YEAR, now.get(Calendar.YEAR))
                    set(Calendar.MONTH, now.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR))
                    add(Calendar.MINUTE, -10)
                }
            }
            
            val endCal = if (appointment.utcTime != null) {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(appointment.utcTime) ?: return false
                Calendar.getInstance().apply { 
                    time = date
                    add(Calendar.MINUTE, 30) // Default 30 min duration
                }
            } else {
                val timeParser = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val endTimeStr = appointment.endTime ?: return true
                val endTime = timeParser.parse(endTimeStr) ?: return true
                Calendar.getInstance().apply {
                    time = endTime
                    set(Calendar.YEAR, now.get(Calendar.YEAR))
                    set(Calendar.MONTH, now.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR))
                }
            }
            
            return now.after(startCal) && now.before(endCal)
        } catch (e: Exception) {
            return false
        }
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

    override fun getItemCount() = appointments.size

    fun updateList(newList: List<AppointmentResponse>) {
        appointments = newList
        notifyDataSetChanged()
    }
}
