package com.example.lenskiegid

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lenskiegid.data.MarkersCatalog
import com.example.lenskiegid.data.PointsCatalog
import com.example.lenskiegid.routing.BRouterEngine
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var btnBuildRoute: Button
    private lateinit var tripInfoContainer: View
    private lateinit var tvTripDurationValue: TextView
    private lateinit var tvTripArrivalValue: TextView
    private lateinit var tvTripDistanceValue: TextView
    private lateinit var btnClearRoute: Button
    private lateinit var btnLogout: Button
    private lateinit var btnPlayAudio: Button
    private lateinit var tvRouteInfo: TextView
    private lateinit var tvAudioInfo: TextView
    private lateinit var btnMenu: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var btnLocate: Button
    private lateinit var btnDownloadYakutia: Button
    private lateinit var segmentsProgressPanel: android.widget.LinearLayout
    private lateinit var progressSegments: android.widget.ProgressBar
    private lateinit var tvSegmentsStatus: TextView
    private lateinit var btnTogglePlayer: ImageButton
    private lateinit var audioPlayerPanel: View
    private lateinit var btnPlayerPlayPause: ImageButton
    private lateinit var btnPlayerStop: ImageButton
    private lateinit var btnPlayerRewind: ImageButton
    private lateinit var btnPlayerForward: ImageButton
    private lateinit var btnPlayerRestart: ImageButton
    private lateinit var tvPlayerTitle: TextView
    private lateinit var tvPlayerElapsed: TextView
    private lateinit var tvPlayerDuration: TextView
    private lateinit var playerProgress: android.widget.ProgressBar
    // simple offline banner
    private lateinit var topBannerContainer: android.widget.LinearLayout
    private lateinit var btnBannerDownload: Button
    private lateinit var settingsPrefs: SharedPreferences
    private var isAudioGuideEnabled = true
    private var isAudioAutoMode = true
    private var lastAutoPlayedZone: String? = null
    private var startPoint: GeoPoint? = null
    private val markers = mutableListOf<Marker>()
    private val audioZones = mutableListOf<Polygon>()
    private val audioZoneInfoMap = mutableMapOf<Polygon, AudioZoneInfo>()
    private var audioZonesAdded = false
    private var currentRoutePoints: List<GeoPoint> = emptyList()
    

    

    private val lenskieStolbyPoint = GeoPoint(61.096667, 127.348333)

    private lateinit var proximityHandler: Handler
    private var proximityChecker: Runnable? = null
    private var lastProximityCheckTime = 0L
    private val PROXIMITY_CHECK_INTERVAL = 100L
    private val REROUTE_MIN_MOVE_METERS = 1.0
    private val REROUTE_DISTANCE_THRESHOLD = 1.0
    private var lastRerouteLocation: GeoPoint? = null

    private var mediaPlayer: MediaPlayer? = null
    private var isAudioPlaying = false
    private var currentAudioZone: String? = null
    private var currentAudioDistanceText: String = ""

    private var isLocationReady = false
    private var fullRoutePoints: List<GeoPoint> = emptyList()
    private var lastRouteUpdateIndex = 0
    private val ROUTE_UPDATE_INTERVAL = 250L
    private var rerouteJob: Job? = null
    private var traveledRoutePoints: List<GeoPoint> = emptyList()
    private var isPlayerPanelVisible = false
    private val playerProgressHandler = Handler(Looper.getMainLooper())
    private var playerProgressRunnable: Runnable? = null
    private var isAudioPrepared = false
    private var shouldStartAfterPrepare = false
    private var lastPreparedDurationMs = 0
    private var lastPreparedZone: String? = null
    private val locationWindow = ArrayDeque<TimedLocation>()
    private val MAX_LOCATION_WINDOW = 5
    private val MAX_INSTANT_SPEED_MS = 5.0 // м/с, всё что быстрее считаем спайком
    private var lastProgressValue = 0
    private var locationTrackingStarted = false

 

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

        settingsPrefs = getSharedPreferences("navigation_settings", MODE_PRIVATE)
        loadNavigationSettings()

        tvRouteInfo = findViewById(R.id.tvRouteInfo)
        tvAudioInfo = findViewById(R.id.tvAudioInfo)
        btnBuildRoute = findViewById(R.id.btnBuildRoute)
        btnClearRoute = findViewById(R.id.btnClearRoute)
        btnLogout = findViewById(R.id.btnLogout)
        btnPlayAudio = findViewById(R.id.btnPlayAudio)
        tripInfoContainer = findViewById(R.id.tripInfoContainer)
        tvTripDurationValue = findViewById(R.id.tvTripDurationValue)
        tvTripArrivalValue = findViewById(R.id.tvTripArrivalValue)
        tvTripDistanceValue = findViewById(R.id.tvTripDistanceValue)
        btnMenu = findViewById(R.id.btnMenu)
        btnHome = findViewById(R.id.btnHome)
        btnLocate = findViewById(R.id.btnLocate)
        btnDownloadYakutia = findViewById(R.id.btnDownloadYakutia)
        segmentsProgressPanel = findViewById(R.id.segmentsProgressPanel)
        progressSegments = findViewById(R.id.progressSegments)
        tvSegmentsStatus = findViewById(R.id.tvSegmentsStatus)
        btnTogglePlayer = findViewById(R.id.btnTogglePlayer)
        audioPlayerPanel = findViewById(R.id.audioPlayerPanel)
        btnPlayerPlayPause = findViewById(R.id.btnPlayerPlayPause)
        btnPlayerStop = findViewById(R.id.btnPlayerStop)
        btnPlayerRewind = findViewById(R.id.btnPlayerRewind)
        btnPlayerForward = findViewById(R.id.btnPlayerForward)
        btnPlayerRestart = findViewById(R.id.btnPlayerRestart)
        tvPlayerTitle = findViewById(R.id.tvPlayerTitle)
        tvPlayerElapsed = findViewById(R.id.tvPlayerElapsed)
        tvPlayerDuration = findViewById(R.id.tvPlayerDuration)
        playerProgress = findViewById(R.id.playerProgress)
        // banner views
        topBannerContainer = findViewById(R.id.topBannerContainer)
        btnBannerDownload = findViewById(R.id.btnBannerDownload)

        btnMenu.setOnClickListener { showNavigationMenu() }
        btnHome.setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java))
        }
        setupPlayerPanel()

        proximityHandler = Handler(Looper.getMainLooper())

        setupOSMDroidConfig()
        initMap()
        setupButtons()
        setupAudioButton()
        brouterEngine = BRouterEngine(this)
        setupYakutiaDownloadButton()
        setupOfflineBannerSimple()
        setupLocateButton()
        // refresh banner strictly by presence of rd5 files
        refreshOfflineBannerSimple()
        try {
            addAudioZones()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        addPointsOfInterest()
        ensureLocationTracking()

        updateRouteInfo("", "")
        btnBuildRoute.isEnabled = false
        updateAudioInfo()
    }

    private fun setupOfflineBannerSimple() {
        btnBannerDownload.setOnClickListener {
            // Use existing download flow
            btnDownloadYakutia.performClick()
        }
    }

    private fun refreshOfflineBannerSimple() {
        if (shouldShowOfflineBannerSimple()) {
            showOfflineBannerSimple()
        } else {
            hideOfflineBannerSimple()
        }
    }

    private fun loadNavigationSettings() {
        isAudioGuideEnabled = settingsPrefs.getBoolean("audio_enabled", true)
        isAudioAutoMode = settingsPrefs.getBoolean("audio_auto", true)
    }

    private fun showNavigationMenu() {
        val dialog = BottomSheetDialog(this)
        val content = layoutInflater.inflate(R.layout.bottom_sheet_navigation_menu, null)
        val audioSwitch = content.findViewById<SwitchCompat>(R.id.switchAudioGuide)
        val autoSwitch = content.findViewById<SwitchCompat>(R.id.switchAutoAudio)

        audioSwitch.isChecked = isAudioGuideEnabled
        autoSwitch.isChecked = isAudioAutoMode
        autoSwitch.isEnabled = isAudioGuideEnabled

        audioSwitch.setOnCheckedChangeListener { _, isChecked ->
            isAudioGuideEnabled = isChecked
            settingsPrefs.edit().putBoolean("audio_enabled", isChecked).apply()
            autoSwitch.isEnabled = isChecked
            if (!isChecked) {
                stopAudio()
                lastAutoPlayedZone = null
            }
            updateAudioButton()
            updateAudioInfo()
        }

        autoSwitch.setOnCheckedChangeListener { _, isChecked ->
            isAudioAutoMode = isChecked
            settingsPrefs.edit().putBoolean("audio_auto", isChecked).apply()
        }

        dialog.setContentView(content)
        dialog.show()
    }

    private fun shouldShowOfflineBannerSimple(): Boolean {
        return try {
            val dir = brouterEngine.segmentsPath()
            val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".rd5") }
            files == null || files.isEmpty()
        } catch (_: Exception) { true }
    }

    private fun showOfflineBannerSimple() {
        runOnUiThread { topBannerContainer.visibility = android.view.View.VISIBLE }
    }

    private fun hideOfflineBannerSimple() {
        runOnUiThread { topBannerContainer.visibility = android.view.View.GONE }
    }

    private fun setupButtons() {
        btnBuildRoute.setOnClickListener {
            if (!isLocationReady) {
                return@setOnClickListener
            }

            if (startPoint != null) {
                buildOfflineRoute(startPoint!!, lenskieStolbyPoint)
            } else {
                return@setOnClickListener
            }
        }

        btnClearRoute.setOnClickListener {
            clearRoute()
        }

        btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun setupLocateButton() {
        btnLocate.setOnClickListener {
            if (!checkLocationPermission()) {
                requestLocationPermission()
                return@setOnClickListener
            }

            if (!::locationOverlay.isInitialized) {
                enableMyLocation()
            }

            val target = if (::locationOverlay.isInitialized) {
                locationOverlay.myLocation ?: startPoint
            } else {
                startPoint
            }

            if (target != null) {
                if (::locationOverlay.isInitialized) {
                    try {
                        locationOverlay.enableFollowLocation()
                    } catch (_: Exception) { }
                }
                map.controller.animateTo(target)
                if (map.zoomLevelDouble < 15.0) {
                    map.controller.setZoom(15.0)
                }
            } else {
                Toast.makeText(this, "Местоположение пока недоступно", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildOfflineRoute(start: GeoPoint, end: GeoPoint) {
        updateRouteInfo("", "")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val points = withContext(Dispatchers.IO) {
                    brouterEngine.routeCar(start, end)
                }
                if (points.isNotEmpty()) {
                    displayRoute(points)
                    val distanceKm = calculateRouteDistance(points)
                    val estimate = calculateEstimatedTime(distanceKm)
                    updateRouteInfo("${"%.1f".format(distanceKm)} км", estimate.formatted, estimate.minutes)
                } else {
                    updateRouteInfo("Маршрут не построен", "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = e.message ?: "Ошибка оффлайн-маршрутизации"
                updateRouteInfo("Ошибка", "")
            }
        }
    }

    // аудио-кнопка
    private fun setupAudioButton() {
        btnPlayAudio.setOnClickListener {
            if (!isAudioGuideEnabled) {
                return@setOnClickListener
            }
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
                    tvSegmentsStatus.visibility = android.view.View.GONE

                    withContext(Dispatchers.IO) {
                        com.example.lenskiegid.routing.SegmentsDownloader.downloadYakutia(
                            this@MainActivity,
                            brouterEngine.segmentsPath(),
                            onProgress = { current, total, name ->
                                val percent = if (total > 0) (current * 100 / total) else 100
                                runOnUiThread {
                                    progressSegments.progress = percent
                                    
                                }
                            }
                        )
                    }
                    // After download, re-check presence and update banner
                    refreshOfflineBannerSimple()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    segmentsProgressPanel.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun playCurrentZoneAudio() {
        if (!isAudioGuideEnabled) {
            return
        }
        currentAudioZone?.let { zoneName ->
            lastAutoPlayedZone = zoneName
            val poi = PointsCatalog.findByName(zoneName)
            val audioResource = poi?.audioResId
            if (audioResource != null) {
                prepareAudio(zoneName, audioResource, startAfterPrepare = true)
            } else {
                return
            }
        } ?: run {
            return
        }
    }

    private fun prepareAudio(zoneName: String, audioResource: Int, startAfterPrepare: Boolean) {
        try {
            if (mediaPlayer != null && isAudioPrepared && currentAudioZone == zoneName) {
                if (startAfterPrepare && !isAudioPlaying) {
                    mediaPlayer?.start()
                    isAudioPlaying = true
                    updateAudioButton()
                    updatePlayerControls()
                    startProgressUpdates()
                }
                return
            }

            stopAudio()
            shouldStartAfterPrepare = startAfterPrepare
            isAudioPrepared = false
            lastPreparedDurationMs = 0
            lastPreparedZone = zoneName

        mediaPlayer = MediaPlayer().apply {
            val afd = resources.openRawResourceFd(audioResource)
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()

                setOnPreparedListener {
                    isAudioPrepared = true
                    setupProgressUi(this)
                    lastPreparedDurationMs = this.duration.coerceAtLeast(0)
                    lastPreparedZone = zoneName
                    lastProgressValue = 0
                    if (shouldStartAfterPrepare) {
                        start()
                        isAudioPlaying = true
                        startProgressUpdates()
                    } else {
                        isAudioPlaying = false
                    }
                    updateAudioButton()
                    updatePlayerControls()
                    refreshPlayerInfoPanel()
                }

                setOnCompletionListener {
                    // ensure resources are released
                    stopAudio()
                }

                setOnErrorListener { _, _, _ ->
                    // ensure resources are released on error
                    stopAudio()
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        isAudioPrepared = false
        shouldStartAfterPrepare = false
        isAudioPlaying = false
        lastProgressValue = 0
        updateAudioButton()
        updatePlayerControls()
        stopProgressUpdates()
        refreshPlayerInfoPanel()
    }

    private fun updateAudioButton() {
        runOnUiThread {
            btnPlayAudio.text = ""
            if (!isAudioGuideEnabled) {
                btnPlayAudio.isEnabled = false
                btnPlayAudio.setBackgroundResource(R.drawable.rounded_button_audio)
                return@runOnUiThread
            }
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
            // текстовый индикатор над панелью скрыт по дизайну
            tvAudioInfo.visibility = View.GONE
        }
        refreshPlayerInfoPanel()
        updatePlayerControls()
    }

    private fun setupPlayerPanel() {
        audioPlayerPanel.visibility = View.GONE
        isPlayerPanelVisible = false
        updateTogglePlayerIcon()
        btnTogglePlayer.setOnClickListener { togglePlayerPanel() }
        btnPlayerPlayPause.setOnClickListener { togglePlayPause() }
        btnPlayerStop.setOnClickListener {
            stopAudio()
            refreshPlayerInfoPanel()
        }
        btnPlayerRewind.setOnClickListener { seekAudioBy(-10000) }
        btnPlayerForward.setOnClickListener { seekAudioBy(10000) }
        btnPlayerRestart.setOnClickListener { seekToStart() }
    }

    private fun togglePlayerPanel() {
        val shouldShow = audioPlayerPanel.visibility != View.VISIBLE
        isPlayerPanelVisible = shouldShow
        animatePlayerPanelVisibility(shouldShow)
        if (shouldShow) {
            refreshPlayerInfoPanel()
            updatePlayerControls()
        }
        updateTogglePlayerIcon()
    }

    private fun animatePlayerPanelVisibility(shouldShow: Boolean) {
        val offset = dpToPx(24f)
        audioPlayerPanel.animate().cancel()
        if (shouldShow) {
            audioPlayerPanel.alpha = 0f
            audioPlayerPanel.translationY = offset
            audioPlayerPanel.visibility = View.VISIBLE
            audioPlayerPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .setListener(null)
                .start()
        } else {
            audioPlayerPanel.animate()
                .alpha(0f)
                .translationY(offset)
                .setDuration(180)
                .setInterpolator(DecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        audioPlayerPanel.visibility = View.GONE
                        audioPlayerPanel.alpha = 1f
                        audioPlayerPanel.translationY = 0f
                    }
                })
                .start()
        }
    }

    private fun crossfadeRouteAction(showTripInfo: Boolean) {
        val fadeIn = 200L
        val fadeOut = 160L
        btnBuildRoute.animate().cancel()
        tripInfoContainer.animate().cancel()

        if (showTripInfo) {
            if (tripInfoContainer.visibility != View.VISIBLE) {
                tripInfoContainer.alpha = 0f
                tripInfoContainer.visibility = View.VISIBLE
            }
            btnBuildRoute.animate()
                .alpha(0f)
                .setDuration(fadeOut)
                .setInterpolator(DecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        btnBuildRoute.visibility = View.GONE
                        btnBuildRoute.alpha = 1f
                    }
                })
                .start()
            tripInfoContainer.animate()
                .alpha(1f)
                .setDuration(fadeIn)
                .setInterpolator(DecelerateInterpolator())
                .setListener(null)
                .start()
        } else {
            btnBuildRoute.visibility = View.VISIBLE
            btnBuildRoute.alpha = 0f
            btnBuildRoute.animate()
                .alpha(1f)
                .setDuration(fadeIn)
                .setInterpolator(DecelerateInterpolator())
                .setListener(null)
                .start()
            tripInfoContainer.animate()
                .alpha(0f)
                .setDuration(fadeOut)
                .setInterpolator(DecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        tripInfoContainer.visibility = View.GONE
                        tripInfoContainer.alpha = 1f
                    }
                })
                .start()
        }
    }

    private fun updateTripInfo(distance: String, time: String, arrival: String?) {
        val distanceValue = distance.replace("км", "").trim().ifEmpty { "--" }
        tvTripDurationValue.text = time.ifEmpty { "--:--" }
        tvTripArrivalValue.text = arrival?.ifEmpty { "--:--" } ?: "--:--"
        tvTripDistanceValue.text = distanceValue
    }

    private fun resetTripInfoText() {
        tvTripDurationValue.text = "--:--"
        tvTripArrivalValue.text = "--:--"
        tvTripDistanceValue.text = "--"
    }


    private fun refreshPlayerInfoPanel() {
        runOnUiThread {
            val hasZone = currentAudioZone != null
            val distance = if (currentAudioDistanceText.isNotEmpty() && hasZone) " • $currentAudioDistanceText" else ""
            tvPlayerTitle.text = if (hasZone) currentAudioZone else "Нет аудиозоны"
            if (distance.isNotEmpty()) {
                tvPlayerTitle.text = "${tvPlayerTitle.text}$distance"
            }

            val player = mediaPlayer
            if (player != null && isAudioPrepared) {
                val dur = player.duration.coerceAtLeast(0)
                val pos = player.currentPosition.coerceIn(0, dur)
                playerProgress.max = dur.coerceAtLeast(1)
                lastProgressValue = pos
                playerProgress.progress = pos
                tvPlayerElapsed.text = formatMillis(pos)
                tvPlayerDuration.text = formatMillis(dur)
            } else if (lastPreparedDurationMs > 0 && currentAudioZone == lastPreparedZone) {
                val dur = lastPreparedDurationMs
                playerProgress.max = dur
                playerProgress.progress = 0
                tvPlayerElapsed.text = "0:00"
                tvPlayerDuration.text = formatMillis(dur)
            } else {
                tvPlayerElapsed.text = "0:00"
                tvPlayerDuration.text = "0:00"
                playerProgress.progress = 0
                playerProgress.max = 100
            }
        }
    }

    private fun updatePlayerControls() {
        runOnUiThread {
            val hasZone = currentAudioZone != null && isAudioGuideEnabled
            val hasPlayer = mediaPlayer != null && isAudioPrepared
            btnPlayerPlayPause.isEnabled = hasZone
            btnPlayerStop.isEnabled = hasPlayer
            btnPlayerRewind.isEnabled = hasPlayer
            btnPlayerForward.isEnabled = hasPlayer
            btnPlayerRestart.isEnabled = hasPlayer
            btnPlayerPlayPause.setImageResource(if (isAudioPlaying) R.drawable.ic_pause else R.drawable.ic_play_vector)
        }
    }

    private fun togglePlayPause() {
        if (!isAudioGuideEnabled) return
        val player = mediaPlayer
        if (player != null && isAudioPlaying) {
            try {
                player.pause()
                isAudioPlaying = false
            } catch (_: Exception) { }
            updateAudioButton()
            updatePlayerControls()
            return
        }
        playCurrentZoneAudio()
    }

    private fun seekAudioBy(millis: Int) {
        val player = mediaPlayer ?: return
        try {
            val duration = if (player.duration > 0) player.duration else 0
            val newPos = (player.currentPosition + millis).coerceIn(0, duration)
            player.seekTo(newPos)
            refreshPlayerInfoPanel()
        } catch (_: Exception) { }
    }

    private fun seekToStart() {
        val player = mediaPlayer ?: return
        try {
            player.seekTo(0)
            refreshPlayerInfoPanel()
        } catch (_: Exception) { }
    }

    private fun setupProgressUi(player: MediaPlayer) {
        val duration = player.duration.coerceAtLeast(0)
        playerProgress.max = duration.coerceAtLeast(1)
        lastProgressValue = 0
        playerProgress.progress = 0
        tvPlayerDuration.text = formatMillis(duration)
    }

    private fun startProgressUpdates() {
        playerProgressRunnable?.let { playerProgressHandler.removeCallbacks(it) }
        playerProgressRunnable = object : Runnable {
            override fun run() {
                val player = mediaPlayer
                if (player != null) {
                    val dur = player.duration.coerceAtLeast(1)
                    val pos = player.currentPosition.coerceIn(0, dur)
                    playerProgress.max = dur
                    setProgressSmooth(pos)
                    tvPlayerElapsed.text = formatMillis(pos)
                    tvPlayerDuration.text = formatMillis(dur)
                }
                playerProgressHandler.postDelayed(this, 150)
            }
        }
        playerProgressHandler.post(playerProgressRunnable!!)
    }

    private fun stopProgressUpdates() {
        playerProgressRunnable?.let { playerProgressHandler.removeCallbacks(it) }
        playerProgressRunnable = null
    }

    private fun setProgressSmooth(target: Int) {
        val safe = target.coerceAtLeast(0)
        lastProgressValue = safe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            playerProgress.setProgress(safe, true)
        } else {
            playerProgress.progress = safe
        }
    }

    private fun formatMillis(millis: Int): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    private fun updateTogglePlayerIcon() {
        runOnUiThread {
            btnTogglePlayer.setImageResource(if (isPlayerPanelVisible) R.drawable.ic_clear else R.drawable.ic_audio)
        }
    }

    private data class TimedLocation(val point: GeoPoint, val timeMillis: Long)

    private fun addSmoothedLocation(raw: GeoPoint): GeoPoint? {
        val now = System.currentTimeMillis()
        val last = locationWindow.lastOrNull()
        if (last != null) {
            val dt = (now - last.timeMillis).coerceAtLeast(1)
            val dist = raw.distanceToAsDouble(last.point)
            val speed = dist / dt * 1000.0
            if (speed > MAX_INSTANT_SPEED_MS) {
                // Спайк — игнорируем новый пункт
                return currentSmoothedLocation()
            }
        }
        locationWindow.addLast(TimedLocation(raw, now))
        while (locationWindow.size > MAX_LOCATION_WINDOW) {
            locationWindow.removeFirst()
        }
        return currentSmoothedLocation()
    }

    private fun currentSmoothedLocation(): GeoPoint? {
        if (locationWindow.isEmpty()) return null
        var latSum = 0.0
        var lonSum = 0.0
        locationWindow.forEach {
            latSum += it.point.latitude
            lonSum += it.point.longitude
        }
        val cnt = locationWindow.size
        return GeoPoint(latSum / cnt, lonSum / cnt)
    }

    private fun dpToPx(value: Float): Float = value * resources.displayMetrics.density

 

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

        if (foundZone == null) {
            lastAutoPlayedZone = null
            lastPreparedDurationMs = 0
            lastPreparedZone = null
        }
        val previousZone = currentAudioZone
        currentAudioZone = foundZone
        if (foundZone != null && foundZone != previousZone && isAudioGuideEnabled) {
            PointsCatalog.findByName(foundZone)?.audioResId?.let { resId ->
                prepareAudio(foundZone, resId, startAfterPrepare = false)
            }
        }
        if (foundZone != null && isAudioGuideEnabled && isAudioAutoMode && lastAutoPlayedZone != foundZone && !isAudioPlaying) {
            lastAutoPlayedZone = foundZone
            playCurrentZoneAudio()
        }
        updateAudioInfo()
        updateAudioButton()

        if (foundZone == null && isAudioPlaying) {
            stopAudio()
        }

        lastProximityCheckTime = currentTime
    }
    
    // прогресс движения
    private fun updateRouteProgress() {
        try {
            if (fullRoutePoints.isEmpty() || startPoint == null || lastRouteUpdateIndex >= fullRoutePoints.size) {
                return
            }

            val currentLocation = startPoint!!
            var closestIndex = lastRouteUpdateIndex
            var minDistance = Double.MAX_VALUE
            val searchRange = minOf(50, fullRoutePoints.size - lastRouteUpdateIndex)

            // Search only in a limited range around the last update index for better performance
            val startIndex = maxOf(0, lastRouteUpdateIndex - 5)
            val endIndex = minOf(fullRoutePoints.size, lastRouteUpdateIndex + searchRange)
            
            for (i in startIndex until endIndex) {
                val distance = currentLocation.distanceToAsDouble(fullRoutePoints[i])
                if (distance < minDistance) {
                    minDistance = distance
                    closestIndex = i
                }
            }

            if (minDistance < REROUTE_MIN_MOVE_METERS && closestIndex > lastRouteUpdateIndex) {
                lastRouteUpdateIndex = closestIndex
                if (lastRouteUpdateIndex < fullRoutePoints.size) {
                    val remainingRoute = fullRoutePoints.subList(lastRouteUpdateIndex, fullRoutePoints.size)
                    runOnUiThread {
                        try {
                            updateRemainingRoute(remainingRoute)
                        } catch (e: Exception) {
                            Log.e("RouteUpdate", "Error in updateRemainingRoute", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RouteUpdate", "Error in updateRouteProgress", e)
        }
    }
    
    private fun updateRemainingRoute(remainingPoints: List<GeoPoint>) {
        try {
            if (!::map.isInitialized || !::locationOverlay.isInitialized) {
                return
            }

            runOnUiThread {
                try {
                    val locationOverlayIndex = map.overlays.indexOf(locationOverlay).takeIf { it >= 0 } ?: 0
                    val current = locationOverlay.myLocation ?: startPoint
                    if (current == null) return@runOnUiThread
                    // prepend current position to avoid “tail” lag behind GPS
                    val adjustedRemaining = buildList {
                        add(current)
                        addAll(remainingPoints)
                    }

                    // Remove existing route polylines
                    val routeToRemove = map.overlays
                        .filterIsInstance<Polyline>()
                        .filter { it !== locationOverlay }
                    
                    routeToRemove.forEach { map.overlays.remove(it) }

                    // Add remaining route (blue)
                    if (adjustedRemaining.size > 1) {
                        val remainingLine = Polyline().apply {
                            setPoints(adjustedRemaining)
                            outlinePaint.color = Color.parseColor("#1976D2")
                            outlinePaint.strokeWidth = 15.0f
                            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                            outlinePaint.isAntiAlias = true
                        }
                        map.overlays.add(locationOverlayIndex, remainingLine)
                    }

                    // Keep GPS overlay above route lines
                    if (map.overlays.contains(locationOverlay)) {
                        map.overlays.remove(locationOverlay)
                        map.overlays.add(locationOverlay)
                    }

                    traveledRoutePoints = emptyList()
                    currentRoutePoints = adjustedRemaining

                    // Update route info
                    val distance = calculateRouteDistance(adjustedRemaining)
                    val estimate = calculateEstimatedTime(distance)
                    updateRouteInfo("%.1f км".format(distance), estimate.formatted, estimate.minutes)
                    
                    map.invalidate()
                } catch (e: Exception) {
                    Log.e("RouteUpdate", "Error updating route UI", e)
                }
            }
        } catch (e: Exception) {
            Log.e("RouteUpdate", "Error in updateRemainingRoute", e)
        }
    }

    private fun showZoneNotification(zoneName: String, distance: Double) {
        // уведомления отключены по требованию
    }

    private fun logoutUser() {
        stopAudio()

        val editor = getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
        editor.clear()
        editor.apply()

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()

    }

    private fun clearRoute() {
        map.overlays.removeAll { it is Polyline }
        currentRoutePoints = emptyList()
        fullRoutePoints = emptyList()
        lastRouteUpdateIndex = 0
        traveledRoutePoints = emptyList()
        map.invalidate()
        updateRouteInfo("Маршрут не построен", "")
    }

    // маршрут онлайн
    
    
    
    
    
    
    // Получить имя маркера
    
    
    // Отобразить маршрут на карте
    private fun displayRoute(points: List<GeoPoint>) {
        clearRoute()

        val remainingLine = Polyline().apply {
            setPoints(points)
            outlinePaint.color = Color.parseColor("#1976D2")
            outlinePaint.strokeWidth = 15.0f
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
            outlinePaint.isAntiAlias = true
        }

        map.overlays.add(remainingLine)
        // Keep GPS overlay above route lines on initial draw
        if (::locationOverlay.isInitialized && map.overlays.contains(locationOverlay)) {
            map.overlays.remove(locationOverlay)
            map.overlays.add(locationOverlay)
        }
        currentRoutePoints = points
        fullRoutePoints = points.toList()
        lastRouteUpdateIndex = 0
        traveledRoutePoints = emptyList()
        
        map.invalidate()
        fitToPoints(points)
        startPeriodicReroute()
    }

    // Автоматически загружает маршрут до Ленских столбов при старте
    

    

    

    private fun calculateRouteDistance(points: List<GeoPoint>): Double {
        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += points[i].distanceToAsDouble(points[i + 1])
        }
        return totalDistance / 1000
    }

    data class TravelEstimate(val formatted: String, val minutes: Int)

    private fun calculateEstimatedTime(distanceKm: Double): TravelEstimate {
        val averageSpeed = 60.0
        val totalMinutes = (distanceKm / averageSpeed * 60).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val label = if (hours > 0) {
            "${hours} ч ${minutes} мин"
        } else {
            "${minutes} мин"
        }
        return TravelEstimate(label, totalMinutes)
    }

    private fun formatArrivalTime(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "--:--"
        val millis = System.currentTimeMillis() + totalMinutes * 60_000L
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(millis))
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

    // Резервный OSRM удален

    

    

    private fun updateRouteInfo(distance: String, time: String, travelMinutes: Int? = null) {
        if (distance.isEmpty() && time.isEmpty()) {
            tvRouteInfo.visibility = View.GONE
            crossfadeRouteAction(showTripInfo = false)
            resetTripInfoText()
            return
        }
        if (time.isEmpty()) {
            tvRouteInfo.visibility = View.VISIBLE
            tvRouteInfo.text = distance
            crossfadeRouteAction(showTripInfo = false)
            resetTripInfoText()
            return
        }

        tvRouteInfo.visibility = View.GONE
        val arrivalText = travelMinutes?.let { formatArrivalTime(it) }
        updateTripInfo(distance, time, arrivalText)
        crossfadeRouteAction(showTripInfo = true)
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
                true
            }

            map.overlays.add(marker)
            markers.add(marker)
        }

        PointsCatalog.pointsOfInterest.forEach { poi ->
            try {
                if (!hasMarkerNear(poi.point, 300.0)) {
                    val marker = Marker(map)
                    marker.position = poi.point
                    marker.title = poi.name + "\nАудиозона"
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    val icon = ContextCompat.getDrawable(this, R.drawable.marker)
                    val scaledIcon = resizeDrawable(icon, 64, 94)
                    marker.icon = scaledIcon

                    marker.setOnMarkerClickListener { m, _ ->
                        showMarkerInfo(m.title ?: "Нет информации")
                        true
                    }

                    map.overlays.add(marker)
                    markers.add(marker)
                }
            } catch (_: Exception) { }
        }

        if (!isLocationReady) {
            val centerPoint = GeoPoint(60.9, 121.0)
            map.controller.setZoom(8.0)
            map.controller.setCenter(centerPoint)
        }
        map.invalidate()
    }

    private fun hasMarkerNear(point: GeoPoint, thresholdMeters: Double): Boolean {
        return try {
            markers.any { existing ->
                existing.position?.distanceToAsDouble(point) ?: Double.MAX_VALUE <= thresholdMeters
            }
        } catch (_: Exception) { false }
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

        // Slightly upscale all markers for better visibility
        val scale = 1.2
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()
        val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, scaledWidth, scaledHeight)
        drawable.draw(canvas)

        return BitmapDrawable(resources, bitmap)
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun ensureLocationTracking() {
        if (locationTrackingStarted) return
        if (checkLocationPermission()) {
            enableMyLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun enableMyLocation() {
        if (checkLocationPermission()) {
            locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
            applyCustomGpsMarkerIcon()
            locationOverlay.enableMyLocation()
            locationOverlay.enableFollowLocation()
            map.overlays.add(locationOverlay)
            locationTrackingStarted = true

            proximityChecker = object : Runnable {
                override fun run() {
                    try {
                        locationOverlay.myLocation?.let { current ->
                            addSmoothedLocation(current)?.let { smoothed ->
                                startPoint = smoothed
                            }
                        }
                    } catch (_: Exception) { }
                    checkAudioZones()
                    updateRouteProgress()
                    proximityHandler.postDelayed(this, PROXIMITY_CHECK_INTERVAL)
                }
            }
            proximityHandler.postDelayed(proximityChecker!!, PROXIMITY_CHECK_INTERVAL)

            locationOverlay.runOnFirstFix {
                runOnUiThread {
                    startPoint = locationOverlay.myLocation
                    startPoint?.let {
                        isLocationReady = true
                        map.controller.setCenter(it)
                        map.controller.setZoom(15.0)

                        btnBuildRoute.isEnabled = true
                        updateRouteInfo("", "")
                        // buildOfflineRoute(it, lenskieStolbyPoint) // авто-построение отключено, маршрут запускается вручную
                    }
                }
            }
        }
    }

    private fun applyCustomGpsMarkerIcon() {
        // Try to use a custom PNG named gps_marker.png in res/drawable
        val resId = resources.getIdentifier("gps_marker", "drawable", packageName)
        if (resId == 0) return
        val drawable = ContextCompat.getDrawable(this, resId) ?: return
        val sizePx = (32 * resources.displayMetrics.density).toInt().coerceAtLeast(24)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        locationOverlay.setPersonIcon(bitmap)
        locationOverlay.setDirectionIcon(bitmap)
        locationOverlay.setPersonHotspot(sizePx / 2f, sizePx / 2f)
    }

    

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                } else {
                    println("хуй")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ensureLocationTracking()
        map.onResume()
        // resume proximity checks if available
        proximityChecker?.let { proximityHandler.postDelayed(it, PROXIMITY_CHECK_INTERVAL) }
        // if маршрут уже был, вернуть фоновые пересчеты
        if (fullRoutePoints.isNotEmpty()) {
            lastRerouteLocation = null
            startPeriodicReroute()
        }
        // keep banner visibility in sync with presence of rd5 files
        refreshOfflineBannerSimple()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        // Останавливаем аудио при паузе приложения
        stopAudio()
        stopPeriodicReroute()
        // stop proximity checks to save battery
        proximityHandler.removeCallbacksAndMessages(null)
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
                    val end = lenskieStolbyPoint
                    if (cur != null) {
                        val shouldRecalc = lastRerouteLocation?.let { cur.distanceToAsDouble(it) > REROUTE_DISTANCE_THRESHOLD } ?: true
                        if (!shouldRecalc) {
                            delay(ROUTE_UPDATE_INTERVAL)
                            continue
                        }

                        val pts = withContext(Dispatchers.IO) { brouterEngine.routeCar(cur, end) }
                        if (pts.isNotEmpty()) {
                            lastRerouteLocation = cur
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
