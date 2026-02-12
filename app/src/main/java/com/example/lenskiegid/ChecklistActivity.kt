package com.example.lenskiegid

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class ChecklistActivity : BaseEdgeToEdgeActivity() {

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
        webView.addJavascriptInterface(ChecklistBridge(this), "AndroidChecklist")
        webView.loadUrl("file:///android_asset/pages/checklist.html")
    }

    private class ChecklistBridge(private val activity: ChecklistActivity) {
        @JavascriptInterface
        fun goBack() {
            activity.runOnUiThread {
            activity.finish()
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        }
    }
}
