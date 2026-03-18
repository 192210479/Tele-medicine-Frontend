package com.simats.tmapp

import com.simats.tmapp.api.TelemedicineAPI
import com.simats.tmapp.api.GenericResponse
import com.simats.tmapp.api.NotificationResponse
import com.simats.tmapp.api.ApiClient
import com.simats.tmapp.api.ApiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationRepository {
    private val apiService = ApiClient.instance

    fun getNotifications(userId: Int, role: String, callback: (List<NotificationResponse>?) -> Unit) {
        apiService.getNotifications(userId, role).enqueue(object : Callback<List<NotificationResponse>> {
            override fun onResponse(call: Call<List<NotificationResponse>>, response: Response<List<NotificationResponse>>) {
                if (response.isSuccessful) {
                    callback(response.body())
                } else {
                    callback(null)
                }
            }
            override fun onFailure(call: Call<List<NotificationResponse>>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun markAsRead(notificationId: Int, callback: (Boolean) -> Unit) {
        apiService.markNotificationRead(notificationId).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                callback(response.isSuccessful)
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                callback(false)
            }
        })
    }

    fun markAllAsRead(userId: Int, role: String, callback: (Boolean) -> Unit) {
        val body = mapOf("user_id" to userId, "role" to role)
        apiService.markAllNotificationsRead(body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                callback(response.isSuccessful)
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                callback(false)
            }
        })
    }
}
