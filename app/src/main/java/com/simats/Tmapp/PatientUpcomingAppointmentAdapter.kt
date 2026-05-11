package com.simats.Tmapp

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.AppointmentResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class PatientUpcomingAppointmentAdapter(
    private var appointments: List<AppointmentResponse>,
    private val onJoinClick: (AppointmentResponse) -> Unit,
    private val onDetailsClick: (AppointmentResponse) -> Unit
) : RecyclerView.Adapter<PatientUpcomingAppointmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val doctorName: TextView = view.findViewById(R.id.tvPatientDoctorName)
        val specialization: TextView = view.findViewById(R.id.tvPatientSpecialization)
        val date: TextView = view.findViewById(R.id.tvPatientDate)
        val time: TextView = view.findViewById(R.id.tvPatientTime)
        val doctorImage: ImageView = view.findViewById(R.id.ivDoctorImage)
        val statusPill: TextView = view.findViewById(R.id.tvPatientStatusPill)
        val btnJoin: Button = view.findViewById(R.id.btnJoin)
        val btnDetails: Button = view.findViewById(R.id.btnDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient_upcoming_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = appointments[position]

        // ── Doctor Info ────────────────────────────────────────────────────────
        val doctorNameText = item.doctorName ?: "Doctor"
        holder.doctorName.text = if (doctorNameText.startsWith("Dr.")) doctorNameText else "Dr. $doctorNameText"
        holder.specialization.text = item.specialization ?: ""

        // ── Resolve local date (mirrors doctor-side UpcomingAppointmentAdapter) ──
        // item.date can be null when the backend only sends utcTime; use TimeUtils
        // to convert so isToday is always correct regardless of which field arrives.
        val localDateStr: String = if (!item.utcTime.isNullOrEmpty()) {
            TimeUtils.convertIsoUtcToLocalDate(item.utcTime)
        } else {
            item.date ?: ""
        }

        holder.date.text = localDateStr
        holder.time.text = when {
            !item.localTime.isNullOrEmpty() -> item.localTime
            !item.time.isNullOrEmpty()      -> item.time
            else                            -> convertUTCToLocal(item.utcTime)
        }

        // ── Image ──────────────────────────────────────────────────────────────
        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        val docName = item.doctorName ?: "Doctor"
        val docImgUrl = if (!item.doctorImage.isNullOrEmpty()) {
            if (item.doctorImage.startsWith("http")) item.doctorImage
            else "$baseUrl${item.doctorImage}"
        } else "$baseUrl/api/profile/image/${item.doctorId}?role=doctor"
        AvatarUtils.loadAvatar(holder.doctorImage, docImgUrl, docName)

        // ── Status badge (use resolved localDateStr, not raw item.date) ─────────
        val dateFmt   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = dateFmt.format(Calendar.getInstance().time)
        val tomorrowDate = dateFmt.format(
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
        )

        when {
            (item.status ?: "").equals("Cancelled", ignoreCase = true) -> {
                holder.statusPill.text = "Cancelled"
                holder.statusPill.setBackgroundResource(R.drawable.bg_pill_grey)
                holder.statusPill.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_grey))
            }
            (item.status ?: "").equals("Missed", ignoreCase = true) -> {
                holder.statusPill.text = "Missed"
                holder.statusPill.setBackgroundResource(R.drawable.bg_pill_grey)
                holder.statusPill.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_grey))
            }
            localDateStr == todayDate -> {
                holder.statusPill.text = "Today"
                holder.statusPill.setBackgroundResource(R.drawable.bg_pill_green)
                holder.statusPill.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.green_success))
            }
            localDateStr == tomorrowDate -> {
                holder.statusPill.text = "Tomorrow"
                holder.statusPill.setBackgroundResource(R.drawable.bg_pill_blue)
                holder.statusPill.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.primary_blue))
            }
            else -> {
                holder.statusPill.text = "Scheduled"
                holder.statusPill.setBackgroundResource(R.drawable.bg_pill_grey)
                holder.statusPill.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_grey))
            }
        }

        // ── Button State Logic ─────────────────────────────────────────────────
        val st = (item.status ?: "").trim().lowercase()
        val ct = (item.consultationStatus ?: "").trim().lowercase()
        val paymentStatus = (item.paymentStatus ?: item.paymentInfo?.status ?: "").trim().lowercase()
        val bookingStatus = (item.bookingStatus ?: "").trim().lowercase()

        val isCancelled = st == "cancelled"
        val isCompleted = st == "completed" || ct == "completed"
        val isMissed = st == "missed"

        // Relaxed isPaid: accept blank payment if booking status looks valid (matches ViewModel logic)
        val isPaid = paymentStatus == "paid" || paymentStatus == "success" ||
            (paymentStatus.isBlank() && (
                bookingStatus == "confirmed" || bookingStatus == "booked" ||
                bookingStatus == "scheduled" || bookingStatus.isBlank()
            ))
        val isConfirmed = bookingStatus == "confirmed" || bookingStatus == "booked" ||
            bookingStatus == "scheduled" || bookingStatus.isEmpty()

        // Show Join button for ALL valid upcoming appointments (cancelled/completed/missed are excluded)
        val shouldShowJoin = !isCancelled && !isCompleted && !isMissed && isPaid && isConfirmed

        // ── isToday: use the already-resolved localDateStr (accounts for utcTime) ─
        val isToday = localDateStr == todayDate

        // Join Consultation — visible for ALL valid upcoming appointments:
        //   • TODAY  → active (blue, fully clickable) → opens consultation waiting screen
        //   • FUTURE → greyed-out, non-clickable (patient sees it coming)
        holder.btnJoin.visibility = if (shouldShowJoin) View.VISIBLE else View.GONE

        if (shouldShowJoin) {
            // Always keep isEnabled=true so Material doesn't force its own disabled colours;
            // control touchability via isClickable.
            holder.btnJoin.isEnabled = true
            holder.btnJoin.text = "Join Consultation"

            // PER USER REQUEST: Enable "join consultation" flow for any date appointments
            holder.btnJoin.isClickable = true
            holder.btnJoin.alpha = 1.0f
            holder.btnJoin.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.context, R.color.primary_blue)
            )
            holder.btnJoin.setOnClickListener { onJoinClick(item) }
        }

        holder.btnDetails.visibility = View.VISIBLE
        holder.btnDetails.setOnClickListener { onDetailsClick(item) }
    }

    private fun convertUTCToLocal(utc: String?): String {
        if (utc.isNullOrEmpty()) return ""
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            input.timeZone = TimeZone.getTimeZone("UTC")
            val date = input.parse(utc)
            val output = SimpleDateFormat("hh:mm a", Locale.getDefault())
            output.timeZone = TimeZone.getDefault()
            output.format(date!!)
        } catch (e: Exception) {
            ""
        }
    }

    override fun getItemCount() = appointments.size

    fun updateList(newList: List<AppointmentResponse>) {
        appointments = newList
        notifyDataSetChanged()
    }
}
