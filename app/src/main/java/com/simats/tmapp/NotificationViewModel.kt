package com.simats.tmapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.simats.tmapp.api.NotificationResponse
import io.socket.client.Socket
import org.json.JSONObject

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NotificationRepository()
    private val sessionManager = SessionManager.getInstance(application)

    val notifications = MutableLiveData<List<NotificationResponse>>()
    val unreadCount = MutableLiveData<Int>()
    val error = MutableLiveData<String>()

    init {
        setupSocketListener()
    }

    private fun setupSocketListener() {
        SocketService.socket?.on("notification") { args ->
            val data = args.getOrNull(0) as? JSONObject
            if (data != null) {
                try {
                    val id = data.optInt("id")
                    val type = data.optString("type")
                    val title = data.optString("title")
                    val desc = data.optString("description")
                    val createdAt = data.optString("created_at")
                    val isRead = data.optBoolean("is_read", false)
                    val appointmentId = if (data.has("appointment_id")) data.optInt("appointment_id") else null
                    val doctorId = if (data.has("doctor_id")) data.optInt("doctor_id") else null

                    val newNotif = NotificationResponse(
                        id = id,
                        type = type,
                        title = title,
                        description = desc,
                        isRead = isRead,
                        createdAt = createdAt,
                        appointmentId = appointmentId,
                        doctorId = doctorId
                    )

                    addNewNotification(newNotif)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun loadNotifications() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()
        
        if (userId == -1) return

        repository.getNotifications(userId, role) { list ->
            if (list != null) {
                // Sort by newest first
                val sorted = list.sortedByDescending { it.id }
                notifications.postValue(sorted)
                unreadCount.postValue(sorted.count { !it.isRead })
            } else {
                error.postValue("Unable to load notifications")
            }
        }
    }

    private fun addNewNotification(newNotif: NotificationResponse) {
        val currentList = notifications.value?.toMutableList() ?: mutableListOf()
        
        // Prevent duplicates
        val exists = currentList.any { 
            it.title == newNotif.title && 
            it.description == newNotif.description && 
            it.createdAt == newNotif.createdAt 
        }

        if (!exists) {
            currentList.add(0, newNotif)
            notifications.postValue(currentList)
            unreadCount.postValue(currentList.count { !it.isRead })
            
            // Update session manager badge flag
            sessionManager.setHasUnreadNotifications(true)
        }
    }

    fun markAsRead(notificationId: Int) {
        repository.markAsRead(notificationId) { success ->
            if (success) {
                val currentList = notifications.value?.toMutableList() ?: return@markAsRead
                val updatedList = currentList.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
                notifications.postValue(updatedList)
                unreadCount.postValue(updatedList.count { !it.isRead })
            }
        }
    }

    fun markAllAsRead() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()
        
        repository.markAllAsRead(userId, role) { success ->
            if (success) {
                val currentList = notifications.value?.toMutableList() ?: return@markAllAsRead
                val updatedList = currentList.map { it.copy(isRead = true) }
                notifications.postValue(updatedList)
                unreadCount.postValue(0)
                sessionManager.markNotificationsRead()
            }
        }
    }
}
