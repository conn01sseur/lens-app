package com.example.lenskiegid

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("user_prefs", MODE_PRIVATE) }
    private var stage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Если уже показывали — сразу дальше
        if (prefs.getBoolean("welcome_shown", false)) {
            goNext()
            return
        }

        setContentView(R.layout.activity_welcome)

        val tv1 = findViewById<View>(R.id.tvWelcome1)
        val tv2 = findViewById<View>(R.id.tvWelcome2)
        val root = findViewById<View>(R.id.welcome_root)

        root.setOnClickListener {
            when (stage) {
                0 -> {
                    // Первый тап: скрываем первый текст, показываем второй
                    tv1.animate()
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction {
                            tv1.visibility = View.GONE
                            tv2.visibility = View.VISIBLE
                            tv2.alpha = 0f
                            tv2.animate()
                                .alpha(1f)
                                .setDuration(500)
                                .start()
                        }
                        .start()
                    stage = 1
                }
                1 -> {
                    // Второй тап: устанавливаем флаг и переходим
                    prefs.edit().putBoolean("welcome_shown", true).apply()
                    goNext()
                }
            }
        }
    }

    private fun goNext() {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val next = if (isLoggedIn) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(next)
        finish()
    }
}