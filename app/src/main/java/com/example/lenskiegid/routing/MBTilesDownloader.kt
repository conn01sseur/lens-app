package com.example.lenskiegid.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object MBTilesDownloader {
    suspend fun download(urlStr: String, target: File, onProgress: (bytesRead: Long, totalBytes: Long) -> Unit): Unit = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        var attempt = 0
        val maxAttempts = 3
        while (attempt < maxAttempts) {
            attempt++
            var conn: HttpURLConnection? = null
            try {
                val url = URL(urlStr)
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 30000
                conn.readTimeout = 120000
                conn.instanceFollowRedirects = true
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "LenskieGid/1.0")
                val code = conn.responseCode
                if (code / 100 != 2) {
                    continue
                }
                val total = conn.contentLengthLong
                val input = BufferedInputStream(conn.inputStream, 128 * 1024)
                val fos = FileOutputStream(target)
                val output = BufferedOutputStream(fos, 128 * 1024)
                try {
                    val buf = ByteArray(128 * 1024)
                    var sum = 0L
                    while (true) {
                        val r = input.read(buf)
                        if (r <= 0) break
                        output.write(buf, 0, r)
                        sum += r
                        onProgress(sum, total)
                    }
                    output.flush()
                    fos.fd.sync()
                } finally {
                    try { output.close() } catch (_: Exception) {}
                    try { fos.close() } catch (_: Exception) {}
                    try { input.close() } catch (_: Exception) {}
                }
                return@withContext
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }
            try { Thread.sleep(800L * attempt) } catch (_: InterruptedException) {}
            // cleanup partial
            try { if (target.exists()) target.delete() } catch (_: Exception) {}
        }
        throw RuntimeException("Failed to download MBTiles after $maxAttempts attempts")
    }
}
