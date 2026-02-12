package com.example.lenskiegid.weather

data class WeatherHour(
    val timeIso: String,
    val temperatureC: Double,
    val kind: WeatherKind
)

data class WeatherData(
    val temperatureC: Double,
    val kind: WeatherKind,
    val conditionText: String,
    val humidity: Int?,
    val windSpeed: Double?,
    val uvIndex: Double?,
    val sunriseIso: String?,
    val sunsetIso: String?,
    val hourly: List<WeatherHour>
)

enum class WeatherKind {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    SNOW,
    THUNDER,
    UNKNOWN
}
