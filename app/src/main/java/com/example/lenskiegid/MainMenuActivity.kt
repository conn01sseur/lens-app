package com.example.lenskiegid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {

    // JS-интерфейс, чтобы HTML-меню могло открывать нативные экраны
    private class MenuJsBridge(private val activity: MainMenuActivity) {
        @JavascriptInterface
        fun openMap() {
            activity.runOnUiThread {
                activity.startActivity(Intent(activity, MainActivity::class.java))
            }
        }

        @JavascriptInterface
        fun openChecklist() {
            // Заглушка: сюда можно будет повесить экран чек-листа
        }

        @JavascriptInterface
        fun openAbout() {
            // Заглушка: сюда можно будет повесить экран "О Ленских столбах"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val greeting = findViewById<TextView>(R.id.tvGreeting)
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val name = prefs.getString("user_name", null)?.takeIf { it.isNotBlank() }
        val email = prefs.getString("user_email", null)?.takeIf { it.isNotBlank() }
        val displayName = name ?: email ?: "путешественник"
        greeting.text = "Привет, $displayName!"

        // Временный переход на WebView-меню вместо нативных кнопок
        initWebMenu()
        hideNativeButtons()
    }

    private fun initWebMenu() {
        val webView = findViewById<WebView>(R.id.webViewMainMenu)
        webView.visibility = View.VISIBLE

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        webView.setBackgroundColor(0x00000000) // прозрачный фон, чтобы был виден фон из layout
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        webView.webViewClient = object : WebViewClient() {}
        webView.addJavascriptInterface(MenuJsBridge(this), "AndroidMenu")
        webView.loadUrl("file:///android_asset/main_menu.html")
    }

    private fun hideNativeButtons() {
        // Как вернуть нативное XML-меню:
        // 1) удалить вызовы initWebMenu() и hideNativeButtons() из onCreate()
        // 2) вернуть setOnClickListener для btnOpenMap / btnChecklist / btnAboutLenskieStolby
        val ids = listOf(
            R.id.btnOpenMap,
            R.id.btnAboutLenskieStolby,
            R.id.btnChecklist
        )
        ids.forEach { id ->
            findViewById<View?>(id)?.visibility = View.GONE
        }
    }
}
