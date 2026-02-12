package com.example.lenskiegid

import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebScreenActivity : BaseEdgeToEdgeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        val img = intent.getStringExtra(EXTRA_IMAGE) ?: "about.png"
        val url = Uri.parse("file:///android_asset/screens/screen.html")
            .buildUpon()
            .appendQueryParameter("img", img)
            .build()
            .toString()

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
        webView.addJavascriptInterface(ScreenBridge(this), "AndroidScreen")
        webView.loadUrl(url)
    }

    private class ScreenBridge(private val activity: WebScreenActivity) {
        @JavascriptInterface
        fun goBack() {
            activity.runOnUiThread {
            activity.finish()
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        }
    }

    companion object {
        const val EXTRA_IMAGE = "screen_image"
    }
}
