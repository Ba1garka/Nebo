package com.example.nebo.model

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
)