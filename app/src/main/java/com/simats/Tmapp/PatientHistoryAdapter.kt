package com.simats.Tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.AppointmentResponse
import com.simats.Tmapp.api.RatingStatusResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class PatientHistoryAdapter(
    private var historyList: List<AppointmentResponse>,
    private val userId: Int,
    private val onPrescriptionClick: (AppointmentResponse) -> Unit
) : RecyclerView.Adapter<PatientHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivDoctorAvatar)
        val tvName: TextView = view.findViewById(R.id.tvDoctorName)
        val tvSpec: TextView = view.findViewById(R.id.tvSpecialization)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvViewPrescription: TextView = view.findViewById(R.id.tvViewPrescription)
        // Rating views
        val ratingContainer: LinearLayout = view.findViewById(R.id.ratingContainer)
        val layoutRateNow: LinearLayout = view.findViewById(R.id.layoutRateNow)
        val layoutAlreadyRated: LinearLayout = view.findViewById(R.id.layoutAlreadyRated)
        val ivStar1: ImageView = view.findViewById(R.id.ivStar1)
        val ivStar2: ImageView = view.findViewById(R.id.ivStar2)
        val ivStar3: ImageView = view.findViewById(R.id.ivStar3)
        val ivStar4: ImageView = view.findViewById(R.id.ivStar4)
        val ivStar5: ImageView = view.findViewById(R.id.ivStar5)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient_history_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]

        holder.tvName.text = if (item.doctorName.isNullOrEmpty() || item.doctorName == "null")
            "Doctor" else "Dr. ${item.doctorName}"
        holder.tvSpec.text = item.specialization ?: "Specialist"

        // Status Handling — unchanged from original
        val status = item.status ?: ""
        holder.tvStatus.text = status
        when {
            status.equals("Completed", ignoreCase = true) -> {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pill_completed)
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#10B981"))
                holder.tvViewPrescription.visibility = View.VISIBLE
            }
            status.equals("Missed", ignoreCase = true) -> {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pill_cancelled)
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                holder.tvViewPrescription.visibility = View.GONE
            }
            else -> {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pill_cancelled)
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#EF4444"))
                holder.tvViewPrescription.visibility = View.GONE
            }
        }

        // Date Display — unchanged from original
        val dateValue = item.date ?: ""
        val timeValue = when {
            !item.localTime.isNullOrEmpty() -> item.localTime
            !item.time.isNullOrEmpty() -> item.time
            else -> item.utcTime ?: ""
        }
        if (dateValue.isNotEmpty() && timeValue.isNotEmpty()) {
            val displayTime = if (timeValue.contains("T") && timeValue.contains("Z")) {
                convertUTCToLocal(timeValue)
            } else {
                timeValue
            }
            holder.tvDate.text = "$dateValue • $displayTime"
        } else if (dateValue.isNotEmpty()) {
            holder.tvDate.text = dateValue
        } else {
            holder.tvDate.text = ""
        }

        // Avatar — unchanged from original
        val baseUrl = ApiClient.BASE_URL.removeSuffix("/")
        val docImgUrl = if (!item.doctorImage.isNullOrEmpty()) {
            if (item.doctorImage.startsWith("http")) item.doctorImage
            else "$baseUrl${item.doctorImage}"
        } else "$baseUrl/api/profile/image/${item.doctorId}?role=doctor"
        val docName = item.doctorName ?: "Doctor"
        AvatarUtils.loadAvatar(holder.ivAvatar, docImgUrl, docName)

        holder.tvViewPrescription.setOnClickListener { onPrescriptionClick(item) }

        // Rating section — new addition
        bindRatingSection(holder, item, position)
    }

    // ------------------------------------------------------------
    // Rating section logic
    // ------------------------------------------------------------

    private fun bindRatingSection(holder: ViewHolder, item: AppointmentResponse, position: Int) {
        // Only show rating row for Completed appointments
        if (!item.status.equals("Completed", ignoreCase = true)) {
            holder.ratingContainer.visibility = View.GONE
            return
        }

        // Fetch rating status exactly ONCE per item — guard with loading flag
        if (item.ratingStatus == null && !item.isRatingStatusLoading) {
            item.isRatingStatusLoading = true
            holder.ratingContainer.visibility = View.GONE

            ApiClient.instance.getRatingStatus(item.id, userId)
                .enqueue(object : Callback<RatingStatusResponse> {
                    override fun onResponse(
                        call: Call<RatingStatusResponse>,
                        response: Response<RatingStatusResponse>
                    ) {
                        item.ratingStatus = if (response.isSuccessful && response.body() != null) {
                            response.body()!!
                        } else {
                            RatingStatusResponse(canRate = false, alreadyRated = false)
                        }
                        item.isRatingStatusLoading = false
                        notifyItemChanged(position)
                    }

                    override fun onFailure(call: Call<RatingStatusResponse>, t: Throwable) {
                        item.ratingStatus = RatingStatusResponse(canRate = false, alreadyRated = false)
                        item.isRatingStatusLoading = false
                        notifyItemChanged(position)
                    }
                })
            return
        }

        val ratingStatus = item.ratingStatus ?: run {
            holder.ratingContainer.visibility = View.GONE
            return
        }

        when {
            ratingStatus.alreadyRated -> {
                holder.ratingContainer.visibility = View.VISIBLE
                holder.layoutRateNow.visibility = View.GONE
                holder.layoutAlreadyRated.visibility = View.VISIBLE
                renderStars(
                    listOf(holder.ivStar1, holder.ivStar2, holder.ivStar3, holder.ivStar4, holder.ivStar5),
                    ratingStatus.ratingGiven ?: 0
                )
            }
            ratingStatus.canRate -> {
                holder.ratingContainer.visibility = View.VISIBLE
                holder.layoutRateNow.visibility = View.VISIBLE
                holder.layoutAlreadyRated.visibility = View.GONE

                holder.layoutRateNow.setOnClickListener {
                    val fm = (holder.itemView.context as? AppCompatActivity)?.supportFragmentManager
                    fm?.let { fragmentManager ->
                        val sheet = RateDoctorBottomSheetFragment.newInstance(
                            appointmentId = item.id,
                            doctorName = item.doctorName ?: "Doctor",
                            userId = userId
                        ) { ratingGiven, reviewGiven ->
                            // Update only this item immediately — do NOT reload all
                            item.ratingStatus = RatingStatusResponse(
                                canRate = false,
                                alreadyRated = true,
                                ratingGiven = ratingGiven,
                                reviewGiven = reviewGiven
                            )
                            notifyItemChanged(position)
                        }
                        sheet.show(fragmentManager, "RateDoctorSheet")
                    }
                }
            }
            else -> {
                holder.ratingContainer.visibility = View.GONE
            }
        }
    }

    private fun renderStars(starViews: List<ImageView>, filled: Int) {
        starViews.forEachIndexed { index, imageView ->
            imageView.setImageResource(
                if (index < filled) R.drawable.ic_star_filled
                else R.drawable.ic_star_outline
            )
        }
    }

    // ------------------------------------------------------------
    // List management
    // ------------------------------------------------------------

    override fun getItemCount() = historyList.size

    fun updateList(newList: List<AppointmentResponse>) {
        historyList = newList
        notifyDataSetChanged()
    }

    // Preserve original UTC→local conversion unchanged
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
}
