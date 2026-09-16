package com.example.backend.place.adapter.outbound.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class KakaoLocalDocumentTest {
    @Test
    fun `모든 필드가 채워진 KakaoLocalDocument 를 생성하면 값이 그대로 저장된다`() {
        val doc =
            KakaoLocalDocument(
                id = "12345",
                placeName = "어니언 성수",
                categoryName = "음식점 > 카페",
                categoryGroupCode = "CE7",
                roadAddressName = "서울 성동구 성수이로7가길 11",
                addressName = "서울 성동구 성수동2가 123",
                x = "127.0578",
                y = "37.5445",
                phone = "02-1234-5678",
            )

        assertEquals("12345", doc.id)
        assertEquals("어니언 성수", doc.placeName)
        assertEquals("음식점 > 카페", doc.categoryName)
        assertEquals("CE7", doc.categoryGroupCode)
        assertEquals("서울 성동구 성수이로7가길 11", doc.roadAddressName)
        assertEquals("서울 성동구 성수동2가 123", doc.addressName)
        assertEquals("127.0578", doc.x)
        assertEquals("37.5445", doc.y)
        assertEquals("02-1234-5678", doc.phone)
    }

    @Test
    fun `기본 생성자로 만든 KakaoLocalDocument 는 모든 필드가 null 이다`() {
        val doc = KakaoLocalDocument()

        assertNull(doc.id)
        assertNull(doc.placeName)
        assertNull(doc.categoryName)
        assertNull(doc.categoryGroupCode)
        assertNull(doc.roadAddressName)
        assertNull(doc.addressName)
        assertNull(doc.x)
        assertNull(doc.y)
        assertNull(doc.phone)
    }

    @Test
    fun `KakaoLocalDocument equals — 같은 값이면 동일하다`() {
        val a = KakaoLocalDocument(id = "1", placeName = "장소A", categoryName = "카페")
        val b = KakaoLocalDocument(id = "1", placeName = "장소A", categoryName = "카페")

        assertEquals(a, b)
    }
}
