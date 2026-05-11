package com.simats.Tmapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.Doctor
import java.util.Locale

class DoctorListAdapter(
    private var items: List<Doctor>,
    private val onAction: (Doctor, String) -> Unit
) : RecyclerView.Adapter<DoctorListAdapter.DoctorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_list_doctor, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Doctor>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class DoctorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvInitials: TextView = view.findViewById(R.id.tvInitials)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvInfo: TextView = view.findViewById(R.id.tvInfo)
        private val tvStatusChip: TextView = view.findViewById(R.id.tvStatusChip)
        private val ivMore: ImageView = view.findViewById(R.id.ivMore)

        // IMPORTANT:
        // If your XML has this ImageView, avatar will show.
        // If not, code will safely fall back to initials.
        private val ivDoctorAvatar: ImageView? = view.findViewById(R.id.ivDoctorAvatar)

        fun bind(doctor: Doctor) {
            val rawName = doctor.name?.trim().orEmpty()
            val name = if (rawName.isNotBlank()) rawName else "Unknown Doctor"
            val isUnknown = name.equals("Unknown Doctor", ignoreCase = true)

            tvName.text = if (isUnknown) {
                name
            } else {
                if (name.startsWith("Dr.", ignoreCase = true)) name else "Dr. $name"
            }

            val specialization = doctor.specialization?.takeIf { it.isNotBlank() } ?: "General"
            val expYears = doctor.experience ?: doctor.experienceYears ?: 0
            tvInfo.text = "$specialization • ${expYears} Years Exp"

            val initials = if (isUnknown) {
                "DR"
            } else {
                name.split(" ")
                    .filter { it.isNotBlank() && !it.equals("Dr.", ignoreCase = true) }
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .joinToString("")
                    .take(2)
            }
            tvInitials.text = if (initials.isNotBlank()) initials else "DR"

            // ===== DOCTOR AVATAR LOAD FIX =====
            if (ivDoctorAvatar != null) {
                val baseUrl = ApiClient.BASE_URL.removeSuffix("/")

                val finalDoctorImage = when {
                    !doctor.profileImage.isNullOrEmpty() && doctor.profileImage!!.startsWith("http") -> doctor.profileImage
                    !doctor.profileImage.isNullOrEmpty() && doctor.profileImage!!.startsWith("/") -> "$baseUrl${doctor.profileImage}"
                    doctor.id != 0 -> "$baseUrl/api/profile/image/${doctor.id}?role=doctor"
                    else -> null
                }

                AvatarUtils.loadAvatar(
                    imageView = ivDoctorAvatar,
                    imageUrl = finalDoctorImage,
                    name = doctor.name ?: "Doctor"
                )

                tvInitials.visibility = View.GONE
                ivDoctorAvatar.visibility = View.VISIBLE
            } else {
                tvInitials.visibility = View.VISIBLE
            }
            // ===== END FIX =====

            // HIDE USELESS 3-DOT MENU
            ivMore.visibility = View.GONE
            ivMore.setOnClickListener(null)

            // Better admin status mapping
            val safeStatus = doctor.status?.trim()?.lowercase(Locale.getDefault()) ?: "available"

            when (safeStatus) {
                "available", "active", "approved" -> {
                    tvStatusChip.visibility = View.VISIBLE
                    tvStatusChip.text = "Available"
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_green)
                    tvStatusChip.setTextColor(Color.parseColor("#10B981"))
                }

                "unavailable", "inactive", "disabled" -> {
                    tvStatusChip.visibility = View.VISIBLE
                    tvStatusChip.text = "Unavailable"
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_orange)
                    tvStatusChip.setTextColor(Color.parseColor("#F59E0B"))
                }

                "incomplete" -> {
                    tvStatusChip.visibility = View.VISIBLE
                    tvStatusChip.text = "Incomplete"
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_red)
                    tvStatusChip.setTextColor(Color.parseColor("#EF4444"))
                }

                else -> {
                    tvStatusChip.visibility = View.VISIBLE
                    tvStatusChip.text = "Available"
                    tvStatusChip.setBackgroundResource(R.drawable.bg_badge_green)
                    tvStatusChip.setTextColor(Color.parseColor("#10B981"))
                }
            }

            itemView.setOnClickListener { onAction(doctor, "click") }
        }
    }
}