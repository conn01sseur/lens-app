package com.example.lenskiegid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
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
        val btnContinue = findViewById<View>(R.id.btnContinue)
        val splashImage = findViewById<ImageView>(R.id.splashImage)
        val progressContainer = findViewById<View>(R.id.progressContainer)
        val progressFill = findViewById<View>(R.id.progressFill)

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
                    updateButtonImage(btnContinue)
                }
                1 -> {
                    stage = 2
                    tv1.visibility = View.GONE
                    tv2.visibility = View.GONE
                    splashImage.visibility = View.VISIBLE
                    progressContainer.visibility = View.VISIBLE
                    (root as? View)?.setPadding(0, 0, 0, 0)
                    // align splash button to same position as initial button (already set in layout)
                    updateButtonImage(btnContinue)
                    showSingleSplash(splashImage, progressFill)
                }
                2 -> {
                    prefs.edit().putBoolean("welcome_shown", true).apply()
                    goNext()
                }
            }
        }

        root.setOnClickListener { advance() }
        btnContinue.setOnClickListener { advance() }
        updateButtonImage(btnContinue)
    }

    private fun updateButtonImage(button: View) {
        val imageButton = button as? android.widget.ImageButton ?: return
        val resId = if (stage < 2) R.drawable.bt_continue else R.drawable.bt_next
        imageButton.setImageResource(resId)
    }

    private fun showSingleSplash(imageView: ImageView, progressFill: View) {
        imageView.animate().cancel()
        imageView.alpha = 0f
        imageView.setImageResource(R.drawable.bkg_splash)
        imageView.animate()
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        progressFill.post {
            val totalWidth = (progressFill.parent as View).width
            val animator = ValueAnimator.ofInt(0, totalWidth)
            animator.duration = 700
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener {
                val w = it.animatedValue as Int
                progressFill.layoutParams = progressFill.layoutParams.apply {
                    width = w
                }
                progressFill.requestLayout()
            }
            animator.start()
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
