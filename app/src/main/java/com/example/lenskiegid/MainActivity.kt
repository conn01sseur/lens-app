package com.example.lenskiegid

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lenskiegid.auth.AuthGate
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
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
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

class   MainActivity : BaseEdgeToEdgeActivity() {
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
    private lateinit var btnLocate: ImageButton
    private lateinit var btnZoomIn: ImageButton
    private lateinit var btnZoomOut: ImageButton
    private lateinit var zoomControls: View
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
    private lateinit var bottomPanel: View
    private lateinit var navSettingsPanel: View
    private lateinit var btnFinishRoute: Button
    private lateinit var btnGas: ImageButton
    private lateinit var btnCafe: ImageButton
    private lateinit var btnShop: ImageButton
    private lateinit var switchAudioGuide: SwitchCompat
    private lateinit var switchMapMarkers: SwitchCompat
    private lateinit var switchRoadHints: SwitchCompat
    // simple offline banner
    private lateinit var topBannerContainer: android.widget.LinearLayout
    private lateinit var btnBannerDownload: Button
    private lateinit var settingsPrefs: SharedPreferences
    private var isAudioGuideEnabled = true
    private var isAudioAutoMode = true
    private var markersVisible = true
    private var isRoadHintsEnabled = true
    private var lastAutoPlayedZone: String? = null
    private var startPoint: GeoPoint? = null
    private val markers = mutableListOf<Marker>()
    private val trailViewpointMarkers = mutableListOf<Marker>()
    private val audioZones = mutableListOf<Polygon>()
    private val audioZoneInfoMap = mutableMapOf<Polygon, AudioZoneInfo>()
    private var audioZonesAdded = false
    private var currentRoutePoints: List<GeoPoint> = emptyList()
    

    

    private val lenskieStolbyPoint = GeoPoint(61.096667, 127.348333)
    private val trailStartPoint = GeoPoint(61.107111, 127.361306) // 61°06'25.6"N 127°21'40.7"E
    private val trailEndPoint = GeoPoint(61.104278, 127.356250) // 61°06'15.4"N 127°21'22.5"E
    private val trailFinishPoint = GeoPoint(61.10296705347721, 127.35374935256745)
    private val lenskieTrailRadiusMeters = 7000.0
    private val trailProgressCaptureMeters = 35.0
    private val lenskieTrailOpenZoom = 14.2
    private val trailOpenCenterLatOffset = 0.000
    private val trailOpenCenterLonOffset = 0.000
    private var trailBounds: BoundingBox? = null
    private val trailViewpoints = listOf(
        "Площадка номер 1" to GeoPoint(61.10380, 127.36123),
        "Площадка номер 2" to GeoPoint(61.09958635055314, 127.36163511181815),
        "Площадка номер 3" to GeoPoint(61.09871, 127.35859),
        "Площадка номер 4" to GeoPoint(61.09793747627536, 127.35711183034245),
        "Площадка номер 5" to GeoPoint(61.09816, 127.35553),
        "Площадка номер 6" to GeoPoint(61.09809652212155, 127.35211426565884),
        "Площадка номер 7" to GeoPoint(61.09844, 127.34946),
        "Площадка номер 8" to GeoPoint(61.09873246582848, 127.34747809095833),
        "Площадка номер 9" to GeoPoint(61.099333333333334, 127.34663888888889),
        "Часовня" to GeoPoint(61.10314303050203, 127.35354985468268),
        "Финиш" to GeoPoint(61.10296705347721, 127.35374935256745)
    )

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
    private var isNavSettingsVisible = false
    private var navPanelAnimator: ValueAnimator? = null
    private val playerProgressHandler = Handler(Looper.getMainLooper())
    private var playerProgressRunnable: Runnable? = null
    private var playerProgressAnimator: ValueAnimator? = null
    private var isAudioPrepared = false
    private var shouldStartAfterPrepare = false
    private var lastPreparedDurationMs = 0
    private var lastPreparedZone: String? = null
    private val locationWindow = ArrayDeque<TimedLocation>()
    private val MAX_LOCATION_WINDOW = 5
    private val MAX_INSTANT_SPEED_MS = 5.0 // м/с, всё что быстрее считаем спайком
    private var lastProgressValue = 0
    private var locationTrackingStarted = false

    private var trailOverlay: Polyline? = null
    private var shouldShowTrail = false
    private val lenskieOverviewZoom = 12.5

    companion object {
        const val EXTRA_SHOW_TRAIL = "show_trail"
    }
 

    private lateinit var brouterEngine: BRouterEngine

 

    // запуск и подготовка UI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (!AuthGate.isLoggedIn(sharedPreferences)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        WindowCompat.getInsetsController(window, window.decorView)?.let { controller ->
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }

        shouldShowTrail = intent?.getBooleanExtra(EXTRA_SHOW_TRAIL, false) == true

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
        btnZoomIn = findViewById(R.id.btnZoomIn)
        btnZoomOut = findViewById(R.id.btnZoomOut)
        zoomControls = findViewById(R.id.zoomControls)
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
        bottomPanel = findViewById(R.id.bottomPanel)
        navSettingsPanel = findViewById(R.id.navSettingsPanel)
        btnFinishRoute = findViewById(R.id.btnFinishRoute)
        btnGas = findViewById(R.id.btnGas)
        btnCafe = findViewById(R.id.btnCafe)
        btnShop = findViewById(R.id.btnShop)
        switchAudioGuide = findViewById(R.id.switchAudioGuide)
        switchMapMarkers = findViewById(R.id.switchMapMarkers)
        switchRoadHints = findViewById(R.id.switchRoadHints)
        // banner views
        topBannerContainer = findViewById(R.id.topBannerContainer)
        btnBannerDownload = findViewById(R.id.btnBannerDownload)
        applyTopInsetForControls()

        animateMapUi()

        btnMenu.setOnClickListener { toggleNavSettingsPanel() }
        btnHome.setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java))
        }
        setupPlayerPanel()
        setupNavSettingsPanel()

        proximityHandler = Handler(Looper.getMainLooper())

        setupOSMDroidConfig()
        initMap()
        setupButtons()
        setupAudioButton()
        brouterEngine = BRouterEngine(this)
        setupYakutiaDownloadButton()
        setupOfflineBannerSimple()
        setupLocateButton()
        setupZoomButtons()
        // refresh banner strictly by presence of rd5 files
        refreshOfflineBannerSimple()
        // В режиме "Тропа" показываем только трек + GPS, без обычных POI-маркеров.
        if (!shouldShowTrail) {
            addPointsOfInterest()
            addAudioZones()
        } else {
            markersVisible = false
        }
        ensureLocationTracking()

        if (shouldShowTrail) {
            showTrailFromAssets()
        }

        updateRouteInfo("", "")
        btnBuildRoute.isEnabled = false
        updateAudioInfo()
    }

    private fun applyTopInsetForControls() {
        val root = findViewById<View>(android.R.id.content)
        val topBannerLp = topBannerContainer.layoutParams as ViewGroup.MarginLayoutParams
        val initialTopBannerMarginTop = topBannerLp.topMargin
        val audioPanelLp = audioPlayerPanel.layoutParams as ViewGroup.MarginLayoutParams
        val initialAudioPanelMarginTop = audioPanelLp.topMargin

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            topBannerLp.topMargin = initialTopBannerMarginTop + topInset
            topBannerContainer.layoutParams = topBannerLp

            audioPanelLp.topMargin = initialAudioPanelMarginTop + topInset
            audioPlayerPanel.layoutParams = audioPanelLp

            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun animateMapUi() {
        val views = listOf(
            btnTogglePlayer,
            btnLocate,
            bottomPanel,
            tvRouteInfo
        )
        var delay = 40L
        views.forEach { v ->
            v.alpha = 0f
            v.translationY = dpToPx(18f)
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(320)
                .setInterpolator(DecelerateInterpolator())
                .start()
            delay += 90L
        }
    }

    private fun setupNavSettingsPanel() {
        navSettingsPanel.visibility = View.GONE
        isNavSettingsVisible = false

        switchAudioGuide.isChecked = isAudioGuideEnabled
        switchMapMarkers.isChecked = markersVisible
        switchRoadHints.isChecked = isRoadHintsEnabled

        switchAudioGuide.setOnCheckedChangeListener { _, isChecked ->
            isAudioGuideEnabled = isChecked
            settingsPrefs.edit().putBoolean("audio_enabled", isChecked).apply()
            if (!isChecked) {
                stopAudio()
                lastAutoPlayedZone = null
            }
            updateAudioButton()
            updateAudioInfo()
        }

        switchMapMarkers.setOnCheckedChangeListener { _, isChecked ->
            markersVisible = isChecked
            settingsPrefs.edit().putBoolean("markers_visible", isChecked).apply()
            applyMarkersVisibility()
        }

        switchRoadHints.setOnCheckedChangeListener { _, isChecked ->
            isRoadHintsEnabled = isChecked
            settingsPrefs.edit().putBoolean("road_hints", isChecked).apply()
        }

        btnGas.setOnClickListener { Toast.makeText(this, "Скоро", Toast.LENGTH_SHORT).show() }
        btnCafe.setOnClickListener { Toast.makeText(this, "Скоро", Toast.LENGTH_SHORT).show() }
        btnShop.setOnClickListener { Toast.makeText(this, "Скоро", Toast.LENGTH_SHORT).show() }

        btnFinishRoute.setOnClickListener { finishRoute() }

        updateMenuIcon()
    }

    private fun toggleNavSettingsPanel() {
        val show = navSettingsPanel.visibility != View.VISIBLE
        isNavSettingsVisible = show
        animateNavSettingsPanel(show)
        updateMenuIcon()
    }

    private fun animateNavSettingsPanel(show: Boolean) {
        navPanelAnimator?.cancel()
        val lp = navSettingsPanel.layoutParams

        if (show) {
            navSettingsPanel.visibility = View.VISIBLE
            val targetHeight = measurePanelTargetHeight()
            lp.height = 0
            navSettingsPanel.layoutParams = lp
            navSettingsPanel.alpha = 0f

            navPanelAnimator = ValueAnimator.ofInt(0, targetHeight).apply {
                duration = 280L
                interpolator = DecelerateInterpolator()
                addUpdateListener { animator ->
                    lp.height = animator.animatedValue as Int
                    navSettingsPanel.layoutParams = lp
                    val progress = animator.animatedFraction
                    navSettingsPanel.alpha = (progress * 1.05f).coerceAtMost(1f)
                    setAuxSideButtonsHiddenProgress(progress)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        lp.height = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        navSettingsPanel.layoutParams = lp
                        navSettingsPanel.alpha = 1f
                        setAuxSideButtonsHiddenProgress(1f)
                    }
                })
                start()
            }
        } else {
            val startHeight = navSettingsPanel.height.takeIf { it > 0 } ?: measurePanelTargetHeight()
            navPanelAnimator = ValueAnimator.ofInt(startHeight, 0).apply {
                duration = 260L
                interpolator = DecelerateInterpolator()
                addUpdateListener { animator ->
                    lp.height = animator.animatedValue as Int
                    navSettingsPanel.layoutParams = lp
                    val progress = animator.animatedFraction
                    navSettingsPanel.alpha = 1f - (progress * 0.95f)
                    setAuxSideButtonsHiddenProgress(1f - progress)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        navSettingsPanel.visibility = View.GONE
                        lp.height = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        navSettingsPanel.layoutParams = lp
                        navSettingsPanel.alpha = 1f
                        setAuxSideButtonsHiddenProgress(0f)
                    }
                })
                start()
            }
        }
    }

    private fun setAuxSideButtonsHiddenProgress(progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        val shift = sideButtonsShiftDistance() * p
        zoomControls.translationX = shift
        btnTogglePlayer.translationX = shift
        val alpha = 1f - p
        zoomControls.alpha = alpha
        btnTogglePlayer.alpha = alpha
        val interactive = p < 0.98f
        zoomControls.isEnabled = interactive
        zoomControls.isClickable = interactive
        btnTogglePlayer.isEnabled = interactive
        btnTogglePlayer.isClickable = interactive
    }

    private fun sideButtonsShiftDistance(): Float {
        val controlsWidth = zoomControls.width.toFloat()
        val playerWidth = btnTogglePlayer.width.toFloat()
        val base = kotlin.math.max(controlsWidth, playerWidth)
        val fallback = dpToPx(92f)
        return if (base > 0f) base + dpToPx(28f) else fallback
    }

    private fun measurePanelTargetHeight(): Int {
        val parent = navSettingsPanel.parent as? View
        val parentWidth = parent?.width ?: resources.displayMetrics.widthPixels
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        navSettingsPanel.measure(widthSpec, heightSpec)
        return navSettingsPanel.measuredHeight.coerceAtLeast(1)
    }

    private fun updateMenuIcon() {
        val icon = if (isNavSettingsVisible) R.drawable.ic_chevron_down else R.drawable.ic_menu_panel
        btnMenu.setImageResource(icon)
        updateMenuButtonSize()
    }

    private fun updateMenuButtonSize() {
        val padDp = if (isNavSettingsVisible) 10 else 14
        val padPx = dpToPx(padDp.toFloat()).toInt()
        btnMenu.setPadding(padPx, padPx, padPx, padPx)
    }

    private fun finishRoute() {
        clearRoute()
        stopPeriodicReroute()
        isNavSettingsVisible = false
        navSettingsPanel.visibility = View.GONE
        updateMenuIcon()
        setLenskieOverview()
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
        markersVisible = settingsPrefs.getBoolean("markers_visible", true)
        isRoadHintsEnabled = settingsPrefs.getBoolean("road_hints", true)
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
                if (shouldShowTrail) {
                    val distToLenskie = startPoint!!.distanceToAsDouble(lenskieStolbyPoint)
                    if (distToLenskie > lenskieTrailRadiusMeters) {
                        Toast.makeText(this, "Вы не в зоне Ленских столбов", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
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
                if (shouldShowTrail) {
                    val distanceToLenskie = target.distanceToAsDouble(lenskieStolbyPoint)
                    if (distanceToLenskie > lenskieTrailRadiusMeters) {
                        Toast.makeText(this, "Вы не в зоне Ленских столбов", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
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

    private fun setupZoomButtons() {
        btnZoomIn.setOnClickListener {
            val next = (map.zoomLevelDouble + 1.0).coerceAtMost(map.maxZoomLevel)
            map.controller.animateTo(map.mapCenter, next, 200L)
        }
        btnZoomOut.setOnClickListener {
            val minAllowed = if (shouldShowTrail) lenskieTrailOpenZoom else map.minZoomLevel
            val next = (map.zoomLevelDouble - 1.0).coerceAtLeast(minAllowed)
            map.controller.animateTo(map.mapCenter, next, 200L)
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
                    // Force camera to include full trip (current GPS -> destination).
                    val cameraPoints = buildList {
                        add(start)
                        addAll(points)
                        add(end)
                    }
                    fitToPoints(cameraPoints)
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
        btnPlayerRestart.setOnClickListener { seekAudioBy(-5000) }
        btnPlayerRewind.setOnClickListener { playAdjacentAudio(-1) }
        btnPlayerForward.setOnClickListener { playAdjacentAudio(1) }
        btnPlayerStop.setOnClickListener { seekAudioBy(5000) }
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
        val distanceValue = distance.replace("км", "").trim()
        val rounded = distanceValue.replace(",", ".").toDoubleOrNull()?.let {
            kotlin.math.round(it).toInt().toString()
        } ?: distanceValue
        tvTripDurationValue.text = time.ifEmpty { "--:--" }
        tvTripArrivalValue.text = arrival?.ifEmpty { "--:--" } ?: "--:--"
        tvTripDistanceValue.text = rounded.ifEmpty { "--" }
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
                tvPlayerElapsed.text = ""
                tvPlayerDuration.text = formatRemainingMillis(dur - pos)
            } else if (lastPreparedDurationMs > 0 && currentAudioZone == lastPreparedZone) {
                val dur = lastPreparedDurationMs
                playerProgress.max = dur
                playerProgress.progress = 0
                tvPlayerElapsed.text = ""
                tvPlayerDuration.text = formatRemainingMillis(dur)
            } else {
                tvPlayerElapsed.text = ""
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
            btnPlayerForward.isEnabled = isAudioGuideEnabled
            btnPlayerRestart.isEnabled = isAudioGuideEnabled
            btnPlayerPlayPause.setImageResource(if (isAudioPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play)
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

    private fun playAdjacentAudio(step: Int) {
        if (!isAudioGuideEnabled) return
        val audioPoints = PointsCatalog.pointsOfInterest.filter { it.audioResId != null }
        if (audioPoints.isEmpty()) return

        val currentIndex = currentAudioZone?.let { zone ->
            audioPoints.indexOfFirst { it.name == zone }
        } ?: -1
        val nextIndex = if (currentIndex < 0) {
            0
        } else {
            (currentIndex + step).floorMod(audioPoints.size)
        }
        val target = audioPoints[nextIndex]
        val resId = target.audioResId ?: return

        currentAudioZone = target.name
        currentAudioDistanceText = ""
        lastAutoPlayedZone = target.name
        prepareAudio(target.name, resId, startAfterPrepare = true)
        refreshPlayerInfoPanel()
    }

    private fun Int.floorMod(size: Int): Int {
        if (size <= 0) return 0
        val mod = this % size
        return if (mod < 0) mod + size else mod
    }

    private fun setupProgressUi(player: MediaPlayer) {
        val duration = player.duration.coerceAtLeast(0)
        playerProgress.max = duration.coerceAtLeast(1)
        lastProgressValue = 0
        playerProgress.progress = 0
        tvPlayerDuration.text = formatRemainingMillis(duration)
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
                    tvPlayerElapsed.text = ""
                    tvPlayerDuration.text = formatRemainingMillis(dur - pos)
                }
                playerProgressHandler.postDelayed(this, 33)
            }
        }
        playerProgressHandler.post(playerProgressRunnable!!)
    }

    private fun stopProgressUpdates() {
        playerProgressRunnable?.let { playerProgressHandler.removeCallbacks(it) }
        playerProgressRunnable = null
        playerProgressAnimator?.cancel()
        playerProgressAnimator = null
    }

    private fun setProgressSmooth(target: Int) {
        val safe = target.coerceAtLeast(0)
        val current = playerProgress.progress
        if (safe == current) {
            lastProgressValue = safe
            return
        }
        if (safe < current || safe - current > 3500) {
            playerProgressAnimator?.cancel()
            playerProgress.progress = safe
            lastProgressValue = safe
            return
        }
        playerProgressAnimator?.cancel()
        playerProgressAnimator = ValueAnimator.ofInt(current, safe).apply {
            duration = 120L
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                playerProgress.progress = animator.animatedValue as Int
            }
            start()
        }
        lastProgressValue = safe
    }

    private fun formatMillis(millis: Int): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    private fun formatRemainingMillis(millis: Int): String {
        return formatMillis(millis.coerceAtLeast(0))
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
        // dark transparent green for audio zones
        return Color.argb(55, 0, 90, 40)
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
            outlinePaint.color = Color.TRANSPARENT
            outlinePaint.strokeWidth = 0f
            outlinePaint.style = Paint.Style.FILL
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

            val captureThreshold = if (shouldShowTrail) trailProgressCaptureMeters else REROUTE_MIN_MOVE_METERS
            if (minDistance < captureThreshold && closestIndex > lastRouteUpdateIndex) {
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

    private fun showTrailFromAssets() {
        try {
            val rawPoints = loadGpxFromAssets("routes/route_lenskie.gpx")
            if (rawPoints.size < 2) return

            // Trail-only mode: remove any existing markers/lines except GPS overlay.
            map.overlays.removeAll(markers)
            markers.clear()
            map.overlays.removeAll(audioZones)
            map.overlays.removeAll(trailViewpointMarkers)
            trailViewpointMarkers.clear()

            val boundedPoints = rawPoints.filter {
                it.distanceToAsDouble(lenskieStolbyPoint) <= lenskieTrailRadiusMeters
            }.ifEmpty { rawPoints }

            val startIndex = boundedPoints
                .indices
                .minByOrNull { idx -> boundedPoints[idx].distanceToAsDouble(trailStartPoint) }
                ?: 0
            val endIndex = boundedPoints
                .indices
                .minByOrNull { idx -> boundedPoints[idx].distanceToAsDouble(trailEndPoint) }
                ?: boundedPoints.lastIndex

            val points = if (startIndex <= endIndex) {
                boundedPoints.subList(startIndex, endIndex + 1)
            } else {
                boundedPoints.subList(endIndex, startIndex + 1).asReversed()
            }.toMutableList()

            if (points.isNotEmpty()) {
                val last = points.last()
                if (last.distanceToAsDouble(trailFinishPoint) > 5.0) {
                    points.add(trailFinishPoint)
                }
            }
            if (points.size < 2) return

            trailOverlay?.let { map.overlays.remove(it) }
            val trailLine = Polyline().apply {
                setPoints(points)
                outlinePaint.color = Color.parseColor("#1976D2")
                outlinePaint.strokeWidth = 15.0f
                outlinePaint.pathEffect = null
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
                outlinePaint.isAntiAlias = true
            }
            map.overlays.add(trailLine)
            trailOverlay = trailLine
            addTrailViewpointMarkers()
            currentRoutePoints = points
            fullRoutePoints = points.toList()
            lastRouteUpdateIndex = 0
            traveledRoutePoints = emptyList()
            applyLenskieTrailAreaLimit(points)
            stopPeriodicReroute()
            map.minZoomLevel = lenskieTrailOpenZoom
            map.invalidate()
            val center = trailBounds?.centerWithDateLine ?: GeoPoint(
                lenskieStolbyPoint.latitude + trailOpenCenterLatOffset,
                lenskieStolbyPoint.longitude + trailOpenCenterLonOffset
            )
            map.controller.setCenter(center)
            map.controller.setZoom(lenskieTrailOpenZoom)
            clampTrailCameraIfNeeded()
            if (::locationOverlay.isInitialized) {
                try {
                    locationOverlay.disableFollowLocation()
                } catch (_: Exception) { }
            }
        } catch (_: Exception) {
        }
    }

    private fun addTrailViewpointMarkers() {
        trailViewpoints.forEach { (title, point) ->
            val marker = Marker(map).apply {
                position = point
                this.title = title
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                val icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.poi_end)
                this.icon = resizeDrawable(icon, 72, 72)
                setOnMarkerClickListener { m, _ ->
                    showMarkerInfo(m.title ?: "Площадка")
                    true
                }
            }
            map.overlays.add(marker)
            trailViewpointMarkers.add(marker)
        }
    }

    private fun loadGpxFromAssets(assetPath: String): List<GeoPoint> {
        val list = mutableListOf<GeoPoint>()
        val parser = android.util.Xml.newPullParser()
        assets.open(assetPath).use { input ->
            parser.setInput(input, null)
            var event = parser.eventType
            while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    if (parser.name == "trkpt" || parser.name == "rtept") {
                        val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        if (lat != null && lon != null) {
                            list.add(GeoPoint(lat, lon))
                        }
                    }
                }
                event = parser.next()
            }
        }
        return list
    }

    private fun setLenskieOverview() {
        map.controller.setZoom(lenskieOverviewZoom)
        map.controller.setCenter(lenskieStolbyPoint)
    }

    private fun applyLenskieTrailAreaLimit(points: List<GeoPoint>? = null) {
        val box = if (!points.isNullOrEmpty()) {
            var minLat = Double.MAX_VALUE
            var maxLat = -Double.MAX_VALUE
            var minLon = Double.MAX_VALUE
            var maxLon = -Double.MAX_VALUE
            points.forEach { p ->
                if (p.latitude < minLat) minLat = p.latitude
                if (p.latitude > maxLat) maxLat = p.latitude
                if (p.longitude < minLon) minLon = p.longitude
                if (p.longitude > maxLon) maxLon = p.longitude
            }
            val latPad = ((maxLat - minLat) * 0.28).coerceAtLeast(0.004)
            val lonPad = ((maxLon - minLon) * 0.28).coerceAtLeast(0.006)
            BoundingBox(
                maxLat + latPad,
                maxLon + lonPad,
                minLat - latPad,
                minLon - lonPad
            )
        } else {
            val latPad = 0.025
            val lonPad = 0.035
            BoundingBox(
                lenskieStolbyPoint.latitude + latPad,
                lenskieStolbyPoint.longitude + lonPad,
                lenskieStolbyPoint.latitude - latPad,
                lenskieStolbyPoint.longitude - lonPad
            )
        }
        trailBounds = box
        map.setScrollableAreaLimitDouble(box)
    }

    private fun clampTrailCameraIfNeeded() {
        if (!shouldShowTrail) return
        val box = trailBounds ?: return
        val center = map.mapCenter ?: return
        var lat = center.latitude
        var lon = center.longitude
        val clampedLat = lat.coerceIn(box.latSouth, box.latNorth)
        val clampedLon = lon.coerceIn(box.lonWest, box.lonEast)
        val moved = kotlin.math.abs(clampedLat - lat) > 1e-7 || kotlin.math.abs(clampedLon - lon) > 1e-7
        if (moved) {
            map.controller.setCenter(GeoPoint(clampedLat, clampedLon))
        }
        if (map.zoomLevelDouble < lenskieTrailOpenZoom) {
            map.controller.setZoom(lenskieTrailOpenZoom)
        }
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
        // гарантия минимального масштаба, чтобы не "переприближать"
        val minSpan = 0.05 // ~5-6 км по широте
        val latSpan = maxLat - minLat
        if (latSpan < minSpan) {
            val center = (maxLat + minLat) / 2
            minLat = center - minSpan / 2
            maxLat = center + minSpan / 2
        }
        val lonSpan = maxLon - minLon
        if (lonSpan < minSpan) {
            val center = (maxLon + minLon) / 2
            minLon = center - minSpan / 2
            maxLon = center + minSpan / 2
        }

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
        val durationText = travelMinutes?.let { formatDurationClock(it) } ?: time
        val arrivalText = travelMinutes?.let { formatArrivalTime(it) }
        updateTripInfo(distance, durationText, arrivalText)
        crossfadeRouteAction(showTripInfo = true)
    }

    private fun formatDurationClock(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "--:--"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}:${minutes.toString().padStart(2, '0')}"
    }

    

    private fun setupOSMDroidConfig() {
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        val osmdroidBase = File(filesDir, "osmdroid").apply { mkdirs() }
        val tilesDir = File(osmdroidBase, "tiles").apply { mkdirs() }
        Configuration.getInstance().osmdroidBasePath = osmdroidBase
        Configuration.getInstance().osmdroidTileCache = tilesDir
        
        // Включаем кэширование тайлов для офлайн режима
        Configuration.getInstance().cacheMapTileCount = 10000
        Configuration.getInstance().cacheMapTileOvershoot = 16
    }

    private fun initMap() {
        map = findViewById(R.id.map)
        map.setUseDataConnection(true)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(false)
        map.minZoomLevel = if (shouldShowTrail) lenskieTrailOpenZoom else 3.0
        map.maxZoomLevel = 18.0
        map.isTilesScaledToDpi = true
        val yakutiaBounds = org.osmdroid.util.BoundingBox(75.0, 162.0, 55.0, 105.0)
        map.setScrollableAreaLimitDouble(yakutiaBounds)
        if (shouldShowTrail) {
            map.addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                    clampTrailCameraIfNeeded()
                    return false
                }

                override fun onZoom(event: ZoomEvent?): Boolean {
                    clampTrailCameraIfNeeded()
                    return false
                }
            })
        }
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

        if (!isLocationReady) {
            setLenskieOverview()
        }
        applyMarkersVisibility()
        map.invalidate()
    }

    private fun applyMarkersVisibility() {
        if (!::map.isInitialized) return
        val overlays = map.overlays
        if (markersVisible) {
            markers.forEach { marker ->
                if (!overlays.contains(marker)) {
                    overlays.add(marker)
                }
            }
        } else {
            overlays.removeAll(markers)
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
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(18f).toInt(), dpToPx(16f).toInt(), dpToPx(18f).toInt(), dpToPx(14f).toInt())
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(18f)
                setColor(Color.parseColor("#6F8D73"))
            }
        }
        val tvTitle = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val tvSubtitle = TextView(this).apply {
            text = description
            setTextColor(Color.parseColor("#EAF5EC"))
            textSize = 15f
            setPadding(0, dpToPx(8f).toInt(), 0, 0)
        }
        val tvDetails = TextView(this).apply {
            text = details
            setTextColor(Color.parseColor("#D9EEDF"))
            textSize = 13f
            visibility = if (details.isBlank()) View.GONE else View.VISIBLE
            setPadding(0, dpToPx(8f).toInt(), 0, 0)
        }
        val closeBtn = Button(this).apply {
            text = "Закрыть"
            setTextColor(Color.WHITE)
            textSize = 14f
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(12f)
                setColor(Color.parseColor("#57755D"))
            }
            setPadding(dpToPx(14f).toInt(), dpToPx(8f).toInt(), dpToPx(14f).toInt(), dpToPx(8f).toInt())
        }
        panel.addView(tvTitle)
        panel.addView(tvSubtitle)
        panel.addView(tvDetails)
        panel.addView(closeBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dpToPx(14f).toInt()
        })

        val dialog = AlertDialog.Builder(this)
            .setView(panel)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        closeBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
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
                        btnBuildRoute.isEnabled = true
                        updateRouteInfo("", "")
                        if (!shouldShowTrail) {
                            map.controller.animateTo(it)
                            if (map.zoomLevelDouble < 14.5) {
                                map.controller.setZoom(14.5)
                            }
                        }
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
        if (fullRoutePoints.isNotEmpty() && !shouldShowTrail) {
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
        if (shouldShowTrail) return
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
