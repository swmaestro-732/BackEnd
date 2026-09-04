package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseSearchCommand
import com.example.backend.course.application.port.inbound.CourseSearchSort
import com.example.backend.course.application.port.outbound.CourseSearchCriteria
import com.example.backend.course.application.port.outbound.CourseSearchHit
import com.example.backend.course.application.port.outbound.CourseSearchPage
import com.example.backend.course.application.port.outbound.CourseSearchQueryPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * 코스 검색 서비스 단위 테스트(DB·OpenSearch 없이 fake 포트). 커서 디코딩→criteria, 인코딩→nextCursor,
 * 빈 파라미터 정규화, 빈 페이지 패스스루를 검증한다.
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

    private fun hit(
        id: Long,
        savesCnt: Int,
        createdAt: Instant,
    ) = CourseSearchHit(
        id = id,
        authorId = 1,
        title = "코스",
        coverImageUrl = null,
        theme = null,
        area = null,
        likesCnt = 0,
        savesCnt = savesCnt,
        createdAt = createdAt,
        score = null,
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
    fun `hasNext 면 마지막 히트로 nextCursor 를 인코딩하고 재요청에 그 커서가 criteria 로 디코딩된다`() {
        val last = hit(id = 3, savesCnt = 1, createdAt = Instant.parse("2026-08-01T00:00:00Z"))
        val port = FakePort(CourseSearchPage(hits = listOf(hit(1, 5, Instant.EPOCH), last), hasNext = true))
        val service = CourseSearchService(port)

        val result = service.search(command(sort = CourseSearchSort.LATEST))

        assertThat(result.hasNext).isTrue()
        assertThat(result.nextCursor).isNotNull()
        assertThat(port.lastCriteria!!.searchAfter).isNull() // 첫 요청은 커서 없음

        // 받은 nextCursor 로 다시 요청하면 마지막 히트의 정렬 값(createdAt millis, id)이 searchAfter 로 복원된다.
        service.search(command(sort = CourseSearchSort.LATEST, cursor = result.nextCursor))
        assertThat(port.lastCriteria!!.searchAfter).containsExactly(last.createdAt.toEpochMilli(), last.id)
    }

    @Test
    fun `hasNext 가 아니면 nextCursor 는 null 이다`() {
        val port = FakePort(CourseSearchPage(hits = listOf(hit(1, 5, Instant.EPOCH)), hasNext = false))
        val result = CourseSearchService(port).search(command())

        assertThat(result.nextCursor).isNull()
        assertThat(result.hasNext).isFalse()
        assertThat(result.courses).hasSize(1)
    }

    @Test
    fun `빈 페이지를 그대로 패스스루한다(클라이언트 부재 fail-soft)`() {
        val port = FakePort(CourseSearchPage(hits = emptyList(), hasNext = false))
        val result = CourseSearchService(port).search(command(keyword = "카페"))

        assertThat(result.courses).isEmpty()
        assertThat(result.nextCursor).isNull()
        assertThat(result.hasNext).isFalse()
    }

    @Test
    fun `빈 문자열 파라미터는 필터 미적용으로 정규화된다`() {
        val port = FakePort(CourseSearchPage(hits = emptyList(), hasNext = false))
        CourseSearchService(port).search(
            command(keyword = "  ", area = "", category = "  ", tags = listOf(" 데이트 ", "", "  ")),
        )

        val criteria = port.lastCriteria!!
        assertThat(criteria.keyword).isNull()
        assertThat(criteria.area).isNull()
        assertThat(criteria.category).isNull()
        assertThat(criteria.tags).containsExactly("데이트")
    }
}
