package com.simats.Tmapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AppointmentReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.simats.tmapp.ACTION_APPOINTMENT_REMINDER") return

        val appointmentId = intent.getIntExtra("appointment_id", -1)
        val doctorName = intent.getStringExtra("doctor_name") ?: "Doctor"
        val date = intent.getStringExtra("date") ?: ""
        val time = intent.getStringExtra("time") ?: ""
        val reminderType = intent.getStringExtra("reminder_type") ?: "reminder"

        val title = when (reminderType) {
            "1_hour" -> "Appointment in 1 hour"
            "15_min" -> "Appointment in 15 minutes"
            else -> "Appointment Reminder"
        }

        val message = "Your consultation with Dr. $doctorName is at $time on $date."

        val openIntent = Intent(context, AppointmentDetailsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("appointment_id", appointmentId)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            appointmentId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "appointment_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Appointment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming doctor appointments"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_bell_thin)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        notificationManager.notify(appointmentId + 5000, notification)
    }
}