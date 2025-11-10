package com.example.lenskiegid.routing

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import btools.router.*
import java.io.File
import java.io.FileOutputStream

class BRouterEngine(private val context: Context) {
    private val baseDir: File by lazy { File(context.filesDir, "brouter").apply { mkdirs() } }
    private val segmentsDir: File by lazy { File(baseDir, "segments4").apply { mkdirs() } }
    private val profilesDir: File by lazy { File(baseDir, "profiles2").apply { mkdirs() } }
    private val carProfileFile: File by lazy { File(profilesDir, "car-vario.brf") }

    suspend fun ensureCarProfile(): Unit = withContext(Dispatchers.IO) {
        if (!carProfileFile.exists()) {
            context.assets.open("profiles2/car-vario.brf").use { input ->
                FileOutputStream(carProfileFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        val lookups = File(profilesDir, "lookups.dat")
        if (!lookups.exists()) {
            try {
                context.assets.open("profiles2/lookups.dat").use { input ->
                    FileOutputStream(lookups).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (_: Exception) {}
        }
    } // Надо было сделать ДИНАМИЧНО стрющийся маршрут по которому будут идти пользователи

    fun segmentsPath(): File = segmentsDir

    private fun toIntLon(lon: Double): Int = ((lon + 180.0) * 1e6).toInt()
    private fun toIntLat(lat: Double): Int = ((lat + 90.0) * 1e6).toInt()
    private fun fromIntLon(ilon: Int): Double = ilon * 1e-6 - 180.0
    private fun fromIntLat(ilat: Int): Double = ilat * 1e-6 - 90.0

    private suspend fun ensureSegmentsFromAssets(): Unit = withContext(Dispatchers.IO) {
        try {
            val names = context.assets.list("segments4") ?: emptyArray()
            for (name in names) {
                if (!name.endsWith(".rd5", ignoreCase = true)) continue
                val target = File(segmentsDir, name)
                if (target.exists() && target.length() > 500_000L) continue
                context.assets.open("segments4/$name").use { input ->
                    FileOutputStream(target).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (_: Exception) { }
    }

    suspend fun routeCar(from: GeoPoint, to: GeoPoint): List<GeoPoint> = withContext(Dispatchers.IO) {
        ensureCarProfile()
        ensureSegmentsFromAssets()
        if (!RoutingHelper.hasDirectoryAnyDatafiles(segmentsDir)) {
            throw IllegalStateException("No BRouter .rd5 segments found in ${segmentsDir.absolutePath}")
        }

        val wp = arrayListOf<OsmNodeNamed>()
        wp.add(OsmNodeNamed().apply {
            ilon = toIntLon(from.longitude)
            ilat = toIntLat(from.latitude)
            name = "from"
        })
        wp.add(OsmNodeNamed().apply {
            ilon = toIntLon(to.longitude)
            ilat = toIntLat(to.latitude)
            name = "to"
        })

        val rc = RoutingContext().apply {
            localFunction = carProfileFile.absolutePath
            // prefer classic routing mode
            useDynamicDistance = false
            outputFormat = "gpx"
        }

        val engine = RoutingEngine(null, null, segmentsDir, wp, rc, RoutingEngine.BROUTER_ENGINEMODE_ROUTING)
        engine.doRun(0)
        val track = engine.foundTrack
        if (track == null || track.nodes == null || track.nodes.isEmpty()) {
            throw IllegalStateException(engine.errorMessage ?: "No route found")
        }
        return@withContext track.nodes.map { node ->
            GeoPoint(fromIntLat(node.getILat()), fromIntLon(node.getILon()))
        }
    }
}
