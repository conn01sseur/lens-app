package com.example.lenskiegid.auth

import android.content.SharedPreferences

object AuthGate {
    // Flip to true to re-enable account flow.
    const val ENABLE_AUTH = false

    fun isLoggedIn(prefs: SharedPreferences): Boolean {
        return if (ENABLE_AUTH) {
            prefs.getBoolean("is_logged_in", false)
        } else {
            true
        }
    }
}
