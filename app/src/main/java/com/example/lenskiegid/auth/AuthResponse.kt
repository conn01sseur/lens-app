package com.example.lenskiegid.auth

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: UserResponse
)