package com.example.lenskiegid.auth

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("register")
    fun register(@Body userData: UserCreateRequest): Call<AuthResponse>

    @POST("login")
    fun login(@Body userData: UserLoginRequest): Call<AuthResponse>
}

object AuthServiceFactory {
    private const val BASE_URL = "http://46.17.105.226:8000/" // мой сервер, можете запинговать

    fun create(): AuthService {
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
        return retrofit.create(AuthService::class.java)
    }
}