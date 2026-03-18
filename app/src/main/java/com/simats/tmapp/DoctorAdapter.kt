package com.simats.tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.simats.tmapp.api.DoctorResponse
import com.simats.tmapp.api.ApiClient

class DoctorAdapter(
    private var doctors: List<DoctorResponse>,
    private val onBookClick: (DoctorResponse) -> Unit,
    private val onCardClick: (DoctorResponse) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    class DoctorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivDoc: ImageView = view.findViewById(R.id.ivDoc)
        val tvDocName: TextView = view.findViewById(R.id.tvDocName)
        val tvDocSpec: TextView = view.findViewById(R.id.tvDocSpec)
        val tvExp: TextView = view.findViewById(R.id.tvExperience)
        val tvFee: TextView = view.findViewById(R.id.tvFee)
        val tvReviews: TextView = view.findViewById(R.id.tvReviews)
        val btnBook: MaterialButton = view.findViewById(R.id.btnBook)
        val cardContainer: View = view.findViewById(R.id.cardContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_doctor, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        val doc = doctors[position]
        if (doc.name.isNotEmpty()) {
            holder.tvDocName.text = "Dr. ${doc.name}"
        } else {
            holder.tvDocName.text = "Unknown Doctor"
        }
        holder.tvDocSpec.text = doc.specialization
        holder.tvExp.text = "${doc.experience ?: 0} Yrs Exp"
        holder.tvFee.text = "$${doc.fee ?: 0.0}"
        holder.tvReviews.text = "(124 reviews)"

        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        val avatarUrl = "$baseUrl/api/profile/image/${doc.id}"
        Glide.with(holder.itemView.context)
            .load(avatarUrl)
            .placeholder(R.drawable.bg_image_mock)
            .error(R.drawable.bg_image_mock)
            .circleCrop()
            .into(holder.ivDoc)

        holder.btnBook.setOnClickListener { onBookClick(doc) }
        holder.cardContainer.setOnClickListener { onCardClick(doc) }
    }

    override fun getItemCount() = doctors.size

    fun updateDoctors(newDoctors: List<DoctorResponse>) {
        doctors = newDoctors
        notifyDataSetChanged()
    }
}
