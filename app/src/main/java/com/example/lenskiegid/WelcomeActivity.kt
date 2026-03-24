package com.example.lenskiegid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.lenskiegid.auth.AuthGate

class WelcomeActivity : BaseEdgeToEdgeActivity() {

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
        val btnContinue = findViewById<View>(R.id.btnContinue)

        root.alpha = 0f
        root.animate()
            .alpha(1f)
            .setDuration(260)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        val advance: () -> Unit = {
            when (stage) {
                0 -> {
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
                    updateButtonVisual(btnContinue)
                }
                1 -> {
                    prefs.edit().putBoolean("welcome_shown", true).apply()
                    goNext()
                }
            }
        }

        root.setOnClickListener { advance() }
        btnContinue.setOnClickListener { advance() }
        updateButtonVisual(btnContinue)
    }

    private fun updateButtonVisual(button: View) {
        val btn = button as? Button ?: return
        btn.setBackgroundResource(R.drawable.bt_continue)
        btn.text = getString(R.string.continue_label)
    }

    private fun goNext() {
        val isLoggedIn = AuthGate.isLoggedIn(prefs)
        val next = if (isLoggedIn || !AuthGate.ENABLE_AUTH) {
            Intent(this, MainMenuActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(next)
        finish()
    }
}
