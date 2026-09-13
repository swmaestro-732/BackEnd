package com.example.backend.course.adapter.outbound.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CourseDocumentTest {
    @Test
    fun `CourseDocument 생성 — 모든 필드가 그대로 저장된다`() {
        val doc =
            CourseDocument(
                title = "성수 카페 투어",
                description = "성수동 카페 3곳",
                area = "서울 성동구",
                visibility = "PUBLIC",
                isPublished = true,
                userId = "42",
                likesCnt = 10,
                savesCnt = 5,
                createdAt = 1_700_000_000_000L,
            )

        assertEquals("성수 카페 투어", doc.title)
        assertEquals("성수동 카페 3곳", doc.description)
        assertEquals("서울 성동구", doc.area)
        assertEquals("PUBLIC", doc.visibility)
        assertEquals(true, doc.isPublished)
        assertEquals("42", doc.userId)
        assertEquals(10, doc.likesCnt)
        assertEquals(5, doc.savesCnt)
        assertEquals(1_700_000_000_000L, doc.createdAt)
    }

    @Test
    fun `CourseDocument 생성 — nullable 필드는 null 을 허용한다`() {
        val doc =
            CourseDocument(
                title = "제목만",
                description = null,
                area = null,
                visibility = "PRIVATE",
                isPublished = false,
                userId = "1",
                likesCnt = 0,
                savesCnt = 0,
                createdAt = null,
            )

        assertNull(doc.description)
        assertNull(doc.area)
        assertNull(doc.createdAt)
    }

    @Test
    fun `CourseDocument equals — 같은 값이면 동일하다`() {
        val a = CourseDocument("t", null, null, "PUBLIC", false, "1", 0, 0, null)
        val b = CourseDocument("t", null, null, "PUBLIC", false, "1", 0, 0, null)

        assertEquals(a, b)
    }
}
