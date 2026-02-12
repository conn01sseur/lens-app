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
        MarkerDef(GeoPoint(62.027481, 129.731774), "Якутск\nНаселенный пункт", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.893893, 129.515920), "Владимировка\nНаселенный пункт", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.708506, 129.470213), "Техтюр\nНаселенный пункт", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.674387, 129.402848), "Октемцы\nНаселенный пункт", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.618642, 129.253229), "Улах-Ан\nНаселенный пункт", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.485377, 129.127368), "Покровск\nНаселенный пункт", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.399736, 128.948731), "Мохсоголлох\nНаселенный пункт", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.377621, 128.876050), "Бестях\nНаселенный пункт", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.302895, 128.661865), "Булгунняхтах\nНаселенный пункт", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.296495, 128.260511), "Улахан-Ан\nНаселенный пункт", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.230073, 127.748935), "Тит-Ары\nНаселенный пункт", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.181947, 127.539958), "Тумул\nНаселенный пункт", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.104833, 127.357249), "База отдыха Ленские столбы\nТуристическая база", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.749130, 129.547600), "Куллаты\nТочка маршрута", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.776020, 129.537490), "Граница Ханалас\nТочка маршрута", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.536908, 129.187815), "Ой\nТочка маршрута", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.367290, 128.415510), "Камень Дите\nДостопримечательность", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.305940, 128.280890), "Кафе Артык\nКафе", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.301370, 128.266120), "Айыы уола\nТочка маршрута", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.270382, 128.113965), "Еланка\nНаселенный пункт", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.293060, 128.247400), "Зимник\nЗимний маршрут по льду", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.252540, 127.933280), "Арка желаний\nДостопримечательность", R.drawable.marker, 64, 94),
        MarkerDef(GeoPoint(61.134996, 127.338703), "Батамай\nНаселенный пункт", R.drawable.marker, 64, 94)
    )
}
