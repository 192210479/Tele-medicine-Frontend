package com.simats.Tmapp

import com.simats.Tmapp.api.AppointmentResponse
import java.text.SimpleDateFormat
import java.util.*

/**
 * Shared helper for classifying doctor appointments.
 * Status rules:
 * - UPCOMING   : status == "Scheduled" AND appointment datetime > now (device local time)
 * - COMPLETED  : status == "Completed" OR consultationStatus == "Completed"
 * - MISSED     : status == "Scheduled" AND appointment datetime <= now, with no prescription given
 * - CANCELLED  : status == "Cancelled"
 */
object DoctorAppointmentHelper {

    enum class AppointmentStatus {
        UPCOMING, COMPLETED, CANCELLED, MISSED
    }

    fun getStatus(appt: AppointmentResponse): AppointmentStatus {
        val s = appt.status?.trim() ?: ""
        android.util.Log.d(
            "DOCTOR_HELPER",
            "id=${appt.id}, rawStatus=${appt.status}, consult=${appt.consultationStatus}, date=${appt.date}, localTime=${appt.localTime}, time=${appt.time}"
        )
        val cs = appt.consultationStatus?.trim() ?: ""

        // Explicit terminal states take priority
        if (s.equals("Cancelled", ignoreCase = true)) return AppointmentStatus.CANCELLED
        if (
            s.equals("Completed", ignoreCase = true) ||
            cs.equals("Completed", ignoreCase = true)
        ) return AppointmentStatus.COMPLETED
        if (s.equals("Missed", ignoreCase = true)) return AppointmentStatus.MISSED

        // Valid upcoming booking states
        if (
            s.equals("Scheduled", ignoreCase = true) ||
            s.equals("Confirmed", ignoreCase = true) ||
            s.equals("Booked", ignoreCase = true) ||
            s.equals("Pending", ignoreCase = true) ||
            s.equals("Upcoming", ignoreCase = true)
        ) {
            val apptTime = parseDateTime(appt.date, appt.localTime ?: appt.time)
            val now = Calendar.getInstance().time

            return if (apptTime != null && apptTime.after(now)) {
                android.util.Log.d(
                    "DOCTOR_HELPER",
                    "id=${appt.id}, parsedTime=$apptTime, now=$now"
                )
                AppointmentStatus.UPCOMING
            } else {
                AppointmentStatus.MISSED
            }
        }

        // Fallback: classify using datetime
        val t = parseDateTime(appt.date, appt.localTime ?: appt.time)
        val now = Calendar.getInstance().time
        return if (t != null && t.after(now)) AppointmentStatus.UPCOMING else AppointmentStatus.MISSED
    }

    fun parseDateTime(date: String?, time: String?): Date? {
        if (date.isNullOrEmpty()) return null

        val safeTime = when {
            !time.isNullOrEmpty() -> time
            else -> "00:00:00"
        }

        val timeStr = when {
            safeTime.length == 5 -> "$safeTime:00"
            safeTime.length >= 8 -> safeTime.substring(0, 8)
            else -> "00:00:00"
        }

        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.parse("$date $timeStr")
        } catch (e: Exception) {
            null
        }
    }

    fun cleanText(value: String?, fallback: String = ""): String {
        val cleaned = value?.trim().orEmpty()
        return if (cleaned.isEmpty() || cleaned.equals("null", ignoreCase = true)) fallback else cleaned
    }

    fun sort(list: List<AppointmentResponse>): List<AppointmentResponse> {
        return list.sortedWith { a, b ->
            val left = parseDateTime(a.date, a.localTime ?: a.time)
            val right = parseDateTime(b.date, b.localTime ?: b.time)

            when {
                left == null && right == null -> a.id.compareTo(b.id)
                left == null -> 1
                right == null -> -1
                else -> left.compareTo(right)
            }
        }
    }

    fun getNextAppointment(list: List<AppointmentResponse>): AppointmentResponse? {
        val now = Calendar.getInstance().time

        return list.filter { appt ->
            val status = getStatus(appt)
            val apptTime = parseDateTime(appt.date, appt.localTime ?: appt.time)

            status == AppointmentStatus.UPCOMING &&
                    apptTime != null &&
                    !apptTime.before(now)
        }.sortedBy { parseDateTime(it.date, it.localTime ?: it.time) }
            .firstOrNull()
    }
    
    fun getTimeUntil(appt: AppointmentResponse): String {
        val apptTime = parseDateTime(appt.date, appt.localTime ?: appt.time) ?: return ""
        val now = Calendar.getInstance().time

        val diffMillis = apptTime.time - now.time
        if (diffMillis <= 0) return "Now"

        val totalMinutes = diffMillis / (60 * 1000)
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60

        return when {
            days > 0 -> "in ${days}d ${hours}h"
            hours > 0 -> "in ${hours}h ${minutes}m"
            else -> "in ${minutes} mins"
        }
    }
}
