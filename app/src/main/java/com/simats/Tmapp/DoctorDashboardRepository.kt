package com.simats.Tmapp

import com.simats.Tmapp.api.AppointmentResponse
import com.simats.Tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorDashboardRepository {
    private val apiService = ApiClient.instance

    fun getDoctorAppointments(userId: Int, role: String, onResult: (List<AppointmentResponse>?, String?) -> Unit) {
        // Pass null for status — fetch ALL appointments and filter client-side.
        // Previously passed default "Upcoming" which the backend never matches
        // because it stores "Scheduled", not "Upcoming" — causing blank screens.
        apiService.getMyAppointments(userId, role, null).enqueue(object : Callback<List<AppointmentResponse>> {
            override fun onResponse(call: Call<List<AppointmentResponse>>, response: Response<List<AppointmentResponse>>) {
                android.util.Log.d("DOCTOR_APPOINTMENTS_API", "Status: ${response.code()}, Body: ${response.body()}")
                if (response.isSuccessful) {
                    onResult(response.body() ?: emptyList(), null)
                } else {
                    onResult(null, "Failed to fetch appointments: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<List<AppointmentResponse>>, t: Throwable) {
                android.util.Log.e("DOCTOR_APPOINTMENTS_API", "Network failure: ${t.message}")
                onResult(null, t.message)
            }
        })
    }
    fun getDoctorSlots(doctorId: Int, onResult: (List<com.simats.Tmapp.api.AvailabilityResponse>?, String?) -> Unit) {
        apiService.getDoctorAvailability(doctorId).enqueue(object : Callback<List<com.simats.Tmapp.api.AvailabilityResponse>> {
            override fun onResponse(call: Call<List<com.simats.Tmapp.api.AvailabilityResponse>>, response: Response<List<com.simats.Tmapp.api.AvailabilityResponse>>) {
                if (response.isSuccessful) onResult(response.body(), null)
                else onResult(null, "Failed to fetch slots")
            }
            override fun onFailure(call: Call<List<com.simats.Tmapp.api.AvailabilityResponse>>, t: Throwable) {
                onResult(null, t.message)
            }
        })
    }

    fun deleteSlot(slotId: Int, userId: Int, role: String, onResult: (Boolean, String?) -> Unit) {
        apiService.deleteAvailability(slotId, userId, role).enqueue(object : Callback<com.simats.Tmapp.api.GenericResponse> {
            override fun onResponse(call: Call<com.simats.Tmapp.api.GenericResponse>, response: Response<com.simats.Tmapp.api.GenericResponse>) {
                if (response.isSuccessful) onResult(true, null)
                else onResult(false, "Failed to delete slot")
            }
            override fun onFailure(call: Call<com.simats.Tmapp.api.GenericResponse>, t: Throwable) {
                onResult(false, t.message)
            }
        })
    }

    fun cleanupSlots(userId: Int, role: String) {
        val body = mapOf("user_id" to userId, "role" to role)
        apiService.cleanupSlots(body).enqueue(object : Callback<com.simats.Tmapp.api.GenericResponse> {
            override fun onResponse(call: Call<com.simats.Tmapp.api.GenericResponse>, response: Response<com.simats.Tmapp.api.GenericResponse>) {}
            override fun onFailure(call: Call<com.simats.Tmapp.api.GenericResponse>, t: Throwable) {}
        })
    }
}
