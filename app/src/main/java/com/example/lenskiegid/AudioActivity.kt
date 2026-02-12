package com.example.lenskiegid

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class AudioActivity : BaseEdgeToEdgeActivity() {

    private lateinit var webView: WebView
    private var mediaPlayer: MediaPlayer? = null
    private var currentIndex = -1
    private var isPrepared = false
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            sendPlayerState()
            handler.postDelayed(this, 80)
        }
    }

    private data class AudioItem(
        val id: Int,
        val title: String,
        val group: String,
        val resId: Int
    )

    private val items = listOf(
        AudioItem(1, "Покровск", "settlement", R.raw.pokrovsk_audio),
        AudioItem(2, "Булгунняхтах", "settlement", R.raw.bulgunniahtah_audio),
        AudioItem(3, "Улахан Аан", "settlement", R.raw.ulahan_aan_audio),
        AudioItem(4, "Тит-Ары", "settlement", R.raw.tit_ary_audio),
        AudioItem(5, "Тумул", "settlement", R.raw.tumul_audio),
        AudioItem(6, "Батамай", "settlement", R.raw.batamay_audio),
        AudioItem(7, "Ленские столбы", "route", R.raw.lenskie_stolby_audio),
        AudioItem(8, "Точка 1", "route", R.raw.test),
        AudioItem(9, "Точка 2", "route", R.raw.test),
        AudioItem(10, "Точка 3", "route", R.raw.test),
        AudioItem(11, "Точка 4", "route", R.raw.test)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webView = findViewById(R.id.webView)
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
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                sendListToWeb()
                sendPlayerState()
            }
        }
        webView.addJavascriptInterface(AudioBridge(this), "AndroidAudio")
        webView.loadUrl("file:///android_asset/pages/audio.html")
    }

    private fun sendListToWeb() {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("group", item.group)
            )
        }
        val js = "window.renderAudioList(${arr});"
        webView.evaluateJavascript(js, null)
    }

    private fun playById(id: Int) {
        val index = items.indexOfFirst { it.id == id }
        if (index == -1) return
        if (currentIndex == index && mediaPlayer != null) {
            togglePlayPause()
            return
        }
        currentIndex = index
        startPlayback(items[index])
    }

    private fun startPlayback(item: AudioItem) {
        stopPlayback()
        isPrepared = false
        mediaPlayer = MediaPlayer.create(this, item.resId)?.apply {
            setOnPreparedListener {
                isPrepared = true
                start()
                handler.removeCallbacks(updateRunnable)
                handler.post(updateRunnable)
                sendPlayerState()
            }
            setOnCompletionListener {
                handler.removeCallbacks(updateRunnable)
                sendPlayerState()
            }
        }
        sendPlayerState()
    }

    private fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (isPrepared) player.start()
        }
        sendPlayerState()
    }

    private fun next() {
        if (items.isEmpty()) return
        val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % items.size
        currentIndex = nextIndex
        startPlayback(items[nextIndex])
    }

    private fun prev() {
        if (items.isEmpty()) return
        val prevIndex = if (currentIndex <= 0) items.size - 1 else currentIndex - 1
        currentIndex = prevIndex
        startPlayback(items[prevIndex])
    }

    private fun seekBy(deltaMs: Int) {
        val player = mediaPlayer ?: return
        val newPos = (player.currentPosition + deltaMs).coerceIn(0, player.duration)
        player.seekTo(newPos)
        sendPlayerState()
    }

    private fun sendPlayerState() {
        val json = JSONObject()
        if (currentIndex in items.indices) {
            val item = items[currentIndex]
            json.put("title", item.title)
            json.put("id", item.id)
        }
        val player = mediaPlayer
        json.put("playing", player?.isPlaying == true)
        json.put("pos", player?.currentPosition ?: 0)
        json.put("dur", player?.duration ?: 0)
        val js = "window.updatePlayerState($json);"
        webView.evaluateJavascript(js, null)
    }

    private fun stopPlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        handler.removeCallbacks(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        sendPlayerState()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
    }

    private class AudioBridge(private val activity: AudioActivity) {
        @JavascriptInterface
        fun goBack() {
            activity.runOnUiThread {
            activity.finish()
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        }

        @JavascriptInterface
        fun play(id: Int) {
            activity.runOnUiThread { activity.playById(id) }
        }

        @JavascriptInterface
        fun toggle() {
            activity.runOnUiThread { activity.togglePlayPause() }
        }

        @JavascriptInterface
        fun next() {
            activity.runOnUiThread { activity.next() }
        }

        @JavascriptInterface
        fun prev() {
            activity.runOnUiThread { activity.prev() }
        }

        @JavascriptInterface
        fun seekBy(ms: Int) {
            activity.runOnUiThread { activity.seekBy(ms) }
        }
    }
}
