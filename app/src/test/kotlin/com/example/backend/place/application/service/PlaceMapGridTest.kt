package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.common.geo.Viewport
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

class PlaceMapGridTest {
    @Test
    fun `세계에서 골목까지 실제 교차 셀 수가 응답 상한을 넘지 않는다`() {
        val viewports =
            listOf(
                Viewport(Coordinate(-90.0, -180.0), Coordinate(90.0, 180.0)),
                Viewport(Coordinate(33.0, 124.0), Coordinate(39.0, 132.0)),
                Viewport(Coordinate(37.0, 126.0), Coordinate(38.0, 128.0)),
                Viewport(Coordinate(37.50, 127.00), Coordinate(37.51, 127.01)),
                Viewport(Coordinate(37.500001, 127.000001), Coordinate(37.500002, 127.000002)),
                Viewport(Coordinate(-0.001, -180.0), Coordinate(0.001, 180.0)),
            )
        for (viewport in viewports) {
            val precision = PlaceMapGrid.precision(viewport)
            val bits = precision * 5
            val columns = 2.0.pow(ceil(bits / 2.0)).toLong()
            val rows = 2.0.pow(floor(bits / 2.0)).toLong()
            val firstColumn =
                floor((viewport.southWest.longitude + 180) / 360 * columns).toLong().coerceAtMost(columns - 1)
            val lastColumn =
                floor((viewport.northEast.longitude + 180) / 360 * columns).toLong().coerceAtMost(columns - 1)
            val firstRow = floor((viewport.southWest.latitude + 90) / 180 * rows).toLong().coerceAtMost(rows - 1)
            val lastRow = floor((viewport.northEast.latitude + 90) / 180 * rows).toLong().coerceAtMost(rows - 1)
            val cellCount = (lastColumn - firstColumn + 1) * (lastRow - firstRow + 1)

            assertTrue(cellCount <= 256, "viewport=$viewport precision=$precision cells=$cellCount")
        }
    }

    @Test
    fun `좁은 뷰포트는 넓은 뷰포트보다 세밀한 격자를 사용한다`() {
        val wide = Viewport(Coordinate(33.0, 124.0), Coordinate(39.0, 132.0))
        val narrow = Viewport(Coordinate(37.50, 127.00), Coordinate(37.51, 127.01))
        assertTrue(PlaceMapGrid.precision(narrow) > PlaceMapGrid.precision(wide))
    }
}
