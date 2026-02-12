package com.example.lenskiegid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lenskiegid.weather.WeatherRepository
import com.example.lenskiegid.weather.WeatherUi
import com.example.lenskiegid.weather.WeatherCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class WeatherActivity : BaseEdgeToEdgeActivity() {

    private lateinit var webView: WebView
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pendingJson: String? = null
    private var pageReady = false
    private val LOCATION_REQUEST = 3002

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

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pageReady = true
                pendingJson?.let { json ->
                    view?.evaluateJavascript("window.renderWeather($json);", null)
                }
            }
        }
        webView.addJavascriptInterface(WeatherBridge(this), "AndroidWeather")
        webView.loadUrl("file:///android_asset/pages/weather.html")

        WeatherCache.init(this)
        renderCachedWeather()
        loadWeather()
    }

    private fun renderCachedWeather() {
        val json = WeatherCache.getFreshJson(this, 30 * 60 * 1000L)?.withUpdatedAtFromCache()
        if (json != null) {
            pendingJson = json
            if (pageReady) {
                webView.evaluateJavascript("window.renderWeather($json);", null)
            }
        }
    }

    private fun loadWeather() {
        val location = getLastKnownLocation()
        if (location != null) {
            fetchWeather(location)
            return
        }
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST
            )
            return
        }
        requestSingleLocation { loc ->
            if (loc != null) {
                fetchWeather(loc)
            }
        }
    }

    private fun fetchWeather(location: Location) {
        scope.launch {
            val data = runCatching {
                withContext(Dispatchers.IO) {
                    WeatherRepository.fetch(location.latitude, location.longitude)
                }
            }.getOrNull()
            if (data == null) {
                val cached = WeatherCache.getAnyJson(this@WeatherActivity)?.withUpdatedAtFromCache() ?: return@launch
                pendingJson = cached
                if (pageReady) {
                    webView.evaluateJavascript("window.renderWeather($cached);", null)
                }
                return@launch
            }
            val fetchedAt = System.currentTimeMillis()
            val json = JSONObject()
                .put("temp", data.temperatureC)
                .put("condition", data.conditionText)
                .put("color", WeatherUi.color(data.kind))
                .put("icon", WeatherUi.iconFile(data.kind))
                .put("humidity", data.humidity)
                .put("wind", data.windSpeed)
                .put("uv", data.uvIndex)
                .put("sunrise", data.sunriseIso)
                .put("sunset", data.sunsetIso)
                .put("updatedAt", fetchedAt)
                .put("hourly", JSONArray().apply {
                    data.hourly.forEach { hour ->
                        put(
                            JSONObject()
                                .put("time", hour.timeIso)
                                .put("temp", hour.temperatureC)
                                .put("icon", WeatherUi.iconFile(hour.kind))
                        )
                    }
                })
            val jsonText = json.toString()
            pendingJson = jsonText
            WeatherCache.update(this@WeatherActivity, jsonText)
            if (pageReady) {
                webView.evaluateJavascript("window.renderWeather($jsonText);", null)
            }
        }
    }

    private fun String.withUpdatedAtFromCache(): String {
        return runCatching {
            val obj = JSONObject(this)
            if (!obj.has("updatedAt") && WeatherCache.lastUpdatedMillis > 0L) {
                obj.put("updatedAt", WeatherCache.lastUpdatedMillis)
            }
            obj.toString()
        }.getOrDefault(this)
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        return providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }

    private fun requestSingleLocation(onResult: (Location?) -> Unit) {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                manager.removeUpdates(this)
                onResult(location)
            }
            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        val provider = if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LocationManager.GPS_PROVIDER
        } else {
            LocationManager.NETWORK_PROVIDER
        }
        runCatching {
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }.onFailure {
            onResult(null)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadWeather()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext.cancel()
    }

    private class WeatherBridge(private val activity: WeatherActivity) {
        @JavascriptInterface
        fun goBack() {
            activity.runOnUiThread {
            activity.finish()
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        }
    }
}
