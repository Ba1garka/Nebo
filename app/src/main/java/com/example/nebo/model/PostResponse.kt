package com.example.nebo.model

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class PostResponse(
    val id: Long,
    val authorId: Long,
    val authorName: String,
    val authorAvatarUrl: String?,
    val drawingId: Long,
    val drawingUrl: String,
    val description: String,
    val createdAt: String,
    val likesCount: Long,
    var isLikedByCurrentUser: Boolean
){
    fun formatCreatedAt(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC") // Z означает UTC
            }

            val date = inputFormat.parse(createdAt) ?: return createdAt

            val outputFormat = SimpleDateFormat("dd MMM HH:mm", Locale.ENGLISH).apply {
                timeZone = TimeZone.getDefault()
            }
            outputFormat.format(date)
        } catch (e: Exception) {
            Log.e("DateFormatter", "Error formatting date: ${e.message}")
            createdAt
        }
    }
}