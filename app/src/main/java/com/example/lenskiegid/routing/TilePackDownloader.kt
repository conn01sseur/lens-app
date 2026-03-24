package com.example.lenskiegid.routing

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

object TilePackDownloader {
    private const val MAPNIK = "https://tile.openstreetmap.org"

    private fun lonToX(lon: Double, z: Int): Int {
        val n = 2.0.pow(z.toDouble())
        return ((lon + 180.0) / 360.0 * n).toInt()
    }

    private fun latToY(lat: Double, z: Int): Int {
        val n = 2.0.pow(z.toDouble())
        val latRad = Math.toRadians(lat)
        val y = (1.0 - ln(tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / Math.PI) / 2.0 * n
        return y.toInt()
    }

    suspend fun downloadLenskieTiles(
        context: Context,
        onProgress: (current: Int, total: Int, name: String) -> Unit
    ): Unit = withContext(Dispatchers.IO) {
        val cacheRoot = File(context.filesDir, "osmdroid/tiles/Mapnik")
        cacheRoot.mkdirs()

        // Practical zoom range for offline navigation near Lenskie Stolby.
        val minZoom = 9
        val maxZoom = 14
        val minLat = 60.4
        val maxLat = 61.6
        val minLon = 126.2
        val maxLon = 128.2

        val tiles = mutableListOf<Triple<Int, Int, Int>>()
        for (z in minZoom..maxZoom) {
            val xMin = lonToX(minLon, z)
            val xMax = lonToX(maxLon, z)
            val yMin = latToY(maxLat, z)
            val yMax = latToY(minLat, z)
            for (x in xMin..xMax) {
                for (y in yMin..yMax) {
                    tiles.add(Triple(z, x, y))
                }
            }
        }

        val pending = tiles.filter { (z, x, y) ->
            !File(cacheRoot, "$z/$x/$y.png").exists()
        }
        val total = pending.size
        var current = 0

        for ((z, x, y) in pending) {
            current += 1
            onProgress(current - 1, total, "$z/$x/$y")
            downloadTile(cacheRoot, z, x, y)
            onProgress(current, total, "$z/$x/$y")
        }
    }

    private fun downloadTile(root: File, z: Int, x: Int, y: Int) {
        val target = File(root, "$z/$x/$y.png")
        target.parentFile?.mkdirs()
        if (target.exists()) return

        var conn: HttpURLConnection? = null
        try {
            val url = URL("$MAPNIK/$z/$x/$y.png")
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 25000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "LenskieGid/1.0")
            }
            if (conn.responseCode / 100 != 2) return
            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (_: Exception) {
            if (target.exists()) target.delete()
        } finally {
            conn?.disconnect()
        }
    }
}
