package com.example.lenskiegid

import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class OtherActivity : BaseEdgeToEdgeActivity() {

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
        webView.addJavascriptInterface(OtherBridge(this), "AndroidOther")
        webView.loadUrl("file:///android_asset/pages/other.html")
    }

    private class OtherBridge(private val activity: OtherActivity) {
        @JavascriptInterface
        fun goBack() {
            activity.runOnUiThread {
            activity.finish()
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        }

        @JavascriptInterface
        fun openDownloadedContent() {
            activity.runOnUiThread {
                activity.startActivity(Intent(activity, DownloadedContentActivity::class.java))
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }
}
