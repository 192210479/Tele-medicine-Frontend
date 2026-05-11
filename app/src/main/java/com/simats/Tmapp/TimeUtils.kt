package com.simats.Tmapp

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    /**
     * Converts an ISO 8601 UTC timestamp to the device's local time string. 
     * Expected format: "yyyy-MM-dd'T'HH:mm:ss'Z'"
     */
    fun convertIsoUtcToLocal(utc: String?, outputPattern: String = "hh:mm a"): String {
        if (utc.isNullOrEmpty()) return ""
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(utc)
            val output = SimpleDateFormat(outputPattern, Locale.getDefault())
            output.timeZone = TimeZone.getDefault()
            output.format(date!!)
        } catch (e: Exception) {
            utc
        }
    }

    /**
     * Converts a UTC timestamp string to the device's local time string.
     * Expected backend format: "yyyy-MM-dd HH:mm:ss"
     */
    fun convertUtcToLocal(utcTime: String?, inputPattern: String = "yyyy-MM-dd HH:mm:ss", outputPattern: String = "dd MMM yyyy, hh:mm a"): String {
        if (utcTime.isNullOrEmpty()) return ""
        
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
        if (dateStr.isNullOrEmpty()) return ""
        if (dateStr == "null") return ""
        
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
    fun convertUTCToLocal(utc: String?): String {
        if (utc.isNullOrEmpty()) return ""

        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            input.timeZone = TimeZone.getTimeZone("UTC")

            val date = input.parse(utc)

            val output = SimpleDateFormat("hh:mm a", Locale.getDefault())
            output.timeZone = TimeZone.getDefault()

            output.format(date!!)
        } catch (e: Exception) {
            ""
        }
    }

    fun formatDateTime(input: String?): String {
        if (input.isNullOrEmpty()) return ""

        return try {
            val parser = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
            parser.timeZone = TimeZone.getTimeZone("UTC")

            val date = parser.parse(input)

            val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            formatter.timeZone = TimeZone.getDefault()

            formatter.format(date!!)
        } catch (e: Exception) {
            ""
        }
    }

    fun convertIsoUtcToLocalDate(utc: String): String {
        return convertIsoUtcToLocal(utc, "yyyy-MM-dd")
    }
}
