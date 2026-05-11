package com.simats.Tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.DoctorResponse
import com.simats.Tmapp.api.ApiClient

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

        val name = doc.name?.trim()

        holder.tvDocName.text = if (!name.isNullOrEmpty()) {
            "Dr. $name"
        } else {
            "Dr. #${doc.id}"
        }

        holder.tvDocSpec.text = doc.specialization ?: ""
        holder.tvExp.text = "${doc.experience ?: 0} Yrs Exp"
        holder.tvFee.text = "₹${doc.fee ?: 0.0}"

        val reviews = doc.reviewsCount ?: 0
        val effectiveRating = if (reviews > 0) doc.rating ?: 0.0f else 0.0f
        val ratingStr = "★ ${"%.1f".format(effectiveRating)}"
        val reviewStr = if (reviews > 0) "$reviews Patient Reviews" else "No reviews yet"
        holder.tvReviews.text = "$ratingStr  •  $reviewStr"

        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")

        val finalDoctorImage = when {
            !doc.profileImage.isNullOrEmpty() && doc.profileImage!!.startsWith("http") -> doc.profileImage
            !doc.profileImage.isNullOrEmpty() && doc.profileImage!!.startsWith("/") -> "$baseUrl${doc.profileImage}"
            else -> "$baseUrl/api/profile/image/${doc.id}?role=doctor"
        }

        AvatarUtils.loadAvatar(
            imageView = holder.ivDoc,
            imageUrl = finalDoctorImage,
            name = doc.name ?: "Doctor"
        )

        holder.btnBook.setOnClickListener { onBookClick(doc) }
        holder.cardContainer.setOnClickListener { onCardClick(doc) }
    }

    override fun getItemCount() = doctors.size

    fun updateDoctors(newDoctors: List<DoctorResponse>) {
        doctors = newDoctors
        notifyDataSetChanged()
    }
}
