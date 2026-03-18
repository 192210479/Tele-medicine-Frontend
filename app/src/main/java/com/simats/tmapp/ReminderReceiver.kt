package com.simats.tmapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.simats.tmapp.api.GenericResponse
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val reminderId = intent.getIntExtra("reminder_id", -1)
        val medicineName = intent.getStringExtra("medicine_name") ?: "Medicine"
        val userId = intent.getIntExtra("user_id", -1)

        when (action) {
            "com.simats.tmapp.ACTION_REMINDER" -> {
                showNotification(context, reminderId, medicineName)
                createInAppNotification(userId, medicineName)
            }
            "com.simats.tmapp.ACTION_MARK_TAKEN" -> {
                markAsTaken(context, reminderId)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // In a real app, we'd fetch and reschedule all alarms here
            }
        }
    }

    private fun showNotification(context: Context, reminderId: Int, medicineName: String) {
        val channelId = "medication_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for medication reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MedicationRemindersActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, reminderId, intent, PendingIntent.FLAG_IMMUTABLE)

        val takenIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.simats.tmapp.ACTION_MARK_TAKEN"
            putExtra("reminder_id", reminderId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(context, reminderId + 1000, takenIntent, PendingIntent.FLAG_IMMUTABLE)

        // Snooze Options
        val snooze5Intent = createSnoozeIntent(context, reminderId, medicineName, 5)
        val snooze10Intent = createSnoozeIntent(context, reminderId, medicineName, 10)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle("Medication Reminder")
            .setContentText("Time to take $medicineName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_check, "Mark Taken", takenPendingIntent)
            .addAction(R.drawable.ic_plus, "Snooze 5m", snooze5Intent)
            .addAction(R.drawable.ic_plus, "Snooze 10m", snooze10Intent)
            .build()

        notificationManager.notify(reminderId, notification)
    }

    private fun createInAppNotification(userId: Int, medicineName: String) {
        if (userId == -1) return

        val body = mapOf(
            "user_id" to userId,
            "role" to "patient",
            "type" to "Medication",
            "title" to "Medication Reminder",
            "description" to "Time to take $medicineName"
        )

        ApiClient.instance.createNotification(body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                // In-app notification created successfully
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                // Fail silently
            }
        })
    }

    private fun createSnoozeIntent(context: Context, reminderId: Int, medicineName: String, minutes: Int): PendingIntent {
        val intent = Intent(context, SnoozeReceiver::class.java).apply {
            action = "com.simats.tmapp.ACTION_SNOOZE"
            putExtra("reminder_id", reminderId)
            putExtra("medicine_name", medicineName)
            putExtra("snooze_minutes", minutes)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId + 2000 + minutes,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun markAsTaken(context: Context, reminderId: Int) {
        if (reminderId == -1) return

        ApiClient.instance.completeMedicationReminder(reminderId).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(reminderId)
                
                val refreshIntent = Intent("com.simats.tmapp.REFRESH_REMINDERS")
                context.sendBroadcast(refreshIntent)
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
            }
        })
    }
}
