package com.example.lenskiegid.data

import com.example.lenskiegid.R
import org.osmdroid.util.GeoPoint

data class MarkerDef(
    val point: GeoPoint,
    val title: String,
    val iconResId: Int,
    val width: Int,
    val height: Int
)

object MarkersCatalog {
    val markers: List<MarkerDef> = listOf(
        MarkerDef(GeoPoint(61.484444, 129.140278), "Покровск\nГород", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.300226, 128.659670), "Булгунняхтах\nНаселенный пункт", R.drawable.marker1, 64, 94),
        MarkerDef(GeoPoint(61.300556, 128.257222), "Улахан Аан\nНаселенный пункт", R.drawable.marker2, 64, 94),
        MarkerDef(GeoPoint(61.224667, 127.729690), "Тит-Ары\nНаселенный пункт", R.drawable.marker3, 64, 94),
        MarkerDef(GeoPoint(61.179843, 127.535734), "Тумул\nНаселенный пункт", R.drawable.marker4, 64, 94),
        MarkerDef(GeoPoint(61.133312, 127.339562), "Батамай\nНаселенный пункт", R.drawable.marker5, 64, 94),
        MarkerDef(GeoPoint(61.096667, 127.348333), "Ленские столбы\nПриродный парк, объект ЮНЕСКО", R.drawable.marker6, 96, 135),
        MarkerDef(GeoPoint(61.49068114739213, 129.13797321345726), "АЗС\nКруглосуточная АЗС с магазином, кафе и туалетом. Принимаются все виды карт.", R.drawable.azc, 64, 94),
        MarkerDef(GeoPoint(61.48931620933417, 129.13756653681764), "Экспресс\nСтоловая", R.drawable.cafe, 64, 94),
        MarkerDef(GeoPoint(61.297475985194545, 128.65579251732652), "Туйгун\nМагазин", R.drawable.shop, 64, 94),
        MarkerDef(GeoPoint(61.30723448570929, 128.28139895446074), "Туймаада-Нефть\nЗаправка", R.drawable.azc, 64, 94),
        MarkerDef(GeoPoint(61.13336883970555, 127.3397867755516), "Турбаза Ленские Столбы\nНочлег", R.drawable.sleep, 64, 94),
        MarkerDef(GeoPoint(61.10528069768027, 127.35620518596014), "Көрөр сир\nСмотровая площадка", R.drawable.stolb, 64, 94),
        MarkerDef(GeoPoint(61.982444304858184, 129.65160774390316), "Тест", R.drawable.marker, 64, 94)
    )
}
