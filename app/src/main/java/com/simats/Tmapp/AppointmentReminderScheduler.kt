package com.simats.Tmapp

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

object AppointmentReminderScheduler {

    private const val REQUEST_CODE_1_HOUR = 1001
    private const val REQUEST_CODE_15_MIN = 1002

    fun scheduleAppointmentReminders(
        context: Context,
        appointmentId: Int,
        doctorName: String,
        date: String?,
        time: String?
    ) {
        if (appointmentId == -1 || date.isNullOrBlank() || time.isNullOrBlank()) return

        val appointmentMillis = parseLocalDateTimeMillis(date, time) ?: return
        val now = System.currentTimeMillis()

        val oneHourBefore = appointmentMillis - (60 * 60 * 1000)
        val fifteenMinBefore = appointmentMillis - (15 * 60 * 1000)

        if (oneHourBefore > now) {
            scheduleReminder(
                context = context,
                triggerAtMillis = oneHourBefore,
                requestCode = appointmentId * 10 + REQUEST_CODE_1_HOUR,
                appointmentId = appointmentId,
                doctorName = doctorName,
                date = date,
                time = time,
                reminderType = "1_hour"
            )
        }

        if (fifteenMinBefore > now) {
            scheduleReminder(
                context = context,
                triggerAtMillis = fifteenMinBefore,
                requestCode = appointmentId * 10 + REQUEST_CODE_15_MIN,
                appointmentId = appointmentId,
                doctorName = doctorName,
                date = date,
                time = time,
                reminderType = "15_min"
            )
        }
    }

    fun cancelAppointmentReminders(context: Context, appointmentId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        listOf(
            appointmentId * 10 + REQUEST_CODE_1_HOUR,
            appointmentId * 10 + REQUEST_CODE_15_MIN
        ).forEach { requestCode ->
            val intent = Intent(context, AppointmentReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun scheduleReminder(
        context: Context,
        triggerAtMillis: Long,
        requestCode: Int,
        appointmentId: Int,
        doctorName: String,
        date: String,
        time: String,
        reminderType: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
            action = "com.simats.tmapp.ACTION_APPOINTMENT_REMINDER"
            putExtra("appointment_id", appointmentId)
            putExtra("doctor_name", doctorName)
            putExtra("date", date)
            putExtra("time", time)
            putExtra("reminder_type", reminderType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    private fun parseLocalDateTimeMillis(date: String, time: String): Long? {
        return try {
            val normalizedTime = when {
                time.length == 5 -> "$time:00"
                else -> time
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.timeZone = TimeZone.getDefault()
            val parsed = sdf.parse("$date $normalizedTime")
            parsed?.time
        } catch (e: Exception) {
            null
        }
    }
}