package com.example.nebo.model

import android.util.Log
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Locale
import java.util.TimeZone

data class SendDto(
    val id: Long,
    val drawingId: Long,
    val drawingTitle: String,
    val drawingPath: String,
    val senderId: Long,
    val senderName: String,
    val senderAvatarPath: String?,
    val recipientId: Long,
    val recipientName: String,
    val sentAt: String
){
    fun formatCreatedAt(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC") // Z означает UTC
            }

            val date = inputFormat.parse(sentAt) ?: return sentAt

            val outputFormat = SimpleDateFormat("dd MMM HH:mm", Locale.ENGLISH).apply {
                timeZone = TimeZone.getDefault()
            }
            outputFormat.format(date)
        } catch (e: Exception) {
            Log.e("DateFormatter", "Error formatting date: ${e.message}")
            sentAt
        }
    }
}