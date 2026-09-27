package com.example.backend.place.application.service

import com.example.backend.common.geo.Viewport
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

/** 뷰포트 크기로 격자 크기를 결정한다. 모든 격자를 반환할 수 있도록 상한을 보수적으로 계산한다. */
internal object PlaceMapGrid {
    const val MAX_MARKERS = 100
    const val MAX_BUCKETS = 256

    fun precision(viewport: Viewport): Int =
        (12 downTo 1).firstOrNull { precision ->
            val bits = precision * 5
            val width = 360.0 / 2.0.pow(ceil(bits / 2.0))
            val height = 180.0 / 2.0.pow(floor(bits / 2.0))
            val columns = floor((viewport.northEast.longitude - viewport.southWest.longitude) / width) + 2
            val rows = floor((viewport.northEast.latitude - viewport.southWest.latitude) / height) + 2
            columns * rows <= MAX_BUCKETS
        } ?: 1 // 전 세계도 precision=1에서는 최대 32개 셀이다.
}
