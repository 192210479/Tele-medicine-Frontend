package com.simats.Tmapp.api

import com.google.gson.annotations.SerializedName

data class SubmitRatingRequest(
    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("role")
    val role: String = "patient",

    @SerializedName("rating")
    val rating: Int,

    @SerializedName("review")
    val review: String? = null
)
