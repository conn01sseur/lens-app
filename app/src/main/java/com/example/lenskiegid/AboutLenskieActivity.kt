package com.example.lenskiegid

import android.os.Bundle
import android.text.TextUtils
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class AboutLenskieActivity : BaseEdgeToEdgeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        val webView = findViewById<WebView>(R.id.webView)
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = true

        webView.isLongClickable = false
        webView.isHapticFeedbackEnabled = false
        webView.setOnLongClickListener { true }

        webView.webViewClient = object : WebViewClient() {}
        webView.addJavascriptInterface(AboutBridge(this), "AndroidAbout")

        loadAboutPage(webView)
    }

    private fun loadAboutPage(webView: WebView) {
        val template = assets.open("pages/about_lenskie.html")
            .bufferedReader()
            .use { it.readText() }
        val rawText = runCatching {
            assets.open("pages/text_for_btn.txt")
                .bufferedReader()
                .use { it.readText() }
        }.getOrDefault("")

        val normalized = rawText
            .replace("\u2028", "\n")
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        val paragraphs = mutableListOf<String>()
        val current = StringBuilder()
        normalized.split("\n").forEach { line ->
            if (line.trim().isEmpty()) {
                if (current.isNotEmpty()) {
                    paragraphs.add(current.toString())
                    current.clear()
                }
            } else {
                if (current.isNotEmpty()) current.append("\n")
                current.append(line.trim())
            }
        }
        if (current.isNotEmpty()) paragraphs.add(current.toString())

        val blocks = (if (paragraphs.isEmpty()) {
            listOf("Текст пока не заполнен.")
        } else {
            paragraphs
        }).joinToString("\n") { paragraph ->
            val safe = TextUtils.htmlEncode(paragraph).replace("\n", "<br/>")
            "<div class=\"text-card\">$safe</div>"
        }

        val html = template.replace("{{TEXT_BLOCKS}}", blocks)
        webView.loadDataWithBaseURL(
            "file:///android_asset/pages/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    private class AboutBridge(private val activity: AboutLenskieActivity) {
        @JavascriptInterface
        fun goBack() {
            activity.runOnUiThread {
            activity.finish()
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        }
    }
}
