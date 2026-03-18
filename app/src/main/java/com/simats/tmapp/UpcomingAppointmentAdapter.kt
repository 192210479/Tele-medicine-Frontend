package com.simats.tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.simats.tmapp.api.AppointmentResponse
import com.simats.tmapp.api.ApiClient
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

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
        val joinButton: MaterialButton = view.findViewById(R.id.btnJoinConsultation)
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
        holder.date.text = formatDate(appointment.date)
        holder.time.text = formatTime(appointment.time)

        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        val avatarUrl = "$baseUrl/api/profile/image/${appointment.doctorId}"
        Glide.with(holder.itemView.context)
            .load(avatarUrl)
            .placeholder(R.drawable.bg_circle_soft_blue)
            .error(R.drawable.bg_circle_soft_blue)
            .circleCrop()
            .into(holder.doctorImage)

        val statusText = appointment.status ?: "Upcoming"
        holder.itemView.findViewById<TextView>(R.id.tvStatusPill).text = statusText
        
        // Date Logic for Buttons
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
        if (appointment.date == today) {
            holder.joinButton.text = "Join"
            holder.joinButton.setOnClickListener { onJoinClick(appointment) }
        } else {
            holder.joinButton.text = "View"
            holder.joinButton.setOnClickListener { onDetailsClick(appointment) }
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
