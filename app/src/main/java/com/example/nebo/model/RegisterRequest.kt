package com.example.nebo.model

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val birthDate: String
)