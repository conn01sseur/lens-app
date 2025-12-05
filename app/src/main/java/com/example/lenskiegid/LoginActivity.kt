package com.example.lenskiegid

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.lenskiegid.auth.AuthResponse
import com.example.lenskiegid.auth.AuthServiceFactory
import com.example.lenskiegid.auth.UserCreateRequest
import com.example.lenskiegid.auth.UserLoginRequest
import com.example.lenskiegid.auth.UserResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var sharedPreferences: SharedPreferences
    private val authService = AuthServiceFactory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        val userEmail = sharedPreferences.getString("user_email", null)
        if (userEmail != null) {
            startMainActivity()
            return
        }

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                loginUser(email, password)
            }
        }

        tvRegister.setOnClickListener {
            showRegistrationDialog()
        }
    }

    private fun showRegistrationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_register, null)
        val etFullName = dialogView.findViewById<EditText>(R.id.etFullName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Регистрация")
            .setView(dialogView)
            .setPositiveButton("Зарегистрироваться") { _, _ ->
                val fullName = etFullName.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()
                val confirmPassword = etConfirmPassword.text.toString().trim()

                if (validateRegistration(fullName, email, password, confirmPassword)) {
                    registerUser(fullName, email, password)
                }
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun validateRegistration(fullName: String, email: String, password: String, confirmPassword: String): Boolean {
        if (fullName.isEmpty()) {
            showError("Введите имя")
            return false
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Введите корректный email")
            return false
        }
        if (password.length < 3) {
            showError("Пароль должен содержать минимум 3 символа")
            return false
        }
        if (password != confirmPassword) {
            showError("Пароли не совпадают")
            return false
        }
        return true
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            etEmail.error = "Введите email"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Введите корректный email"
            return false
        }
        if (password.isEmpty()) {
            etPassword.error = "Введите пароль"
            return false
        }
        return true
    }

    private fun loginUser(email: String, password: String) {
        showLoading(true)

        val loginRequest = UserLoginRequest(email, password)

        authService.login(loginRequest).enqueue(object : Callback<com.example.lenskiegid.auth.AuthResponse> {
            override fun onResponse(call: Call<com.example.lenskiegid.auth.AuthResponse>, response: Response<com.example.lenskiegid.auth.AuthResponse>) {
                showLoading(false)

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse?.success == true) {
                        saveUserData(authResponse.user)
                        startMainActivity()
                        showSuccess(authResponse.message)
                    } else {
                        showError(authResponse?.message ?: "Ошибка входа")
                    }
                } else {
                    when (response.code()) {
                        401 -> showError("Неверный email или пароль")
                        400 -> showError("Пользователь не найден")
                        else -> showError("Ошибка сервера: ${response.code()}")
                    }
                }
            }

            override fun onFailure(call: Call<com.example.lenskiegid.auth.AuthResponse>, t: Throwable) {
                showLoading(false)
                showError("Ошибка сети: ${t.message}")
            }
        })
    }

    private fun registerUser(fullName: String, email: String, password: String) {
        showLoading(true)

        val registerRequest = UserCreateRequest(email, password, fullName)

        authService.register(registerRequest).enqueue(object : Callback<com.example.lenskiegid.auth.AuthResponse> {
            override fun onResponse(call: Call<com.example.lenskiegid.auth.AuthResponse>, response: Response<com.example.lenskiegid.auth.AuthResponse>) {
                showLoading(false)

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse?.success == true) {
                        saveUserData(authResponse.user)
                        startMainActivity()
                        showSuccess(authResponse.message)
                    } else {
                        showError(authResponse?.message ?: "Ошибка регистрации")
                    }
                } else {
                    when (response.code()) {
                        400 -> showError("Пользователь с таким email уже существует")
                        else -> showError("Ошибка регистрации: ${response.code()}")
                    }
                }
            }

            override fun onFailure(call: Call<com.example.lenskiegid.auth.AuthResponse>, t: Throwable) {
                showLoading(false)
                showError("Ошибка сети: ${t.message}")
            }
        })
    }

    private fun saveUserData(user: UserResponse) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("is_logged_in", true)
        editor.putString("user_email", user.email)
        editor.putString("user_name", user.full_name)
        editor.putInt("user_id", user.id)
        editor.apply()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainMenuActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
