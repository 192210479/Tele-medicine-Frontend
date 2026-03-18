package com.simats.tmapp.api

data class BookAppointmentRequest(
    val user_id: Int,
    val role: String,
    val availability_id: Int
)
