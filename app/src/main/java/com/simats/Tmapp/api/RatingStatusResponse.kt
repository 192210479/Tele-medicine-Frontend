package com.simats.Tmapp.api

import com.google.gson.annotations.SerializedName

data class RatingStatusResponse(
    @SerializedName("can_rate")
    val canRate: Boolean = false,

    @SerializedName("already_rated")
    val alreadyRated: Boolean = false,

    @SerializedName("rating_given")
    val ratingGiven: Int? = null,

    @SerializedName("review_given")
    val reviewGiven: String? = null,

    @SerializedName("reason")
    val reason: String? = null
)
