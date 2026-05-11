package com.simats.Tmapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.SubmitRatingRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RateDoctorBottomSheetFragment : BottomSheetDialogFragment() {

    private var appointmentId: Int = -1
    private var doctorName: String = "Doctor"
    private var userId: Int = -1
    private var onRatingSubmitted: ((ratingGiven: Int, reviewGiven: String?) -> Unit)? = null

    private var selectedRating: Int = 0
    private lateinit var starViews: List<ImageView>

    companion object {
        fun newInstance(
            appointmentId: Int,
            doctorName: String,
            userId: Int,
            onRatingSubmitted: (ratingGiven: Int, reviewGiven: String?) -> Unit
        ): RateDoctorBottomSheetFragment {
            return RateDoctorBottomSheetFragment().apply {
                this.appointmentId = appointmentId
                this.doctorName = doctorName
                this.userId = userId
                this.onRatingSubmitted = onRatingSubmitted
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_rate_doctor, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvRateTitle)
        val etReview = view.findViewById<TextInputEditText>(R.id.etReview)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btnSubmitRating)
        val tvSkip = view.findViewById<TextView>(R.id.tvSkipRating)

        tvTitle.text = "Rate Dr. $doctorName"

        starViews = listOf(
            view.findViewById(R.id.ivRateStar1),
            view.findViewById(R.id.ivRateStar2),
            view.findViewById(R.id.ivRateStar3),
            view.findViewById(R.id.ivRateStar4),
            view.findViewById(R.id.ivRateStar5)
        )

        // Star tap logic: tap star N → fill 1..N, outline N+1..5
        starViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                selectedRating = index + 1
                renderStars(selectedRating)
            }
        }

        btnSubmit.setOnClickListener {
            if (selectedRating == 0) {
                Toast.makeText(requireContext(), "Please select a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val reviewText = etReview.text?.toString()?.trim().takeIf { !it.isNullOrEmpty() }
            submitRating(selectedRating, reviewText, btnSubmit)
        }

        tvSkip.setOnClickListener { dismiss() }
    }

    private fun renderStars(filled: Int) {
        starViews.forEachIndexed { index, imageView ->
            imageView.setImageResource(
                if (index < filled) R.drawable.ic_star_filled
                else R.drawable.ic_star_outline
            )
        }
    }

    private fun submitRating(rating: Int, review: String?, btnSubmit: MaterialButton) {
        if (appointmentId == -1 || userId == -1) {
            Toast.makeText(requireContext(), "Invalid appointment", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmit.isEnabled = false
        btnSubmit.text = "Submitting..."

        val request = SubmitRatingRequest(
            userId = userId,
            role = "patient",
            rating = rating,
            review = review
        )

        ApiClient.instance.submitRating(appointmentId, request)
            .enqueue(object : Callback<com.google.gson.JsonObject> {
                override fun onResponse(
                    call: Call<com.google.gson.JsonObject>,
                    response: Response<com.google.gson.JsonObject>
                ) {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Submit Rating"

                    when (response.code()) {
                        201 -> {
                            // Notify adapter to update only this card
                            onRatingSubmitted?.invoke(rating, review)
                            dismiss()
                        }
                        409 -> {
                            Toast.makeText(requireContext(), "Already rated", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                        else -> {
                            val errorMsg = try {
                                response.errorBody()?.string()
                                    ?.let { com.google.gson.JsonParser.parseString(it).asJsonObject }
                                    ?.get("message")?.asString
                                    ?: "Failed to submit rating"
                            } catch (e: Exception) {
                                "Failed to submit rating"
                            }
                            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<com.google.gson.JsonObject>, t: Throwable) {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Submit Rating"
                    Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
