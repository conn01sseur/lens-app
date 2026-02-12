package com.example.lenskiegid.weather

object WeatherUi {
    fun color(kind: WeatherKind): String = when (kind) {
        WeatherKind.CLEAR -> "#96C4D3"
        WeatherKind.PARTLY_CLOUDY -> "#8FB6C4"
        WeatherKind.CLOUDY -> "#7F9BAA"
        WeatherKind.FOG -> "#9AA7AF"
        WeatherKind.DRIZZLE -> "#7FA7B8"
        WeatherKind.RAIN -> "#6F8FA8"
        WeatherKind.SNOW -> "#B7C8D8"
        WeatherKind.THUNDER -> "#6D7E98"
        WeatherKind.UNKNOWN -> "#96C4D3"
    }

    fun iconFile(kind: WeatherKind): String = when (kind) {
        WeatherKind.CLEAR -> "sun.png"
        WeatherKind.PARTLY_CLOUDY -> "partly_cloudy.png"
        WeatherKind.CLOUDY -> "cloudy.png"
        WeatherKind.FOG -> "fog.png"
        WeatherKind.DRIZZLE -> "rain.png"
        WeatherKind.RAIN -> "rain.png"
        WeatherKind.SNOW -> "snow.png"
        WeatherKind.THUNDER -> "thunder.png"
        WeatherKind.UNKNOWN -> "cloudy.png"
    }

    fun description(kind: WeatherKind): String = when (kind) {
        WeatherKind.CLEAR -> "Ясно"
        WeatherKind.PARTLY_CLOUDY -> "Переменная облачность"
        WeatherKind.CLOUDY -> "Облачно"
        WeatherKind.FOG -> "Туман"
        WeatherKind.DRIZZLE -> "Морось"
        WeatherKind.RAIN -> "Дождь"
        WeatherKind.SNOW -> "Снег"
        WeatherKind.THUNDER -> "Гроза"
        WeatherKind.UNKNOWN -> "Погода"
    }
}
