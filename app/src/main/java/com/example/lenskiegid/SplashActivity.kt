package com.example.lenskiegid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.lenskiegid.auth.AuthGate

class SplashActivity : BaseEdgeToEdgeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigateToNextActivity()
    }

    private fun navigateToNextActivity() {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = AuthGate.isLoggedIn(sharedPreferences)
        val isWelcomeShown = sharedPreferences.getBoolean("welcome_shown", false)
        if (!isWelcomeShown) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }
        
        val intent = if (isLoggedIn || !AuthGate.ENABLE_AUTH) {
            Intent(this, MainMenuActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        
        startActivity(intent)
        finish()
    }
}
