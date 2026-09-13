package com.example.backend.place.adapter.outbound.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PlaceDocumentTest {
    @Test
    fun `PlaceDocument 생성 — 모든 필드가 그대로 저장된다`() {
        val location = GeoLocation(lat = 37.5445, lon = 127.0578)
        val doc =
            PlaceDocument(
                name = "어니언 성수",
                description = "성수동 유명 카페",
                category = "CAFE",
                address = "서울 성동구 성수이로7가길 11",
                areaCode = "1168010100",
                location = location,
                status = "ACTIVE",
            )

        assertEquals("어니언 성수", doc.name)
        assertEquals("성수동 유명 카페", doc.description)
        assertEquals("CAFE", doc.category)
        assertEquals("서울 성동구 성수이로7가길 11", doc.address)
        assertEquals("1168010100", doc.areaCode)
        assertEquals(37.5445, doc.location.lat)
        assertEquals(127.0578, doc.location.lon)
        assertEquals("ACTIVE", doc.status)
    }

    @Test
    fun `PlaceDocument 생성 — nullable 필드는 null 을 허용한다`() {
        val doc =
            PlaceDocument(
                name = "장소",
                description = null,
                category = "RESTAURANT",
                address = "주소",
                areaCode = null,
                location = GeoLocation(0.0, 0.0),
                status = "ACTIVE",
            )

        assertNull(doc.description)
        assertNull(doc.areaCode)
    }

    @Test
    fun `GeoLocation 생성 — lat lon 이 정확히 저장된다`() {
        val geo = GeoLocation(lat = 37.123456, lon = 127.654321)

        assertEquals(37.123456, geo.lat)
        assertEquals(127.654321, geo.lon)
    }

    @Test
    fun `PlaceDocument equals — 같은 값이면 동일하다`() {
        val loc = GeoLocation(1.0, 2.0)
        val a = PlaceDocument("n", null, "CAFE", "addr", null, loc, "ACTIVE")
        val b = PlaceDocument("n", null, "CAFE", "addr", null, loc, "ACTIVE")

        assertEquals(a, b)
    }
}
