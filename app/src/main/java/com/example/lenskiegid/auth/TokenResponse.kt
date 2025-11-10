package com.example.lenskiegid.auth

data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val user: UserResponse
)