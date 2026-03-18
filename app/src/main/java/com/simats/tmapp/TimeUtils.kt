package com.simats.tmapp

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    /**
     * Converts a UTC timestamp string to the device's local time string.
     * Expected backend format: "yyyy-MM-dd HH:mm:ss"
     */
    fun convertUtcToLocal(utcTime: String?, inputPattern: String = "yyyy-MM-dd HH:mm:ss", outputPattern: String = "dd MMM yyyy, hh:mm a"): String {
        if (utcTime.isNullOrEmpty()) return "N/A"
        
        return try {
            val utcFormatter = SimpleDateFormat(inputPattern, Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = utcFormatter.parse(utcTime)
            
            if (date == null) return utcTime

            val localFormatter = SimpleDateFormat(outputPattern, Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            localFormatter.format(date)
        } catch (e: Exception) {
            utcTime
        }
    }

    /**
     * Converts "yyyy-MM-dd" date strings to "dd MMM yyyy"
     */
    fun formatSimpleDate(dateStr: String?, outputPattern: String = "dd MMM yyyy"): String {
        if (dateStr.isNullOrEmpty()) return "N/A"
        if (dateStr == "null") return "N/A"
        
        return try {
            // Check if it's already a full timestamp
            val inputPattern = if (dateStr.contains(" ")) "yyyy-MM-dd HH:mm:ss" else "yyyy-MM-dd"
            
            val inputFormatter = SimpleDateFormat(inputPattern, Locale.getDefault()).apply {
                if (dateStr.contains(" ")) timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = inputFormatter.parse(dateStr)
            
            if (date == null) return dateStr

            val localFormatter = SimpleDateFormat(outputPattern, Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            localFormatter.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }
}
