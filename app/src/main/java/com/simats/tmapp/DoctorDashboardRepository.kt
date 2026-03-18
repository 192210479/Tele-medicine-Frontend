package com.simats.tmapp

import com.simats.tmapp.api.AppointmentResponse
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorDashboardRepository {
    private val apiService = ApiClient.instance

    fun getDoctorAppointments(userId: Int, role: String, onResult: (List<AppointmentResponse>?, String?) -> Unit) {
        apiService.getMyAppointments(userId, role).enqueue(object : Callback<List<AppointmentResponse>> {
            override fun onResponse(call: Call<List<AppointmentResponse>>, response: Response<List<AppointmentResponse>>) {
                android.util.Log.d("DOCTOR_APPOINTMENTS_API", response.body().toString())
                if (response.isSuccessful) {
                    onResult(response.body(), null)
                } else {
                    onResult(null, "Failed to fetch appointments")
                }
            }

            override fun onFailure(call: Call<List<AppointmentResponse>>, t: Throwable) {
                onResult(null, t.message)
            }
        })
    }
    fun getDoctorSlots(doctorId: Int, onResult: (List<com.simats.tmapp.api.AvailabilityResponse>?, String?) -> Unit) {
        apiService.getDoctorAvailability(doctorId).enqueue(object : Callback<List<com.simats.tmapp.api.AvailabilityResponse>> {
            override fun onResponse(call: Call<List<com.simats.tmapp.api.AvailabilityResponse>>, response: Response<List<com.simats.tmapp.api.AvailabilityResponse>>) {
                if (response.isSuccessful) onResult(response.body(), null)
                else onResult(null, "Failed to fetch slots")
            }
            override fun onFailure(call: Call<List<com.simats.tmapp.api.AvailabilityResponse>>, t: Throwable) {
                onResult(null, t.message)
            }
        })
    }

    fun deleteSlot(slotId: Int, userId: Int, role: String, onResult: (Boolean, String?) -> Unit) {
        apiService.deleteAvailability(slotId, userId, role).enqueue(object : Callback<com.simats.tmapp.api.GenericResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.GenericResponse>, response: Response<com.simats.tmapp.api.GenericResponse>) {
                if (response.isSuccessful) onResult(true, null)
                else onResult(false, "Failed to delete slot")
            }
            override fun onFailure(call: Call<com.simats.tmapp.api.GenericResponse>, t: Throwable) {
                onResult(false, t.message)
            }
        })
    }

    fun cleanupSlots(userId: Int, role: String) {
        val body = mapOf("user_id" to userId, "role" to role)
        apiService.cleanupSlots(body).enqueue(object : Callback<com.simats.tmapp.api.GenericResponse> {
            override fun onResponse(call: Call<com.simats.tmapp.api.GenericResponse>, response: Response<com.simats.tmapp.api.GenericResponse>) {}
            override fun onFailure(call: Call<com.simats.tmapp.api.GenericResponse>, t: Throwable) {}
        })
    }
}
