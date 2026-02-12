package com.example.lenskiegid.data

import com.example.lenskiegid.R
import org.osmdroid.util.GeoPoint

data class PointOfInterest(
    val point: GeoPoint,
    val name: String,
    val type: String,
    val audioResId: Int?,
    val customRadius: Double? = null
)

object PointsCatalog {
    private val typeDefaultRadii = mapOf(
        "город" to 3000.0,
        "населенный пункт" to 2000.0,
        "природный парк" to 5000.0,
        "азс" to 1000.0,
        "кафе" to 800.0,
        "магазин" to 500.0,
        "турбаза" to 1500.0,
        "смотровая" to 2000.0,
        "Достопримечательность" to 1000.0,
        "test" to 500.0
    )

    val pointsOfInterest: List<PointOfInterest> = listOf(
        PointOfInterest(GeoPoint(61.484444, 129.140278), "Покровск", "город", R.raw.pokrovsk_audio),
        PointOfInterest(GeoPoint(61.300226, 128.659670), "Булгунняхтах", "населенный пункт", R.raw.bulgunniahtah_audio),
        PointOfInterest(GeoPoint(61.300556, 128.257222), "Улахан Аан", "населенный пункт", R.raw.ulahan_aan_audio),
        PointOfInterest(GeoPoint(61.224667, 127.729690), "Тит-Ары", "населенный пункт", R.raw.tit_ary_audio),
        PointOfInterest(GeoPoint(61.179843, 127.535734), "Тумул", "населенный пункт", R.raw.tumul_audio),
        PointOfInterest(GeoPoint(61.133312, 127.339562), "Батамай", "населенный пункт", R.raw.batamay_audio),
        PointOfInterest(GeoPoint(61.096667, 127.348333), "Ленские столбы", "природный парк", R.raw.lenskie_stolby_audio, 5000.0),
        PointOfInterest(GeoPoint(62.02788856645921, 129.7306748847576), "Площадь Ленина", "Достопримечательность", R.raw.test, 500.0),
        PointOfInterest(GeoPoint(62.032693319464045, 129.75033555900478), "IT-Park", "test", R.raw.test),
        PointOfInterest(GeoPoint(61.98212476616803, 129.65329478064962), "Тестовая аудио зона", "test", R.raw.test, 500.0),
        PointOfInterest(GeoPoint(61.982444304858184, 129.65160774390316), "test", "test", R.raw.test, 500.0)
    )

    fun defaultRadius(type: String): Double = typeDefaultRadii[type] ?: 1000.0

    fun findByName(name: String): PointOfInterest? = pointsOfInterest.firstOrNull { it.name == name }
}
