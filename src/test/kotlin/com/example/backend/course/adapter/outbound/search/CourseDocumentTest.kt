package com.example.backend.course.adapter.outbound.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * [CourseDocument] 단위 테스트 — 색인·검색 양쪽이 공유하는 문서 매핑 값 객체의 생성·동등성·복사를 검증한다.
 */
class CourseDocumentTest {
    private fun sample() =
        CourseDocument(
            id = 1L,
            title = "성수 카페 코스",
            description = "설명",
            area = "성수",
            category = "CAFETOUR",
            tags = listOf("데이트", "힐링"),
            coverImageUrl = "https://img/1.jpg",
            visibility = "PUBLIC",
            isPublished = true,
            userId = "7",
            likesCnt = 3,
            savesCnt = 11,
            createdAt = 1000L,
        )

    @Test
    fun `필드를 그대로 보관한다`() {
        val doc = sample()

        assertThat(doc.id).isEqualTo(1L)
        assertThat(doc.title).isEqualTo("성수 카페 코스")
        assertThat(doc.description).isEqualTo("설명")
        assertThat(doc.area).isEqualTo("성수")
        assertThat(doc.category).isEqualTo("CAFETOUR")
        assertThat(doc.tags).containsExactly("데이트", "힐링")
        assertThat(doc.coverImageUrl).isEqualTo("https://img/1.jpg")
        assertThat(doc.visibility).isEqualTo("PUBLIC")
        assertThat(doc.isPublished).isTrue()
        assertThat(doc.userId).isEqualTo("7")
        assertThat(doc.likesCnt).isEqualTo(3)
        assertThat(doc.savesCnt).isEqualTo(11)
        assertThat(doc.createdAt).isEqualTo(1000L)
    }

    @Test
    fun `nullable 필드는 null 을 허용한다`() {
        val doc =
            CourseDocument(
                id = 2L,
                title = "코스",
                description = null,
                area = null,
                category = null,
                tags = emptyList(),
                coverImageUrl = null,
                visibility = "PRIVATE",
                isPublished = false,
                userId = "9",
                likesCnt = 0,
                savesCnt = 0,
                createdAt = null,
            )

        assertThat(doc.description).isNull()
        assertThat(doc.area).isNull()
        assertThat(doc.category).isNull()
        assertThat(doc.coverImageUrl).isNull()
        assertThat(doc.createdAt).isNull()
    }

    @Test
    fun `동등성과 복사, toString 이 동작한다`() {
        val doc = sample()

        assertThat(doc).isEqualTo(sample())
        assertThat(doc.hashCode()).isEqualTo(sample().hashCode())
        assertThat(doc.copy(title = "다른 제목")).isNotEqualTo(doc)
        assertThat(doc.copy()).isEqualTo(doc)
        assertThat(doc.toString()).contains("성수 카페 코스")
        assertThat(doc.component1()).isEqualTo(1L)
    }
}
