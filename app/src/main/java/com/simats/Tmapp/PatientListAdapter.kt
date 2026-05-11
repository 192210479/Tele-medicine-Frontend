package com.simats.Tmapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.Patient
import java.util.Locale

class PatientListAdapter(
    private var items: List<Patient>,
    private val onAction: (Patient, String) -> Unit
) : RecyclerView.Adapter<PatientListAdapter.PatientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_list_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Patient>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class PatientViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvInitials: TextView = view.findViewById(R.id.tvInitials)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvInfo: TextView = view.findViewById(R.id.tvInfo)
        private val tvStatusChip: TextView = view.findViewById(R.id.tvStatusChip)
        private val ivMore: ImageView = view.findViewById(R.id.ivMore)
        private val ivPatientAvatar: ImageView? = view.findViewById(R.id.ivPatientAvatar)

        fun bind(patient: Patient) {
            val rawName = patient.fullName?.trim().orEmpty()
            val name = if (rawName.isNotBlank()) rawName else "Unknown Patient"
            val isUnknown = name.equals("Unknown Patient", ignoreCase = true)

            tvName.text = name

            val safeAge = patient.age ?: 0
            val safeLastVisit = patient.lastAppointment?.takeIf { it.isNotBlank() } ?: "No visits yet"
            tvInfo.text = "Age: $safeAge • Last Visit: $safeLastVisit"

            val initials = if (isUnknown) {
                "PA"
            } else {
                name.split(" ")
                    .filter { it.isNotBlank() }
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .joinToString("")
                    .take(2)
            }
            tvInitials.text = if (initials.isNotBlank()) initials else "PA"

            if (ivPatientAvatar != null) {
                val baseUrl = ApiClient.BASE_URL.removeSuffix("/")

                val finalPatientImage = when {
                    !patient.profileImage.isNullOrEmpty() && patient.profileImage!!.startsWith("http") -> patient.profileImage
                    !patient.profileImage.isNullOrEmpty() && patient.profileImage!!.startsWith("/") -> "$baseUrl${patient.profileImage}"
                    patient.id > 0 -> "$baseUrl/api/profile/image/${patient.id}?role=patient"
                    else -> null
                }

                AvatarUtils.loadAvatar(
                    imageView = ivPatientAvatar,
                    imageUrl = finalPatientImage,
                    name = patient.fullName ?: "Patient"
                )

                tvInitials.visibility = View.GONE
            } else {
                tvInitials.visibility = View.VISIBLE
            }

            // HIDE USELESS 3-DOT MENU
            ivMore.visibility = View.GONE
            ivMore.setOnClickListener(null)

            // Better patient status mapping
            val safeStatus = patient.status?.trim()?.lowercase(Locale.getDefault()) ?: "active"

            when (safeStatus) {
                "new" -> {
                    tvStatusChip.visibility = View.VISIBLE
                    tvStatusChip.text = "New"
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_orange)
                    tvStatusChip.setTextColor(Color.parseColor("#F59E0B"))
                }

                "inactive" -> {
                    tvStatusChip.visibility = View.VISIBLE
                    tvStatusChip.text = "Inactive"
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_red)
                    tvStatusChip.setTextColor(Color.parseColor("#EF4444"))
                }

                "active" -> {
                    tvStatusChip.visibility = View.VISIBLE
                    tvStatusChip.text = "Active"
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_green)
                    tvStatusChip.setTextColor(Color.parseColor("#10B981"))
                }

                else -> {
                    tvStatusChip.visibility = View.VISIBLE
                    tvStatusChip.text = "Active"
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_green)
                    tvStatusChip.setTextColor(Color.parseColor("#10B981"))
                }
            }

            itemView.setOnClickListener { onAction(patient, "click") }
        }
    }
}