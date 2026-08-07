package com.example.backend.common.persistence.postgis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GeographyPointColumnTypeTest {
    private val columnType = GeographyPointColumnType()

    @Test
    fun `같은 트랜잭션 insert 후 재읽기 시 넘어온 GeoPoint 를 그대로 반환한다`() {
        // prod 500 회귀 지점: Exposed 가 캐시된 GeoPoint 를 그대로 넘기면 예외 없이 그대로 반환해야 한다.
        val point = GeoPoint(latitude = 37.5445, longitude = 127.0575)

        assertEquals(point, columnType.valueFromDB(point))
    }

    @Test
    fun `지원하지 않는 값 타입은 IllegalStateException 을 던진다`() {
        assertThrows<IllegalStateException> { columnType.valueFromDB(42) }
    }
}
