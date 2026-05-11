package com.simats.Tmapp

import android.util.Log
import androidx.lifecycle.ViewModel
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.AppointmentResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UpcomingAppointmentsViewModel : ViewModel() {

    private val _appointments = MutableStateFlow<List<AppointmentResponse>>(emptyList())
    val appointments: StateFlow<List<AppointmentResponse>> = _appointments

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchAppointments(userId: Int, role: String) {
        _isLoading.value = true

        // Ask backend specifically for upcoming appointments only
        ApiClient.instance.getMyAppointments(userId, role, "upcoming")
            .enqueue(object : Callback<List<AppointmentResponse>> {
                override fun onResponse(
                    call: Call<List<AppointmentResponse>>,
                    response: Response<List<AppointmentResponse>>
                ) {
                    _isLoading.value = false

                    if (response.isSuccessful) {
                        val all = response.body() ?: emptyList()
                        Log.d("UPCOMING_VM", "Upcoming fetched from backend: ${all.size}")

                        val upcoming = all.filter { appt ->
                            val status = appt.status?.trim()?.lowercase() ?: ""
                            val consultationStatus = appt.consultationStatus?.trim()?.lowercase() ?: ""
                            val paymentStatus = (appt.paymentStatus ?: appt.paymentInfo?.status ?: "").trim().lowercase()
                            val bookingStatus = (appt.bookingStatus ?: "").trim().lowercase()

                            // Hard block only truly terminal appointments
                            val isTerminal =
                                status == "completed" ||
                                status == "cancelled" ||
                                status == "missed" ||
                                consultationStatus == "completed"

                            if (isTerminal) return@filter false

                            // Paid-only rule for patient side
                            // But allow blank payment status for backward compatibility ONLY if booking looks valid
                            val isPaid =
                                paymentStatus == "paid" ||
                                paymentStatus == "success" ||
                                (paymentStatus.isBlank() && (
                                    bookingStatus == "confirmed" ||
                                    bookingStatus == "booked" ||
                                    bookingStatus == "scheduled" ||
                                    bookingStatus.isBlank()
                                ))

                            // Accept more valid booking states
                            val isConfirmedOrBooked =
                                bookingStatus == "confirmed" ||
                                bookingStatus == "booked" ||
                                bookingStatus == "scheduled" ||
                                bookingStatus.isBlank()

                            if (!isPaid || !isConfirmedOrBooked) return@filter false

                            // For Upcoming Appointments screen:
                            // do NOT restrict by current time/date window.
                            // If appointment is valid and not terminal, keep it visible.
                            true
                        }.sortedWith(
                            compareBy<AppointmentResponse> { it.date ?: "" }
                                .thenBy { it.localTime ?: it.time ?: "" }
                        )

                        Log.d("UPCOMING_VM", "Upcoming final filtered: ${upcoming.size}")
                        _appointments.value = upcoming
                    } else {
                        Log.e("UPCOMING_VM", "Error: ${response.code()} ${response.errorBody()?.string()}")
                        _error.value = "Error loading appointments"
                    }
                }

                override fun onFailure(call: Call<List<AppointmentResponse>>, t: Throwable) {
                    _isLoading.value = false
                    Log.e("UPCOMING_VM", "Network failure: ${t.message}")
                    _error.value = "Network error. Please check your connection."
                }
            })
    }

    fun clearError() {
        _error.value = null
    }
}
