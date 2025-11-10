package com.example.lenskiegid.auth

data class UserCreateRequest(
    val email: String,
    val password: String,
    val full_name: String
)