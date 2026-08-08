package com.example.backend.course.application.service

import com.example.backend.course.application.port.outbound.CourseTagQueryPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecommendedTagServiceTest {
    private val fakePort =
        object : CourseTagQueryPort {
            var placeBasedTags: List<String> = emptyList()
            var popularTags: List<String> = emptyList()

            override fun findTagNamesByPlaceIds(
                placeIds: List<Long>,
                limit: Int,
            ): List<String> = placeBasedTags.take(limit)

            override fun findPopularTagNames(limit: Int): List<String> = popularTags.take(limit)

            // 추천 태그와 무관한 포트 메서드(코스 상세 해시태그) — 이 테스트에서는 쓰이지 않는다.
            override fun findTagNamesByCourseId(courseId: Long): List<String> = emptyList()
        }

    private val service = RecommendedTagService(fakePort)

    @Test
    fun `placeIds가 있으면 장소 기반 태그를 앞세우고 인기 태그로 채운다`() {
        fakePort.placeBasedTags = listOf("감성카페", "통창뷰", "조용한")
        fakePort.popularTags = listOf("데이트", "맛집투어")

        val result = service.recommend(placeIds = listOf(1L, 2L), limit = 10)

        assertEquals(listOf("감성카페", "통창뷰", "조용한", "데이트", "맛집투어"), result)
    }

    @Test
    fun `placeIds가 비어 있으면 인기 태그를 반환한다`() {
        fakePort.popularTags = listOf("데이트", "맛집투어", "도심산책")

        val result = service.recommend(placeIds = emptyList(), limit = 10)

        assertEquals(listOf("데이트", "맛집투어", "도심산책"), result)
    }

    @Test
    fun `장소 기반 태그가 부족하면 인기 태그로 채우되 중복은 제거한다`() {
        fakePort.placeBasedTags = listOf("감성카페")
        fakePort.popularTags = listOf("데이트", "감성카페", "도심산책")

        val result = service.recommend(placeIds = listOf(1L), limit = 3)

        assertEquals(listOf("감성카페", "데이트", "도심산책"), result)
    }

    @Test
    fun `limit만큼만 반환한다`() {
        fakePort.placeBasedTags = listOf("감성카페", "통창뷰", "조용한", "웨이팅없음")

        val result = service.recommend(placeIds = listOf(1L), limit = 2)

        assertEquals(listOf("감성카페", "통창뷰"), result)
    }
}
