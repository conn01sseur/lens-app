package com.example.lenskiegid

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lenskiegid.routing.BRouterEngine
import com.example.lenskiegid.routing.SegmentsDownloader
import com.example.lenskiegid.routing.TilePackDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.File
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class DownloadedContentActivity : BaseEdgeToEdgeActivity() {
    private lateinit var webView: WebView
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var routeJob: Job? = null
    private var tileJob: Job? = null
    private var pendingRouteAfterPermission = false
    private val lenskiePoint = GeoPoint(61.096667, 127.348333)

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 2101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webView = findViewById(R.id.webView)
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = true

        webView.isLongClickable = false
        webView.isHapticFeedbackEnabled = false
        webView.setOnLongClickListener { true }

        webView.clearCache(true)
        webView.clearHistory()
        webView.webViewClient = object : WebViewClient() {}
        webView.addJavascriptInterface(DownloadBridge(this), "AndroidDownload")
        webView.loadUrl("file:///android_asset/pages/downloaded_content.html")
    }

    private fun startRouteDownload() {
        if (routeJob?.isActive == true) return
        if (!hasLocationPermission()) {
            pendingRouteAfterPermission = true
            requestLocationPermission()
            return
        }
        updateRouteState("loading")

        routeJob = scope.launch {
            try {
                val engine = BRouterEngine(this@DownloadedContentActivity)
                val segmentsDir = engine.segmentsPath()
                val currentPoint = getBestLastKnownGeoPoint()
                val requiredFiles = if (currentPoint != null) {
                    neededTilesForRoute(currentPoint, lenskiePoint)
                } else {
                    neededTilesForBounds(59.0, 63.0, 124.0, 130.0)
                }
                val missingCount = requiredFiles.count { !File(segmentsDir, it).exists() }

                if (missingCount == 0) {
                    updateRouteState("done")
                    showToast("Маршрут уже скачан")
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    if (currentPoint != null) {
                        SegmentsDownloader.ensureSegments(
                            this@DownloadedContentActivity,
                            segmentsDir,
                            currentPoint,
                            lenskiePoint
                        )
                    } else {
                        SegmentsDownloader.downloadLenskiePackage(segmentsDir) { _, _, _ -> }
                    }
                }

                updateRouteState("done")
                showToast("Маршрут скачан")
            } catch (_: Exception) {
                updateRouteState("error")
                showToast("Ошибка скачивания маршрута")
            }
        }
    }

    private fun startTileDownload() {
        if (tileJob?.isActive == true) return
        updateTileState("loading")

        tileJob = scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    TilePackDownloader.downloadLenskieTiles(this@DownloadedContentActivity) { _, _, _ -> }
                }
                updateTileState("done")
            } catch (_: Exception) {
                updateTileState("error")
            }
        }
    }

    private fun updateRouteState(state: String) {
        val safe = state.replace("'", "")
        webView.evaluateJavascript("window.setRouteState('$safe');", null)
    }

    private fun updateTileState(state: String) {
        val safe = state.replace("'", "")
        webView.evaluateJavascript("window.setTileState('$safe');", null)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext.cancel()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != LOCATION_PERMISSION_REQUEST) return
        val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
        if (granted && pendingRouteAfterPermission) {
            pendingRouteAfterPermission = false
            startRouteDownload()
        } else if (pendingRouteAfterPermission) {
            pendingRouteAfterPermission = false
            showToast("Без GPS нельзя проверить маршрут")
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST
        )
    }

    private fun getBestLastKnownGeoPoint(): GeoPoint? {
        if (!hasLocationPermission()) return null
        val manager = getSystemService(LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        val locations = providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        val best = locations.maxByOrNull { it.time }
        return best?.let { GeoPoint(it.latitude, it.longitude) }
    }

    private fun neededTilesForRoute(from: GeoPoint, to: GeoPoint): List<String> {
        return neededTilesForBounds(
            min(from.latitude, to.latitude),
            max(from.latitude, to.latitude),
            min(from.longitude, to.longitude),
            max(from.longitude, to.longitude)
        )
    }

    private fun neededTilesForBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<String> {
        val nMinLat = floorTo5(minLat)
        val nMaxLat = floorTo5(maxLat)
        val nMinLon = floorTo5(minLon)
        val nMaxLon = floorTo5(maxLon)
        val names = mutableListOf<String>()
        var lat = nMinLat
        while (lat <= nMaxLat) {
            var lon = nMinLon
            while (lon <= nMaxLon) {
                names.add("${lonPrefix(lon)}_${latPrefix(lat)}.rd5")
                lon += 5
            }
            lat += 5
        }
        return names
    }

    private fun floorTo5(x: Double): Int = floor(x / 5.0).toInt() * 5
    private fun lonPrefix(lon: Int): String = if (lon >= 0) "E$lon" else "W${-lon}"
    private fun latPrefix(lat: Int): String = if (lat >= 0) "N$lat" else "S${-lat}"

    private fun showToast(text: String) {
        runOnUiThread { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
    }

    private class DownloadBridge(private val activity: DownloadedContentActivity) {
        @JavascriptInterface
        fun goBack() {
            activity.runOnUiThread {
            activity.finish()
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        }

        @JavascriptInterface
        fun startRouteDownload() {
            activity.runOnUiThread { activity.startRouteDownload() }
        }

        @JavascriptInterface
        fun startTileDownload() {
            activity.runOnUiThread { activity.startTileDownload() }
        }
    }
}
