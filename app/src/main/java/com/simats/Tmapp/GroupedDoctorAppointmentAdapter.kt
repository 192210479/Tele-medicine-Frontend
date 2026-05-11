package com.simats.Tmapp

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.AppointmentResponse

sealed class AppointmentListItem {
    data class Header(val date: String) : AppointmentListItem()
    data class Item(val appointment: AppointmentResponse) : AppointmentListItem()
}

class GroupedDoctorAppointmentAdapter(
    private var items: List<AppointmentListItem> = emptyList(),
    private val onStartCallClick: (AppointmentResponse) -> Unit,
    private val onViewDetailsClick: (AppointmentResponse) -> Unit,
    private val onActionClick: (AppointmentResponse, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AppointmentListItem.Header -> TYPE_HEADER
            is AppointmentListItem.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_date_header, parent, false))
            else -> ItemViewHolder(inflater.inflate(R.layout.item_doctor_schedule, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is HeaderViewHolder && item is AppointmentListItem.Header) {
            holder.bind(item.date)
        } else if (holder is ItemViewHolder && item is AppointmentListItem.Item) {
            holder.bind(item.appointment)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<AppointmentListItem>) {
        items = newList
        notifyDataSetChanged()
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvHeader: TextView = view.findViewById(R.id.tvDateHeader)

        fun bind(dateStr: String) {
            tvHeader.text = dateStr.uppercase()
        }
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivPatientAvatar: ImageView? = view.findViewById(R.id.ivPatientAvatar)
        private val tvPatientName: TextView = view.findViewById(R.id.tvPatientName)
        private val tvType: TextView = view.findViewById(R.id.tvConsultationType)
        private val tvDateTime: TextView = view.findViewById(R.id.tvApptDateTime)
        private val tvStatus: TextView = view.findViewById(R.id.tvStatusBadge)
        private val btnStart: com.google.android.material.button.MaterialButton =
            view.findViewById(R.id.btnStartConsultation)
        private val btnAction: com.google.android.material.button.MaterialButton =
            view.findViewById(R.id.btnViewReports)

        fun bind(appt: AppointmentResponse) {
            val date = DoctorAppointmentHelper.cleanText(appt.date)
            val time = DoctorAppointmentHelper.cleanText(appt.localTime ?: appt.time)
            tvDateTime.text = if (time.isNotEmpty()) "$date • $time" else date

            tvPatientName.text = DoctorAppointmentHelper.cleanText(appt.patientName, "Patient")
            tvType.text = DoctorAppointmentHelper.cleanText(appt.specialization, "Video Call")

            val baseUrl = ApiClient.BASE_URL.removeSuffix("/")

            val finalPatientImage = when {
                !appt.patientImage.isNullOrEmpty() && appt.patientImage!!.startsWith("http") -> appt.patientImage
                !appt.patientImage.isNullOrEmpty() && appt.patientImage!!.startsWith("/") -> "$baseUrl${appt.patientImage}"
                appt.patientId != null -> "$baseUrl/api/profile/image/${appt.patientId}?role=patient"
                else -> null
            }

            ivPatientAvatar?.let {
                AvatarUtils.loadAvatar(
                    imageView = it,
                    imageUrl = finalPatientImage,
                    name = appt.patientName ?: "Patient"
                )
            }

            val statusEnum = DoctorAppointmentHelper.getStatus(appt)
            val consStatus = appt.consultationStatus?.trim()?.lowercase() ?: ""
            val rawStatus = appt.status?.trim()?.lowercase() ?: ""

            // ---------- STATUS BADGE ----------
            when {
                consStatus == "waiting" || consStatus == "ready" -> {
                    tvStatus.text = "READY"
                    tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                    tvStatus.setTextColor(Color.parseColor("#16A34A"))
                }

                statusEnum == DoctorAppointmentHelper.AppointmentStatus.COMPLETED -> {
                    tvStatus.text = "COMPLETED"
                    tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                    tvStatus.setTextColor(Color.parseColor("#16A34A"))
                }

                statusEnum == DoctorAppointmentHelper.AppointmentStatus.CANCELLED -> {
                    tvStatus.text = "CANCELLED"
                    tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                    tvStatus.setTextColor(Color.parseColor("#DC2626"))
                }

                statusEnum == DoctorAppointmentHelper.AppointmentStatus.MISSED -> {
                    tvStatus.text = "MISSED"
                    tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                    tvStatus.setTextColor(Color.parseColor("#D97706"))
                }

                else -> {
                    tvStatus.text = "UPCOMING"
                    tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DBEAFE"))
                    tvStatus.setTextColor(Color.parseColor("#2563EB"))
                }
            }

            // ---------- START BUTTON ----------
            val showStartButton =
                (statusEnum == DoctorAppointmentHelper.AppointmentStatus.UPCOMING) &&
                rawStatus != "completed" &&
                consStatus != "completed" &&
                rawStatus != "cancelled"

            btnStart.visibility = if (showStartButton) View.VISIBLE else View.GONE

            // ---------- ACTION BUTTON ----------
            btnAction.visibility = View.VISIBLE

            when (statusEnum) {
                DoctorAppointmentHelper.AppointmentStatus.UPCOMING -> {
                    btnAction.text = "Details"
                    btnAction.setOnClickListener { onViewDetailsClick(appt) }
                }

                DoctorAppointmentHelper.AppointmentStatus.MISSED -> {
                    btnAction.text = "Mark Missed"
                    btnAction.setOnClickListener { onActionClick(appt, "missed") }
                }

                DoctorAppointmentHelper.AppointmentStatus.COMPLETED -> {
                    btnAction.text = "View Plans"
                    btnAction.setOnClickListener { onViewDetailsClick(appt) }
                }

                DoctorAppointmentHelper.AppointmentStatus.CANCELLED -> {
                    btnAction.text = "View Info"
                    btnAction.setOnClickListener { onActionClick(appt, "info") }
                }
            }

            btnStart.setOnClickListener { onStartCallClick(appt) }
            itemView.setOnClickListener { onViewDetailsClick(appt) }
        }
    }
}
