package com.example.lenskiegid

import android.content.Intent
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lenskiegid.weather.WeatherRepository
import com.example.lenskiegid.weather.WeatherUi
import com.example.lenskiegid.weather.WeatherCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar

class MainMenuActivity : BaseEdgeToEdgeActivity() {

    private lateinit var menuWebView: WebView
    private val weatherScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pendingWeatherJson: String? = null
    private val WEATHER_LOCATION_REQUEST = 3001

    // JS-интерфейс, чтобы HTML-меню могло открывать нативные экраны
    private class MenuJsBridge(private val activity: MainMenuActivity) {
        @JavascriptInterface
        fun openMap() {
            activity.runOnUiThread {
                activity.openWithAnim(Intent(activity, MainActivity::class.java))
            }
        }

        @JavascriptInterface
        fun openChecklist() {
            activity.runOnUiThread {
                activity.openWithAnim(Intent(activity, ChecklistActivity::class.java))
            }
        }

        @JavascriptInterface
        fun openAbout() {
            activity.runOnUiThread {
                activity.openWithAnim(Intent(activity, AboutLenskieActivity::class.java))
            }
        }

        @JavascriptInterface
        fun openWeather() {
            activity.runOnUiThread {
                activity.openWithAnim(Intent(activity, WeatherActivity::class.java))
            }
        }

        @JavascriptInterface
        fun openAudio() {
            activity.runOnUiThread {
                activity.openWithAnim(Intent(activity, AudioActivity::class.java))
            }
        }

        @JavascriptInterface
        fun openEmergency() {
            activity.runOnUiThread {
                activity.openWithAnim(Intent(activity, EmergencyActivity::class.java))
            }
        }

        @JavascriptInterface
        fun openTrail() {
            activity.runOnUiThread {
                val intent = Intent(activity, MainActivity::class.java)
                intent.putExtra(MainActivity.EXTRA_SHOW_TRAIL, true)
                activity.openWithAnim(intent)
            }
        }

        @JavascriptInterface
        fun openOther() {
            activity.runOnUiThread {
                activity.openWithAnim(Intent(activity, OtherActivity::class.java))
            }
        }
    }

    private fun openWithAnim(intent: Intent) {
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val greeting = findViewById<TextView>(R.id.tvGreeting)
        applyTopInsetForGreeting(greeting)
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val name = prefs.getString("user_name", null)?.takeIf { it.isNotBlank() }
        val email = prefs.getString("user_email", null)?.takeIf { it.isNotBlank() }
        val dayGreeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Доброе утро, путешественник!"
            in 12..17 -> "Добрый день, путешественник!"
            in 18..22 -> "Добрый вечер, путешественник!"
            else -> "Доброй ночи, путешественник!"
        }
        greeting.text = SpannableStringBuilder(dayGreeting).apply {
            setSpan(
                ForegroundColorSpan(0xFFCAD3D7.toInt()),
                0,
                dayGreeting.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Временный переход на WebView-меню вместо нативных кнопок
        initWebMenu()
        hideNativeButtons()

        WeatherCache.init(this)
        renderCachedWeatherInMenu()
        loadWeatherForMenu()
    }

    private fun applyTopInsetForGreeting(greeting: View) {
        val root = findViewById<View>(android.R.id.content)
        val lp = greeting.layoutParams as ViewGroup.MarginLayoutParams
        val initialTopMargin = lp.topMargin
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            lp.topMargin = initialTopMargin + topInset
            greeting.layoutParams = lp
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun initWebMenu() {
        menuWebView = findViewById(R.id.webViewMainMenu)
        menuWebView.visibility = View.VISIBLE

        val settings: WebSettings = menuWebView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        menuWebView.setBackgroundColor(0x00000000) // прозрачный фон, чтобы был виден фон из layout
        menuWebView.isVerticalScrollBarEnabled = false
        menuWebView.isHorizontalScrollBarEnabled = false
        menuWebView.isLongClickable = false
        menuWebView.isHapticFeedbackEnabled = false
        menuWebView.setOnLongClickListener { true }

        menuWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pendingWeatherJson?.let { js ->
                    view?.evaluateJavascript("window.updateWeatherCard($js);", null)
                }
            }
        }
        menuWebView.addJavascriptInterface(MenuJsBridge(this), "AndroidMenu")
        menuWebView.loadUrl("file:///android_asset/main_menu.html")
    }

    private fun loadWeatherForMenu() {
        val location = getLastKnownLocation()
        if (location != null) {
            fetchAndApplyWeather(location)
            return
        }
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                WEATHER_LOCATION_REQUEST
            )
            return
        }
        requestSingleLocation { loc ->
            if (loc != null) {
                fetchAndApplyWeather(loc)
            }
        }
    }

    private fun renderCachedWeatherInMenu() {
        val cached = WeatherCache.getFreshJson(this, 30 * 60 * 1000L) ?: return
        pendingWeatherJson = cached
        menuWebView.evaluateJavascript("window.updateWeatherCard($cached);", null)
    }

    private fun fetchAndApplyWeather(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        weatherScope.launch {
            val data = runCatching {
                withContext(Dispatchers.IO) {
                    WeatherRepository.fetch(lat, lon)
                }
            }.getOrNull()
            if (data == null) {
                val cached = WeatherCache.getAnyJson(this@MainMenuActivity) ?: return@launch
                pendingWeatherJson = cached
                menuWebView.evaluateJavascript("window.updateWeatherCard($cached);", null)
                return@launch
            }
            val json = JSONObject()
                .put("temp", data.temperatureC)
                .put("condition", data.conditionText)
                .put("icon", WeatherUi.iconFile(data.kind))
                .put("color", WeatherUi.color(data.kind))
                .put("updatedAt", System.currentTimeMillis())
            val js = json.toString()
            pendingWeatherJson = js
            WeatherCache.update(this@MainMenuActivity, js)
            menuWebView.evaluateJavascript("window.updateWeatherCard($js);", null)
        }
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
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
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
        if (requestCode == WEATHER_LOCATION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadWeatherForMenu()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        weatherScope.coroutineContext.cancel()
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
