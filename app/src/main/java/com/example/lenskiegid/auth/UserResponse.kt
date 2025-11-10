package com.example.lenskiegid.auth

data class UserResponse(
    val id: Int,
    val email: String,
    val full_name: String,
    val created_at: String
) // Данные которые отправляются на сервер выглядят таким образом