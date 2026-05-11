package com.simats.Tmapp

import com.simats.Tmapp.api.GenericResponse
import com.simats.Tmapp.api.NotificationResponse
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.ApiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationRepository {
    private val apiService = ApiClient.instance

    suspend fun getNotifications(userId: Int, role: String, type: String? = null): List<NotificationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getNotifications(userId, role, type).execute()
                if (response.isSuccessful) {
                    val wrapper = response.body()
                    Log.d("NotificationRepo", "Received wrapper: $wrapper")
                    wrapper?.notifications ?: emptyList()
                } else {
                    Log.e("NotificationRepo", "Error: ${response.code()} - ${response.errorBody()?.string()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("NotificationRepo", "Exception loading notifications", e)
                emptyList()
            }
        }
    }

    suspend fun getUnreadCount(userId: Int, role: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getNotifications(userId, role, null).execute()
                if (response.isSuccessful) {
                    response.body()?.unreadCount ?: 0
                } else {
                    0
                }
            } catch (e: Exception) {
                Log.e("NotificationRepo", "Exception getting unread count", e)
                0
            }
        }
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
