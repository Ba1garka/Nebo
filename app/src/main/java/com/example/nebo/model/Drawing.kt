package com.example.nebo.model

data class Drawing(
    val id: Long? = null,
    val title: String,
    val filePath: String,
    val createdAt: String, // Используем String для упрощения
    val user: UserSimpleDto
)