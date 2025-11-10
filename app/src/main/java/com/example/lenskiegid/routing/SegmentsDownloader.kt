package com.example.lenskiegid.routing

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object SegmentsDownloader {
    private const val BASE_URL = "https://brouter.de/brouter/segments4"

    private fun lonPrefix(lon: Int): String = if (lon >= 0) "E$lon" else "W${-lon}"
    private fun latPrefix(lat: Int): String = if (lat >= 0) "N$lat" else "S${-lat}"

    private fun floorTo5(x: Double): Int {
        val f = floor(x / 5.0) * 5.0
        return f.toInt()
    }

    private fun neededTiles(from: GeoPoint, to: GeoPoint): List<String> {
        val minLat = floorTo5(min(from.latitude, to.latitude))
        val maxLat = floorTo5(max(from.latitude, to.latitude))
        val minLon = floorTo5(min(from.longitude, to.longitude))
        val maxLon = floorTo5(max(from.longitude, to.longitude))
        val names = mutableListOf<String>()
        var lat = minLat
        while (lat <= maxLat) {
            var lon = minLon
            while (lon <= maxLon) {
                names.add("${lonPrefix(lon)}_${latPrefix(lat)}.rd5")
                lon += 5
            }
            lat += 5
        }
        return names
    }

    private fun neededTilesForBounds(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<String> {
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

    suspend fun ensureSegments(context: Context, segmentsDir: File, from: GeoPoint, to: GeoPoint): Unit = withContext(Dispatchers.IO) {
        segmentsDir.mkdirs()
        val toGet = neededTiles(from, to).filter { name -> !File(segmentsDir, name).exists() }
        for (name in toGet) {
            downloadOne(name, segmentsDir)
        }
    }

    suspend fun downloadYakutia(context: Context, segmentsDir: File, onProgress: (current: Int, total: Int, name: String) -> Unit): Unit = withContext(Dispatchers.IO) {
        segmentsDir.mkdirs()
        // Bounds for Republic of Sakha (Yakutia) roughly
        val minLat = 55.0
        val maxLat = 75.0
        val minLon = 105.0
        val maxLon = 162.0
        val all = neededTilesForBounds(minLat, maxLat, minLon, maxLon)
        val toGet = all.filter { name -> !File(segmentsDir, name).exists() }
        val total = toGet.size
        var i = 0
        for (name in toGet) {
            i += 1
            onProgress(i - 1, total, name)
            downloadOne(name, segmentsDir) { bytesRead, totalBytes ->
                // We report file-level progress through name; keep global progress simple
                onProgress(i - 1, total, "$name ${(if (totalBytes>0) String.format("%.1f/%.1f MB", bytesRead/1048576.0, totalBytes/1048576.0) else String.format("%.1f MB", bytesRead/1048576.0))}")
            }
        }
        onProgress(total, total, "")
    }

    private fun downloadOne(name: String, segmentsDir: File, perByte: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null) {
        val outFile = File(segmentsDir, name)
        var attempt = 0
        val maxAttempts = 3
        while (attempt < maxAttempts) {
            attempt++
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$BASE_URL/$name")
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 20000
                conn.readTimeout = 60000
                conn.instanceFollowRedirects = true
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "LenskieGid/1.0")
                val code = conn.responseCode
                if (code == 404) {
                    // tile not present on server, skip
                    return
                }
                if (code / 100 != 2) {
                    // retry on non-2xx
                    continue
                }
                val totalBytes = conn.contentLengthLong
                // fresh file per attempt
                if (outFile.exists()) outFile.delete()
                val input = java.io.BufferedInputStream(conn.inputStream, 64 * 1024)
                val fos = FileOutputStream(outFile)
                val output = java.io.BufferedOutputStream(fos, 64 * 1024)
                try {
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    var sum: Long = 0
                    var lastProgressTime = System.currentTimeMillis()
                    val stallTimeoutMs = 30000L
                    while (true) {
                        read = input.read(buf)
                        if (read <= 0) break
                        output.write(buf, 0, read)
                        sum += read
                        perByte?.invoke(sum, totalBytes)
                        val now = System.currentTimeMillis()
                        if (now - lastProgressTime > stallTimeoutMs) {
                            throw java.net.SocketTimeoutException("stall detected")
                        }
                        lastProgressTime = now
                    }
                    output.flush()
                    fos.fd.sync()
                } finally {
                    try { output.close() } catch (_: Exception) {}
                    try { fos.close() } catch (_: Exception) {}
                    try { input.close() } catch (_: Exception) {}
                }
                return // success
            } catch (_: Exception) {
                // retry
            } finally {
                conn?.disconnect()
            }
            // delete partial on retry
            try { if (outFile.exists()) outFile.delete() } catch (_: Exception) {}
            try {
                Thread.sleep((500L * attempt))
            } catch (_: InterruptedException) { }
        }
        // on final failure, leave file possibly partial; best effort
    }
}
