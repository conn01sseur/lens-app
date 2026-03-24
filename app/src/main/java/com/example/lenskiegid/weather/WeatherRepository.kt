package com.example.lenskiegid.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object WeatherRepository {
    private val client = OkHttpClient.Builder()
        .callTimeout(12, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(lat: Double, lon: Double): WeatherData = withContext(Dispatchers.IO) {
        runCatching { fetchOpenMeteo(lat, lon) }
            .getOrElse { fetchMetNo(lat, lon) }
    }

    private fun fetchOpenMeteo(lat: Double, lon: Double): WeatherData {
        val urlA = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current_weather=true" +
            "&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,uv_index" +
            "&daily=sunrise,sunset" +
            "&timezone=auto&forecast_days=1"
        val body = httpGet(urlA)
        val root = JSONObject(body)
        if (root.optBoolean("error", false)) {
            throw IllegalStateException(root.optString("reason", "Open-Meteo error"))
        }
        return parseOpenMeteo(root)
    }

    private fun parseOpenMeteo(root: JSONObject): WeatherData {
        val current = root.optJSONObject("current_weather")
        val hourly = root.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val codes = hourly.getJSONArray("weather_code")
        val humidity = hourly.optJSONArray("relative_humidity_2m")
        val wind = hourly.optJSONArray("wind_speed_10m")
        val uv = hourly.optJSONArray("uv_index")

        val currentTime = current?.optString("time")
        val index = indexOfTime(times, currentTime) ?: 0

        val temp = current?.optDouble("temperature", Double.NaN)
            ?.takeUnless { it.isNaN() } ?: temps.getDouble(index)
        val code = current?.optInt("weathercode", -1)?.takeIf { it >= 0 } ?: codes.getInt(index)
        val windSpeed = current?.optDouble("windspeed", Double.NaN)
            ?.takeUnless { it.isNaN() } ?: wind?.optDouble(index)
        val hum = humidity?.optInt(index)
        val uvIndex = uv?.optDouble(index)

        val daily = root.optJSONObject("daily")
        val sunrise = daily?.optJSONArray("sunrise")?.optString(0)
        val sunset = daily?.optJSONArray("sunset")?.optString(0)

        val hours = buildHourly(times, temps, codes, index)
        val kind = kindFromWmo(code)
        val condition = WeatherUi.description(kind)

        return WeatherData(
            temperatureC = temp,
            kind = kind,
            conditionText = condition,
            humidity = hum,
            windSpeed = windSpeed,
            uvIndex = uvIndex,
            sunriseIso = sunrise,
            sunsetIso = sunset,
            hourly = hours
        )
    }

    private fun fetchMetNo(lat: Double, lon: Double): WeatherData {
        val url = "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=$lat&lon=$lon"
        val body = httpGet(url, mapOf("User-Agent" to "LenskieGid/1.0 (contact@lenskiegid.app)"))
        val root = JSONObject(body)
        val props = root.getJSONObject("properties")
        val timeseries = props.getJSONArray("timeseries")
        if (timeseries.length() == 0) throw IllegalStateException("MET no data")

        val first = timeseries.getJSONObject(0)
        val firstData = first.getJSONObject("data")
        val details = firstData.getJSONObject("instant").getJSONObject("details")
        val temp = details.optDouble("air_temperature")
        val hum = details.optInt("relative_humidity")
        val wind = details.optDouble("wind_speed")
        val uv = details.optDouble("ultraviolet_index_clear_sky")
            .takeIf { !it.isNaN() }

        val symbol = extractSymbol(firstData)
        val kind = kindFromMet(symbol)
        val condition = WeatherUi.description(kind)

        val hours = buildMetHourly(timeseries)

        return WeatherData(
            temperatureC = temp,
            kind = kind,
            conditionText = condition,
            humidity = if (hum == 0) null else hum,
            windSpeed = wind,
            uvIndex = uv,
            sunriseIso = null,
            sunsetIso = null,
            hourly = hours
        )
    }

    private fun buildHourly(
        times: JSONArray,
        temps: JSONArray,
        codes: JSONArray,
        startIndex: Int
    ): List<WeatherHour> {
        val list = ArrayList<WeatherHour>(24)
        val end = minOf(times.length(), startIndex + 24)
        for (i in startIndex until end) {
            val code = codes.optInt(i, -1)
            val kind = kindFromWmo(code)
            list.add(
                WeatherHour(
                    timeIso = times.getString(i),
                    temperatureC = temps.getDouble(i),
                    kind = kind
                )
            )
        }
        return list
    }

    private fun buildMetHourly(timeseries: JSONArray): List<WeatherHour> {
        val list = ArrayList<WeatherHour>(24)
        val end = minOf(timeseries.length(), 24)
        for (i in 0 until end) {
            val item = timeseries.getJSONObject(i)
            val time = item.getString("time")
            val data = item.getJSONObject("data")
            val details = data.getJSONObject("instant").getJSONObject("details")
            val temp = details.optDouble("air_temperature")
            val symbol = extractSymbol(data)
            val kind = kindFromMet(symbol)
            list.add(
                WeatherHour(
                    timeIso = time,
                    temperatureC = temp,
                    kind = kind
                )
            )
        }
        return list
    }

    private fun extractSymbol(data: JSONObject): String? {
        return data.optJSONObject("next_1_hours")?.optJSONObject("summary")?.optString("symbol_code")
            ?: data.optJSONObject("next_6_hours")?.optJSONObject("summary")?.optString("symbol_code")
            ?: data.optJSONObject("next_12_hours")?.optJSONObject("summary")?.optString("symbol_code")
    }

    private fun indexOfTime(times: JSONArray, target: String?): Int? {
        if (target == null) return null
        for (i in 0 until times.length()) {
            if (times.optString(i) == target) return i
        }
        return null
    }

    private fun kindFromWmo(code: Int): WeatherKind = when (code) {
        0 -> WeatherKind.CLEAR
        1, 2 -> WeatherKind.PARTLY_CLOUDY
        3 -> WeatherKind.CLOUDY
        45, 48 -> WeatherKind.FOG
        51, 53, 55, 56, 57 -> WeatherKind.DRIZZLE
        61, 63, 65, 66, 67, 80, 81, 82 -> WeatherKind.RAIN
        71, 73, 75, 77, 85, 86 -> WeatherKind.SNOW
        95, 96, 99 -> WeatherKind.THUNDER
        else -> WeatherKind.UNKNOWN
    }

    private fun kindFromMet(symbol: String?): WeatherKind {
        if (symbol == null) return WeatherKind.UNKNOWN
        return when {
            symbol.startsWith("clearsky") -> WeatherKind.CLEAR
            symbol.startsWith("fair") -> WeatherKind.PARTLY_CLOUDY
            symbol.startsWith("partlycloudy") -> WeatherKind.PARTLY_CLOUDY
            symbol.startsWith("cloudy") -> WeatherKind.CLOUDY
            symbol.startsWith("fog") -> WeatherKind.FOG
            symbol.contains("thunder") -> WeatherKind.THUNDER
            symbol.contains("snow") || symbol.contains("sleet") -> WeatherKind.SNOW
            symbol.contains("rain") || symbol.contains("showers") || symbol.contains("drizzle") -> WeatherKind.RAIN
            else -> WeatherKind.UNKNOWN
        }
    }

    private fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String {
        val builder = Request.Builder().url(url)
        headers.forEach { (k, v) -> builder.addHeader(k, v) }
        val request = builder.build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}")
            }
            return response.body?.string() ?: throw IllegalStateException("Empty body")
        }
    }
}
