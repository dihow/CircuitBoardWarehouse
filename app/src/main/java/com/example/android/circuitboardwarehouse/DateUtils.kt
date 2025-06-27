package com.example.android.circuitboardwarehouse

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private const val UTC3_OFFSET = 0
    private const val DISPLAY_FORMAT = "dd.MM.yyyy HH:mm"
    private val localTimeZone = TimeZone.getTimeZone("GMT+3")

    fun utcToLocal(utcTime: Long): Long {
        return utcTime + UTC3_OFFSET
    }

    fun localToUtc(localTime: Long): Long {
        return localTime - UTC3_OFFSET
    }

    fun formatForDisplay(utcTime: Long): String {
        val date = Date(utcToLocal(utcTime))
        val sdf = SimpleDateFormat(DISPLAY_FORMAT, Locale.getDefault())
        sdf.timeZone = localTimeZone
        return sdf.format(date)
    }

    fun parseFromDisplay(dateString: String): Long? {
        return try {
            val sdf = SimpleDateFormat(DISPLAY_FORMAT, Locale.getDefault())
            sdf.timeZone = localTimeZone
            localToUtc(sdf.parse(dateString)?.time ?: 0)
        } catch (e: Exception) {
            null
        }
    }
}