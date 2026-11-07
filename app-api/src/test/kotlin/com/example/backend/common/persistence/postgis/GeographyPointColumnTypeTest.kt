package com.example.backend.common.persistence.postgis

import com.example.backend.common.geo.Coordinate
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GeographyPointColumnTypeTest {
    private val columnType = GeographyPointColumnType()

    @Test
    fun `같은 트랜잭션 insert 후 재읽기 시 넘어온 Coordinate 를 그대로 반환한다`() {
        // prod 500 회귀 지점: Exposed 가 캐시된 Coordinate 를 그대로 넘기면 예외 없이 동일 인스턴스를 반환해야 한다.
        // Coordinate 가 data class 라 assertEquals 는 새 객체 반환을 못 잡으므로 assertSame 으로 객체 동일성을 검증한다.
        val point = Coordinate(latitude = 37.5445, longitude = 127.0575)

        assertSame(point, columnType.valueFromDB(point))
    }

    @Test
    fun `지원하지 않는 값 타입은 IllegalStateException 을 던진다`() {
        assertThrows<IllegalStateException> { columnType.valueFromDB(42) }
    }
}
