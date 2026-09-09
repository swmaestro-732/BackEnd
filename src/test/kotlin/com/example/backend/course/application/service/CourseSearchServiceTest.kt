package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseSearchCommand
import com.example.backend.course.application.port.inbound.CourseSearchSort
import com.example.backend.course.application.port.inbound.dto.CourseSearchHit
import com.example.backend.course.application.port.outbound.CourseSearchCriteria
import com.example.backend.course.application.port.outbound.CourseSearchPage
import com.example.backend.course.application.port.outbound.CourseSearchQueryPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * 코스 검색 서비스 단위 테스트(fake 포트). 커서는 불투명이라 서비스가 해석하지 않는다 —
 * command.cursor 를 criteria.cursor 로 그대로 넘기고 page.nextCursor 를 결과로 그대로 돌려주는지,
 * 빈 파라미터 정규화·빈 페이지 패스스루를 검증한다.
 */
class CourseSearchServiceTest {
    private class FakePort(
        private val page: CourseSearchPage,
    ) : CourseSearchQueryPort {
        var lastCriteria: CourseSearchCriteria? = null

        override fun search(criteria: CourseSearchCriteria): CourseSearchPage {
            lastCriteria = criteria
            return page
        }
    }

    private fun hit(id: Long) =
        CourseSearchHit(
            id = id,
            authorId = 1,
            title = "코스",
            coverImageUrl = null,
            theme = null,
            area = null,
            likesCnt = 0,
            savesCnt = 0,
            createdAt = Instant.EPOCH,
        )

    private fun command(
        keyword: String? = null,
        area: String? = null,
        category: String? = null,
        tags: List<String> = emptyList(),
        sort: CourseSearchSort = CourseSearchSort.LATEST,
        cursor: String? = null,
        size: Int = 20,
    ) = CourseSearchCommand(keyword, area, category, tags, sort, cursor, size)

    @Test
    fun `커서와 nextCursor 를 그대로 오가게 한다(어댑터가 해석)`() {
        val port = FakePort(CourseSearchPage(hits = listOf(hit(1)), hasNext = true, nextCursor = "NEXT"))
        val service = CourseSearchService(port)

        val result = service.search(command(cursor = "PREV"))

        assertThat(port.lastCriteria!!.cursor).isEqualTo("PREV") // 요청 커서 그대로 전달
        assertThat(result.nextCursor).isEqualTo("NEXT") // 페이지의 다음 커서 그대로 반환
        assertThat(result.hasNext).isTrue()
    }

    @Test
    fun `hasNext 가 아니면 nextCursor 는 null 이다`() {
        val port = FakePort(CourseSearchPage(hits = listOf(hit(1)), hasNext = false, nextCursor = null))
        val result = CourseSearchService(port).search(command())

        assertThat(result.nextCursor).isNull()
        assertThat(result.hasNext).isFalse()
        assertThat(result.courses).hasSize(1)
    }

    @Test
    fun `빈 페이지를 그대로 패스스루한다(클라이언트 부재 fail-soft)`() {
        val port = FakePort(CourseSearchPage(hits = emptyList(), hasNext = false, nextCursor = null))
        val result = CourseSearchService(port).search(command(keyword = "카페"))

        assertThat(result.courses).isEmpty()
        assertThat(result.nextCursor).isNull()
        assertThat(result.hasNext).isFalse()
    }

    @Test
    fun `빈 문자열 파라미터는 필터 미적용으로 정규화된다`() {
        val port = FakePort(CourseSearchPage(hits = emptyList(), hasNext = false, nextCursor = null))
        CourseSearchService(port).search(
            command(keyword = "  ", area = "", category = "  ", tags = listOf(" 데이트 ", "", "  ")),
        )

        val criteria = port.lastCriteria!!
        assertThat(criteria.keyword).isNull()
        assertThat(criteria.area).isNull()
        assertThat(criteria.category).isNull()
        assertThat(criteria.tags).containsExactly("데이트")
    }

    @Test
    fun `area 와 category 는 앞뒤 공백을 제거해 전달한다`() {
        val port = FakePort(CourseSearchPage(hits = emptyList(), hasNext = false, nextCursor = null))
        CourseSearchService(port).search(command(area = "  서울  ", category = " 카페 "))

        val criteria = port.lastCriteria!!
        assertThat(criteria.area).isEqualTo("서울")
        assertThat(criteria.category).isEqualTo("카페")
    }
}
