package com.example.lenskiegid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
 
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
 
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
 
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import com.example.lenskiegid.routing.BRouterEngine
import com.example.lenskiegid.data.PointsCatalog
import com.example.lenskiegid.data.MarkersCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
 
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AlertDialog
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var btnBuildRoute: Button
    private lateinit var btnClearRoute: Button
    private lateinit var btnLogout: Button
    private lateinit var btnPlayAudio: Button
    private lateinit var tvRouteInfo: TextView
    private lateinit var tvAudioInfo: TextView
    private lateinit var btnDownloadYakutia: Button
    private lateinit var segmentsProgressPanel: android.widget.LinearLayout
    private lateinit var progressSegments: android.widget.ProgressBar
    private lateinit var tvSegmentsStatus: TextView
    private var startPoint: GeoPoint? = null
    private var currentEndPoint: GeoPoint? = null
    private val markers = mutableListOf<Marker>()
    private val audioZones = mutableListOf<Polygon>()
    private val audioZoneInfoMap = mutableMapOf<Polygon, AudioZoneInfo>()
    private var audioZonesAdded = false
    private var currentRoutePoints: List<GeoPoint> = emptyList()
    private var currentRouteDistance: Double = 0.0
    private var currentRouteDuration: String = ""

    

    private val lenskieStolbyPoint = GeoPoint(61.096667, 127.348333)

    private lateinit var proximityHandler: Handler
    private var lastProximityCheckTime = 0L
    private val PROXIMITY_CHECK_INTERVAL = 2000L

    private var mediaPlayer: MediaPlayer? = null
    private var isAudioPlaying = false
    private var currentAudioZone: String? = null
    private var currentAudioDistanceText: String = ""

    private var isLocationReady = false
    private var fullRoutePoints: List<GeoPoint> = emptyList()
    private var lastRouteUpdateIndex = 0
    private val ROUTE_UPDATE_INTERVAL = 3000L
    private var rerouteJob: Job? = null

 

    private lateinit var brouterEngine: BRouterEngine

 

    // запуск и подготовка UI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (!sharedPreferences.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

    

        setContentView(R.layout.activity_main)

        tvRouteInfo = findViewById(R.id.tvRouteInfo)
        tvAudioInfo = findViewById(R.id.tvAudioInfo)
        btnBuildRoute = findViewById(R.id.btnBuildRoute)
        btnClearRoute = findViewById(R.id.btnClearRoute)
        btnLogout = findViewById(R.id.btnLogout)
        btnPlayAudio = findViewById(R.id.btnPlayAudio)
        btnDownloadYakutia = findViewById(R.id.btnDownloadYakutia)
        segmentsProgressPanel = findViewById(R.id.segmentsProgressPanel)
        progressSegments = findViewById(R.id.progressSegments)
        tvSegmentsStatus = findViewById(R.id.tvSegmentsStatus)

        proximityHandler = Handler(Looper.getMainLooper())

        setupOSMDroidConfig()
        initMap()
        setupButtons()
        setupAudioButton()
        brouterEngine = BRouterEngine(this)
        setupYakutiaDownloadButton()
        try {
            addAudioZones()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Не удалось создать аудио зоны", Toast.LENGTH_SHORT).show()
        }
        addPointsOfInterest()
        if (checkLocationPermission()) {
            enableMyLocation()
        } else {
            requestLocationPermission()
        }

        updateRouteInfo("Ожидание определения местоположения...", "")
        btnBuildRoute.isEnabled = false
    }

    private fun setupButtons() {
        btnBuildRoute.setOnClickListener {
            if (!isLocationReady) {
                Toast.makeText(this, "Определяется ваше местоположение...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (startPoint != null && currentEndPoint != null) {
                buildOfflineRoute(startPoint!!, currentEndPoint!!)
            } else {
                Toast.makeText(this, "Сначала выберите точку на карте", Toast.LENGTH_SHORT).show()
            }
        }

        btnClearRoute.setOnClickListener {
            clearRoute()
        }

        btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun buildOfflineRoute(start: GeoPoint, end: GeoPoint) {
        updateRouteInfo("Строим оффлайн-маршрут...", "")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val points = withContext(Dispatchers.IO) {
                    brouterEngine.routeCar(start, end)
                }
                if (points.isNotEmpty()) {
                    displayRoute(points)
                    val distanceKm = calculateRouteDistance(points)
                    val timeText = calculateEstimatedTime(distanceKm)
                    updateRouteInfo("${"%.1f".format(distanceKm)} км", timeText)
                    Toast.makeText(this@MainActivity, "Оффлайн-маршрут построен", Toast.LENGTH_SHORT).show()
                } else {
                    updateRouteInfo("Маршрут не построен", "")
                    Toast.makeText(this@MainActivity, "Не удалось построить маршрут", Toast.LENGTH_LONG).show()
                } // asdasd
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = e.message ?: "Ошибка оффлайн-маршрутизации"
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                updateRouteInfo("Ошибка", "")
            }
        }
    }

    // аудио-кнопка
    private fun setupAudioButton() {
        btnPlayAudio.setOnClickListener {
            if (isAudioPlaying) {
                stopAudio()
            } else {
                playCurrentZoneAudio()
            }
        }
        updateAudioButton()
    }

    private fun setupYakutiaDownloadButton() {
        btnDownloadYakutia.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    segmentsProgressPanel.visibility = android.view.View.VISIBLE
                    progressSegments.progress = 0
                    tvSegmentsStatus.text = "0%"

                    withContext(Dispatchers.IO) {
                        com.example.lenskiegid.routing.SegmentsDownloader.downloadYakutia(
                            this@MainActivity,
                            brouterEngine.segmentsPath(),
                            onProgress = { current, total, name ->
                                val percent = if (total > 0) (current * 100 / total) else 100
                                runOnUiThread {
                                    progressSegments.progress = percent
                                    tvSegmentsStatus.text = if (name.isNotEmpty()) "$percent%  $name" else "$percent%"
                                }
                            }
                        )
                    }
                    Toast.makeText(this@MainActivity, "Сегменты Якутии загружены", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, "Ошибка загрузки сегментов", Toast.LENGTH_LONG).show()
                } finally {
                    segmentsProgressPanel.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun playCurrentZoneAudio() {
        currentAudioZone?.let { zoneName ->
            val poi = PointsCatalog.findByName(zoneName)
            val audioResource = poi?.audioResId
            if (audioResource != null) {
                playAudio(audioResource, zoneName)
            } else {
                Toast.makeText(this, "Аудио для этой зоны не найдено", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Вы не в зоне с аудиогидом", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playAudio(audioResource: Int, zoneName: String) {
        try {
            stopAudio()

            mediaPlayer = MediaPlayer().apply {
                val afd = resources.openRawResourceFd(audioResource)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                setOnPreparedListener {
                    start()
                    isAudioPlaying = true
                    updateAudioButton()
                    Toast.makeText(this@MainActivity, "Аудиогид: $zoneName", Toast.LENGTH_SHORT).show()
                }

                setOnCompletionListener {
                    isAudioPlaying = false
                    updateAudioButton()
                }

                setOnErrorListener { _, _, _ ->
                    isAudioPlaying = false
                    updateAudioButton()
                    Toast.makeText(this@MainActivity, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show()
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка загрузки аудио", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudio() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null
        isAudioPlaying = false
        updateAudioButton()
    }

    private fun updateAudioButton() {
        runOnUiThread {
            btnPlayAudio.text = ""
            if (isAudioPlaying) {
                btnPlayAudio.isEnabled = true
                btnPlayAudio.setBackgroundResource(R.drawable.rounded_button_pause)
            } else {
                if (currentAudioZone == null) {
                    btnPlayAudio.isEnabled = false
                    btnPlayAudio.setBackgroundResource(R.drawable.rounded_button_audio)
                } else {
                    btnPlayAudio.isEnabled = true
                    btnPlayAudio.setBackgroundResource(R.drawable.rounded_button_audio)
                }
            }
        }
    }

    private fun updateAudioInfo() {
        runOnUiThread {
            if (currentAudioZone == null) {
                tvAudioInfo.text = "не в зоне действие аудио гида"
            } else {
                val distance = if (currentAudioDistanceText.isNotEmpty()) " • $currentAudioDistanceText" else ""
                tvAudioInfo.text = "${currentAudioZone}$distance"
            }
        }
    }

 

    data class AudioZoneInfo(
        val name: String,
        val center: GeoPoint,
        val radiusMeters: Double,
        val type: String
    )

    private fun addAudioZones() {
        if (audioZonesAdded) return
        try {
            PointsCatalog.pointsOfInterest.forEach { poi ->
                try {
                    val center = poi.point
                    val radius = poi.customRadius ?: PointsCatalog.defaultRadius(poi.type)
                    val zoneColor = getZoneColor(poi.type)
                    val name = poi.name

                    val zone = createCirclePolygon(center, radius, zoneColor, name)
                    audioZones.add(zone)
                    audioZoneInfoMap[zone] = AudioZoneInfo(name, center, radius, poi.type)
                    map.overlays.add(zone)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            audioZonesAdded = true
            map.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка создания аудио зон", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getZoneColor(type: String): Int {
        return Color.argb(60, 0, 0, 255)
    }

    private fun createCirclePolygon(center: GeoPoint, radius: Double, color: Int, name: String): Polygon {
        val points = mutableListOf<GeoPoint>()
        val steps = 24

        for (i in 0 until steps) {
            try {
                val angle = 2.0 * Math.PI * i / steps
                val earthRadius = 6371000.0
                val latOffset = (radius / earthRadius) * (180.0 / Math.PI) * cos(angle)
                val lonOffset = (radius / earthRadius) * (180.0 / Math.PI) * sin(angle) / cos(Math.toRadians(center.latitude))

                val point = GeoPoint(
                    center.latitude + latOffset,
                    center.longitude + lonOffset
                )
                points.add(point)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (points.isNotEmpty()) {
            points.add(points.first())
        }

        return object : Polygon() {
            override fun onSingleTapConfirmed(pEvent: android.view.MotionEvent, pMapView: org.osmdroid.views.MapView): Boolean {
                return false
            }
        }.apply {
            this.points = points
            fillPaint.color = color
            outlinePaint.color = Color.argb(200, Color.red(color), Color.green(color), Color.blue(color))
            outlinePaint.strokeWidth = 2.0f
            outlinePaint.style = Paint.Style.STROKE
            this.title = "$name (${(radius / 1000).toInt()} км)"
        }
    }

    private fun checkAudioZones() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProximityCheckTime < PROXIMITY_CHECK_INTERVAL) {
            return
        }

        var foundZone: String? = null

        startPoint?.let { currentLocation ->
            audioZoneInfoMap.forEach { (zone, info) ->
                val distance = currentLocation.distanceToAsDouble(info.center)
                if (distance <= info.radiusMeters) {
                    foundZone = info.name
                    currentAudioDistanceText = if (distance < 1000) {
                        "${distance.toInt()} м"
                    } else {
                        "${"%.1f".format(distance / 1000)} км"
                    }
                    if (currentAudioZone != info.name) {
                        showZoneNotification(info.name, distance)
                    }
                }
            }
        }

        currentAudioZone = foundZone
        updateAudioInfo()
        updateAudioButton()

        if (foundZone == null && isAudioPlaying) {
            stopAudio()
        }

        lastProximityCheckTime = currentTime
    }
    
    // прогресс движения
    private fun updateRouteProgress() {
        if (fullRoutePoints.isEmpty() || startPoint == null) {
            return
        }

        val currentLocation = startPoint!!
        
        var closestIndex = lastRouteUpdateIndex
        var minDistance = Double.MAX_VALUE

        for (i in lastRouteUpdateIndex until fullRoutePoints.size) {
            val distance = currentLocation.distanceToAsDouble(fullRoutePoints[i])
            if (distance < minDistance) {
                minDistance = distance
                closestIndex = i
            }
        }

        if (minDistance < 30 && closestIndex > lastRouteUpdateIndex) {
            lastRouteUpdateIndex = closestIndex
            val remainingRoute = fullRoutePoints.subList(lastRouteUpdateIndex, fullRoutePoints.size)
            runOnUiThread {
                updateRemainingRoute(remainingRoute)
            }
        }
    }
    
    private fun updateRemainingRoute(remainingPoints: List<GeoPoint>) {
        val locationOverlayIndex = map.overlays.indexOf(locationOverlay)
        val routeToRemove = map.overlays.filterIsInstance<Polyline>().filter { it !== locationOverlay }
        routeToRemove.forEach { map.overlays.remove(it) }
        
        if (remainingPoints.isEmpty()) {
            return
        }
        
        val routeLine = Polyline().apply {
            setPoints(remainingPoints)
            outlinePaint.color = Color.parseColor("#1976D2")
            outlinePaint.strokeWidth = 15.0f
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
            outlinePaint.isAntiAlias = true
        }
        
        if (locationOverlayIndex >= 0) {
            map.overlays.add(locationOverlayIndex, routeLine)
        } else {
            map.overlays.add(routeLine)
        }
        
        currentRoutePoints = remainingPoints
        
        val distance = calculateRouteDistance(remainingPoints)
        val time = calculateEstimatedTime(distance)
        currentRouteDistance = distance
        currentRouteDuration = time
        
        updateRouteInfo("${"%.1f".format(distance)} км", time)
        map.invalidate()
    }

    private fun showZoneNotification(zoneName: String, distance: Double) {
        val distanceText = if (distance < 1000) {
            "${distance.toInt()} м"
        } else {
            "${"%.1f".format(distance / 1000)} км"
        }

        Toast.makeText(
            this,
            "Вы в зоне: $zoneName\nДоступен аудиогид\nРасстояние до центра: $distanceText",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun logoutUser() {
        stopAudio()

        val editor = getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
        editor.clear()
        editor.apply()

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()

        Toast.makeText(this, "Вы вышли из системы", Toast.LENGTH_SHORT).show()
    }

    private fun clearRoute() {
        map.overlays.removeAll { it is Polyline }
        currentRoutePoints = emptyList()
        fullRoutePoints = emptyList()
        lastRouteUpdateIndex = 0
        map.invalidate()
        updateRouteInfo("Маршрут не построен", "")
    }

    // маршрут онлайн
    
    
    
    
    
    
    // Получить имя маркера
    
    
    // Отобразить маршрут на карте
    private fun displayRoute(points: List<GeoPoint>) {
        clearRoute()

        val routeLine = Polyline().apply {
            setPoints(points)
            outlinePaint.color = Color.parseColor("#1976D2")
            outlinePaint.strokeWidth = 15.0f
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
            outlinePaint.isAntiAlias = true
        }

        map.overlays.add(routeLine)
        currentRoutePoints = points
        fullRoutePoints = points.toList()
        lastRouteUpdateIndex = 0
        
        map.invalidate()
        fitToPoints(points)
        startPeriodicReroute()
    }

    // Автоматически загружает маршрут до Ленских столбов при старте
    private fun buildRouteToLenskieStolby() {
        if (!isLocationReady || startPoint == null) {
            return
        }

        startPoint?.let { currentStart ->
            currentEndPoint = lenskieStolbyPoint
            buildOfflineRoute(currentStart, lenskieStolbyPoint)
        }
    }

    

    private fun findClosestPointIndex(point: GeoPoint, routePoints: List<GeoPoint>): Int {
        var minDistance = Double.MAX_VALUE
        var closestIndex = 0

        for (i in routePoints.indices) {
            val distance = point.distanceToAsDouble(routePoints[i])
            if (distance < minDistance) {
                minDistance = distance
                closestIndex = i
            }
        }
        return closestIndex
    }

    private fun calculateRouteDistance(points: List<GeoPoint>): Double {
        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += points[i].distanceToAsDouble(points[i + 1])
        }
        return totalDistance / 1000
    }

    private fun calculateEstimatedTime(distanceKm: Double): String {
        val averageSpeed = 60.0
        val totalMinutes = (distanceKm / averageSpeed * 60).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (hours > 0) {
            "${hours} ч ${minutes} мин"
        } else {
            "${minutes} мин"
        }
    }

    private fun showRouteOnMap(points: List<GeoPoint>, color: Int, strokeWidth: Float) {
        clearRoute()

        val routeLine = Polyline().apply {
            setPoints(points)
            outlinePaint.color = color
            outlinePaint.strokeWidth = strokeWidth
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
            outlinePaint.isAntiAlias = true
        }

        map.overlays.add(routeLine)
        map.invalidate()
        fitToPoints(points)
    }

    private fun fitToPoints(points: List<GeoPoint>) {
        if (points.isEmpty()) return
        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE
        for (p in points) {
            if (p.latitude < minLat) minLat = p.latitude
            if (p.latitude > maxLat) maxLat = p.latitude
            if (p.longitude < minLon) minLon = p.longitude
            if (p.longitude > maxLon) maxLon = p.longitude
        }
        val bbox = org.osmdroid.util.BoundingBox(maxLat, maxLon, minLat, minLon)
        // padding в градусах приблизительно через увеличение bbox
        val padLat = (maxLat - minLat) * 0.1
        val padLon = (maxLon - minLon) * 0.1
        val padded = org.osmdroid.util.BoundingBox(
            maxLat + padLat,
            maxLon + padLon,
            minLat - padLat,
            minLon - padLon
        )
        map.zoomToBoundingBox(padded, true)
    }

    

    

    // Удален резервный прямой маршрут с сохранением

    private fun isPointsClose(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Boolean {
        val distance = GeoPoint(lat1, lon1).distanceToAsDouble(GeoPoint(lat2, lon2))
        return distance < 5000
    }

    

    

    // Резервный OSRM удален

    private fun showStraightLineRoute(start: GeoPoint, end: GeoPoint) {
        clearRoute()

        val points = listOf(start, end)
        currentRoutePoints = points

        val routeLine = Polyline().apply {
            setPoints(points)
            outlinePaint.color = Color.parseColor("#FF9800")
            outlinePaint.strokeWidth = 8.0f
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
            outlinePaint.isAntiAlias = true
            outlinePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }

        map.overlays.add(routeLine)
        map.invalidate()

        val distance = start.distanceToAsDouble(end) / 1000
        val estimatedTimeMinutes = (distance * 1.8).toInt()

        val hours = estimatedTimeMinutes / 60
        val minutes = estimatedTimeMinutes % 60

        val timeText = if (hours > 0) {
            "${hours} ч ${minutes} мин"
        } else {
            "${minutes} мин"
        }

        currentRouteDistance = distance
        currentRouteDuration = "~$timeText"

        updateRouteInfo("${"%.1f".format(distance)} км", "~$timeText")

        Toast.makeText(this, "Прямой онлайн-маршрут построен", Toast.LENGTH_LONG).show()
    }

    

    private fun updateRouteInfo(distance: String, time: String) {
        val infoText = if (time.isNotEmpty()) {
            "Расстояние: $distance\nВремя: $time"
        } else {
            distance
        }
        tvRouteInfo.text = infoText
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        return connectivityManager?.activeNetworkInfo?.isConnected == true
    }

    private fun setupOSMDroidConfig() {
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath = cacheDir
        Configuration.getInstance().osmdroidTileCache = File(cacheDir, "tiles")
        
        // Включаем кэширование тайлов для офлайн режима
        Configuration.getInstance().cacheMapTileCount = 10000
        Configuration.getInstance().cacheMapTileOvershoot = 16
        
        // Создаем папку для тайлов если её нет
        val tilesDir = File(cacheDir, "tiles")
        if (!tilesDir.exists()) {
            tilesDir.mkdirs()
        }
    }

    private fun initMap() {
        map = findViewById(R.id.map)
        map.setUseDataConnection(true)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(false)
        map.minZoomLevel = 3.0
        map.maxZoomLevel = 18.0
        map.isTilesScaledToDpi = true
        val yakutiaBounds = org.osmdroid.util.BoundingBox(75.0, 162.0, 55.0, 105.0)
        map.setScrollableAreaLimitDouble(yakutiaBounds)
    }

    

    private fun addPointsOfInterest() {
        markers.clear()

        MarkersCatalog.markers.forEach { def ->
            val marker = Marker(map)
            marker.position = def.point
            marker.title = def.title
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            val icon = ContextCompat.getDrawable(this, def.iconResId)
            val scaledIcon = resizeDrawable(icon, def.width, def.height)
            marker.icon = scaledIcon

            marker.setOnMarkerClickListener { m, _ ->
                showMarkerInfo(m.title ?: "Нет информации")
                currentEndPoint = def.point
                Toast.makeText(this@MainActivity, "Конечная точка установлена: ${m.title?.substringBefore("\n") ?: "это место"}", Toast.LENGTH_LONG).show()
                true
            }

            map.overlays.add(marker)
            markers.add(marker)
        }

        if (!isLocationReady) {
            val centerPoint = GeoPoint(60.9, 121.0)
            map.controller.setZoom(8.0)
            map.controller.setCenter(centerPoint)
        }
        map.invalidate()
    }

    private fun showMarkerInfo(info: String) {
        val parts = info.split("\n")
        val title = parts.getOrNull(0) ?: "Место"
        val description = parts.getOrNull(1) ?: "Нет описания"
        val details = parts.getOrNull(2) ?: ""

        val message = if (details.isNotEmpty()) {
            "$description\n\n$details"
        } else {
            description
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun resizeDrawable(drawable: Drawable?, width: Int, height: Int): Drawable? {
        if (drawable == null) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)

        return BitmapDrawable(resources, bitmap)
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun enableMyLocation() {
        if (checkLocationPermission()) {
            locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
            locationOverlay.enableMyLocation()
            locationOverlay.enableFollowLocation()
            map.overlays.add(locationOverlay)

            val proximityChecker = object : Runnable {
                override fun run() {
                    try {
                        locationOverlay.myLocation?.let { current ->
                            startPoint = current
                        }
                    } catch (_: Exception) { }
                    checkAudioZones()
                    updateRouteProgress()
                    proximityHandler.postDelayed(this, PROXIMITY_CHECK_INTERVAL)
                }
            }
            proximityHandler.postDelayed(proximityChecker, PROXIMITY_CHECK_INTERVAL)

            locationOverlay.runOnFirstFix {
                runOnUiThread {
                    startPoint = locationOverlay.myLocation
                    startPoint?.let {
                        isLocationReady = true
                        map.controller.setCenter(it)
                        map.controller.setZoom(15.0)

                        btnBuildRoute.isEnabled = true
                        updateRouteInfo("Местоположение определено. Выберите точку назначения.", "")

                        currentEndPoint = lenskieStolbyPoint
                        buildOfflineRoute(it, lenskieStolbyPoint)
                    }
                }
            }
        }
    }

    

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                } else {
                    Toast.makeText(this, "Для построения маршрутов необходимо разрешение на доступ к местоположению", Toast.LENGTH_LONG).show()
                    updateRouteInfo("Разрешение на местоположение не предоставлено", "")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        // Останавливаем аудио при паузе приложения
        stopAudio()
        stopPeriodicReroute()
    }

    override fun onDestroy() {
        super.onDestroy()
        proximityHandler.removeCallbacksAndMessages(null)
        // Останавливаем аудио при уничтожении активности
        stopAudio()
        stopPeriodicReroute()
    }

    private fun startPeriodicReroute() {
        rerouteJob?.cancel()
        rerouteJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                try {
                    val cur = locationOverlay.myLocation ?: startPoint
                    val end = currentEndPoint
                    if (cur != null && end != null) {
                        val pts = withContext(Dispatchers.IO) { brouterEngine.routeCar(cur, end) }
                        if (pts.isNotEmpty()) {
                            updateRemainingRoute(pts)
                        }
                    }
                } catch (_: Exception) {
                }
                delay(ROUTE_UPDATE_INTERVAL)
            }
        }
    }

    private fun stopPeriodicReroute() {
        rerouteJob?.cancel()
        rerouteJob = null
    }
}