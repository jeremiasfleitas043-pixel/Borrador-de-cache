package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ByteFormatter {
    private val decimalFormat = DecimalFormat("0.00")

    fun formatBytes(bytes: Long?): String {
        if (bytes == null || bytes < 0L) return "No disponible"
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return "${decimalFormat.format(kb)} KB"
        val mb = kb / 1024.0
        if (mb < 1024.0) return "${decimalFormat.format(mb)} MB"
        val gb = mb / 1024.0
        return "${decimalFormat.format(gb)} GB"
    }

    fun formatTimestamp(timestampMillis: Long): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES"))
        return formatter.format(Date(timestampMillis))
    }
}
