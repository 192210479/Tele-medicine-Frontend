package com.simats.tmapp.api

data class BookingResponse(
    val appointment_id: Int,
    val doctor_name: String?,
    val specialization: String?,
    val date: String?,
    val time: String?
)
