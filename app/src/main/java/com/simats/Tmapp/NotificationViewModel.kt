package com.simats.Tmapp

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.simats.Tmapp.api.NotificationResponse
import kotlinx.coroutines.launch
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
                    val createdAt = if (data.has("created_at") && !data.optString("created_at").isNullOrBlank()) {
                        data.optString("created_at")
                    } else {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date())
                    }
                    val isRead = data.optBoolean("is_read", false)
                    val appointmentId = if (data.has("appointment_id")) data.optInt("appointment_id") else null
                    val referenceId = if (data.has("reference_id")) data.optInt("reference_id") else null
                    val doctorId = if (data.has("doctor_id")) data.optInt("doctor_id") else null

                    val newNotif = NotificationResponse(
                        id = id,
                        type = type,
                        title = title,
                        description = desc,
                        isRead = isRead,
                        createdAt = createdAt,
                        referenceId = referenceId,
                        appointmentId = appointmentId,
                        data = data.optString("data", null),
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
        viewModelScope.launch {
            try {
                val userId = sessionManager.getUserId()
                val role = sessionManager.getUserRole().lowercase()
                
                if (userId == -1) return@launch
                
                Log.d("NotificationVM", "Loading notifications for user=$userId role=$role")
                
                val list = repository.getNotifications(userId, role, null)
                Log.d("NotificationVM", "Loaded ${list.size} notifications")
                
                // Sort by newest first before posting
                val sortedList = list.sortedByDescending { it.id }
                notifications.postValue(sortedList)
                
                val count = repository.getUnreadCount(userId, role)
                unreadCount.postValue(count)
                
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error loading notifications", e)
                notifications.postValue(emptyList())
                unreadCount.postValue(0)
            }
        }
    }

    private fun addNewNotification(newNotif: NotificationResponse) {
        val currentList = notifications.value?.toMutableList() ?: mutableListOf()
        
        // Prevent duplicates
        val exists = currentList.any { 
            it.referenceId == newNotif.referenceId &&
            it.type.equals(newNotif.type, ignoreCase = true)
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
