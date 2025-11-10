package com.example.lenskiegid.route

import org.osmdroid.util.GeoPoint

object PolyUtil {

    fun decodeFromGraphHopper(coordinates: List<List<Double>>): List<GeoPoint> {
        return coordinates.map { coord ->
            if (coord.size >= 2) {
                GeoPoint(coord[1], coord[0])
            } else {
                GeoPoint(0.0, 0.0)
            }
        }
    }

    fun decodeFromOsrm(coordinates: List<List<Double>>): List<GeoPoint> {
        return coordinates.map { coord ->
            if (coord.size >= 2) {
                GeoPoint(coord[1], coord[0])
            } else {
                GeoPoint(0.0, 0.0)
            }
        }
    }

    fun decodeStraightLine(start: GeoPoint, end: GeoPoint): List<GeoPoint> {
        return listOf(start, end)
    }
}