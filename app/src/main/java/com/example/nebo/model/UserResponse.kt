package com.example.nebo.model

data class UserResponse(
    val email: String,
    val name: String,
    val birthDate: String,
    val drawingsCount: Int,
    val avatarUrl: String?
) {
    companion object {
        fun fromJson(json: Map<String, Any>): UserResponse {
            return UserResponse(
                email = json["email"] as String,
                name = json["name"] as String,
                birthDate = json["birthDate"] as String,
                drawingsCount = (json["drawingsCount"] as? Number)?.toInt() ?: 0,
                avatarUrl = json["avatarUrl"] as? String
            )
        }
    }
}