package com.example.backend.course.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.port.inbound.dto.CourseRecommendQuery
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceStats
import com.example.backend.course.application.port.outbound.CourseRecommendAfter
import com.example.backend.course.application.port.outbound.CourseRecommendCriteria
import com.example.backend.course.application.port.outbound.CourseRecommendHit
import com.example.backend.course.application.port.outbound.CourseRecommendQueryPort
import com.example.backend.course.application.port.outbound.CourseRecommendResultPage
import com.example.backend.course.application.port.outbound.CourseSearchRow
import com.example.backend.course.application.port.outbound.CourseSummaryRow
import com.example.backend.course.application.port.outbound.FeedCursor
import com.example.backend.course.domain.model.CourseCategory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * [CourseRecommendService] — 엔진 우선·DB 폴백 분기, 관심 지역 코드 분할, 커서 출처 보존, 카드 메타(장소 수·도보 분) 병합을 검증한다.
 */
class CourseRecommendServiceTest {
    private val recommendPort = mock(CourseRecommendQueryPort::class.java)
    private val persistence = mock(CoursePersistencePort::class.java)
    private val transactionManager =
        mock(PlatformTransactionManager::class.java).also {
            `when`(it.getTransaction(any())).thenReturn(SimpleTransactionStatus())
        }
    private val areaQueryUseCase = mock(AreaQueryUseCase::class.java)
    private val service =
        CourseRecommendService(recommendPort, persistence, CourseRegionResolver(areaQueryUseCase), transactionManager)

    private val createdAt = Instant.parse("2026-09-01T00:00:00Z")

    private fun <T> anyArg(): T = ArgumentMatchers.any()

    private fun <T> capture(captor: ArgumentCaptor<T>): T = captor.capture()

    private fun row(id: Long) =
        CourseSearchRow(
            id = id,
            authorId = 7L,
            title = "코스 $id",
            coverImageUrl = "https://img/$id.jpg",
            themes = listOf("CAFETOUR"),
            area = "성수",
            likesCnt = 1,
            savesCnt = 2,
            createdAt = createdAt,
        )

    private fun summaryRow(
        id: Long,
        savesCnt: Int = 5,
    ) = CourseSummaryRow(
        id = id,
        userId = 3L,
        title = "코스 $id",
        coverImageUrl = null,
        categories = listOf(CourseCategory.CAFETOUR),
        area = "성수",
        visibility = CourseVisibility.PUBLIC,
        isPublished = true,
        likesCnt = 1,
        savesCnt = savesCnt,
        createdAt = createdAt,
    )

    private fun query(
        likeThemes: List<String> = listOf("CAFETOUR"),
        areaCodes: List<String> = listOf("1168000000", "1168010100"),
        cursor: String? = null,
        size: Int = 2,
        category: String? = null,
    ) = CourseRecommendQuery(
        likeThemes = likeThemes,
        areaCodes = areaCodes,
        cursor = cursor,
        size = size,
        category = category,
    )

    private fun enginePage(
        hits: List<CourseRecommendHit>,
        hasNext: Boolean,
    ) {
        `when`(recommendPort.recommend(anyArg<CourseRecommendCriteria>()))
            .thenReturn(CourseRecommendResultPage(hits = hits, hasNext = hasNext))
    }

    private fun capturedCriteria(): CourseRecommendCriteria {
        val captor: ArgumentCaptor<CourseRecommendCriteria> =
            ArgumentCaptor.forClass(
                CourseRecommendCriteria::class.java,
            )
        verify(recommendPort).recommend(capture(captor))
        return captor.value
    }

    @Test
    fun `첫 페이지는 엔진 랭킹을 쓰고 관심 지역은 시군구·읍면동으로 갈라 넘긴다`() {
        enginePage(listOf(CourseRecommendHit(row(1), 9.0), CourseRecommendHit(row(2), 8.0)), hasNext = true)
        `when`(persistence.findPlaceStats(listOf(1L, 2L))).thenReturn(mapOf(1L to CoursePlaceStats(4, 30)))
        val before = Instant.now()

        val page =
            service.recommend(
                query(
                    likeThemes = listOf("CAFETOUR", "CAFETOUR"),
                    areaCodes = listOf("1168000000", "1168010100", "1171010200"),
                    category = "FOOD",
                ),
            )

        val criteria = capturedCriteria()
        assertThat(criteria.likeThemes).containsExactly("CAFETOUR")
        assertThat(criteria.sigunguCodes).containsExactly("11680", "11710")
        assertThat(criteria.dongCodes).containsExactly("1168010100", "1171010200")
        assertThat(criteria.category).isEqualTo("FOOD")
        assertThat(criteria.after).isNull()
        assertThat(criteria.size).isEqualTo(2)
        assertThat(criteria.origin).isAfterOrEqualTo(before)

        assertThat(page.hasNext).isTrue()
        assertThat(page.items.map { it.id }).containsExactly(1L, 2L)
        val first = page.items[0]
        assertThat(first.authorId).isEqualTo(7L)
        assertThat(first.title).isEqualTo("코스 1")
        assertThat(first.themes).containsExactly("CAFETOUR")
        assertThat(first.placeCount).isEqualTo(4)
        assertThat(first.walkingMinutes).isEqualTo(30)
        assertThat(page.items[1].placeCount).isEqualTo(0)
        assertThat(page.items[1].walkingMinutes).isEqualTo(0)

        val next = CourseRecommendCursorCodec.decode(page.nextCursor) as CourseRecommendCursor.Engine
        assertThat(next.origin).isEqualTo(criteria.origin.truncatedTo(ChronoUnit.MILLIS)) // 커서는 밀리초 정밀도
        assertThat(next.score).isEqualTo(8.0)
        assertThat(next.id).isEqualTo(2L)
        verify(persistence, never()).findPublishedPublic(any(), anyInt(), any(), any())
    }

    @Test
    fun `엔진 결과에 다음 페이지가 없으면 커서를 비운다`() {
        enginePage(listOf(CourseRecommendHit(row(1), 9.0)), hasNext = false)
        `when`(persistence.findPlaceStats(anyList())).thenReturn(emptyMap())

        val page = service.recommend(query())

        assertThat(page.hasNext).isFalse()
        assertThat(page.nextCursor).isNull()
    }

    @Test
    fun `엔진 커서는 저장된 origin 과 점수·id 를 search_after 로 이어 넘긴다`() {
        val origin = Instant.parse("2026-11-01T00:00:00Z")
        val cursor = CourseRecommendCursorCodec.encode(CourseRecommendCursor.Engine(origin, 8.0, 2L))
        enginePage(listOf(CourseRecommendHit(row(3), 7.0)), hasNext = false)
        `when`(persistence.findPlaceStats(anyList())).thenReturn(emptyMap())

        val page = service.recommend(query(cursor = cursor))

        val criteria = capturedCriteria()
        assertThat(criteria.origin).isEqualTo(origin)
        assertThat(criteria.after).isEqualTo(CourseRecommendAfter(8.0, 2L))
        assertThat(page.items.map { it.id }).containsExactly(3L)
    }

    @Test
    fun `엔진 커서로 진행 중 엔진이 실패하면 DB 로 갈아타지 않고 빈 페이지를 준다`() {
        val cursor = CourseRecommendCursorCodec.encode(CourseRecommendCursor.Engine(Instant.now(), 8.0, 2L))
        `when`(recommendPort.recommend(anyArg<CourseRecommendCriteria>())).thenReturn(null)

        val page = service.recommend(query(cursor = cursor))

        assertThat(page.items).isEmpty()
        assertThat(page.hasNext).isFalse()
        assertThat(page.nextCursor).isNull()
        verify(persistence, never()).findPublishedPublic(any(), anyInt(), any(), any())
    }

    @Test
    fun `첫 페이지에서 엔진이 없으면 공개 피드 정렬로 폴백하고 db 커서를 만든다`() {
        `when`(recommendPort.recommend(anyArg<CourseRecommendCriteria>())).thenReturn(null)
        `when`(persistence.findPublishedPublic(isNull(), eq(2), eq(CourseCategory.FOOD), isNull()))
            .thenReturn(listOf(summaryRow(10, savesCnt = 9), summaryRow(11, savesCnt = 8), summaryRow(12)))
        `when`(persistence.findPlaceStats(listOf(10L, 11L))).thenReturn(mapOf(11L to CoursePlaceStats(3, 20)))

        val page = service.recommend(query(category = "FOOD"))

        assertThat(page.hasNext).isTrue()
        assertThat(page.items.map { it.id }).containsExactly(10L, 11L)
        val first = page.items[0]
        assertThat(first.authorId).isEqualTo(3L)
        assertThat(first.themes).containsExactly("CAFETOUR")
        assertThat(first.savesCnt).isEqualTo(9)
        assertThat(first.placeCount).isEqualTo(0)
        assertThat(page.items[1].placeCount).isEqualTo(3)
        assertThat(page.items[1].walkingMinutes).isEqualTo(20)

        val next = CourseRecommendCursorCodec.decode(page.nextCursor) as CourseRecommendCursor.Feed
        assertThat(next.value).isEqualTo(FeedCursor(savesCnt = 8, createdAt = createdAt, id = 11L))
    }

    @Test
    fun `db 커서는 엔진을 부르지 않고 키셋으로 이어 조회한다`() {
        val feedCursor = FeedCursor(savesCnt = 8, createdAt = createdAt, id = 11L)
        val cursor = CourseRecommendCursorCodec.encode(CourseRecommendCursor.Feed(feedCursor))
        `when`(
            persistence.findPublishedPublic(eq(feedCursor), eq(2), isNull(), isNull()),
        ).thenReturn(listOf(summaryRow(12)))
        `when`(persistence.findPlaceStats(listOf(12L))).thenReturn(emptyMap())

        val page = service.recommend(query(cursor = cursor))

        assertThat(page.items.map { it.id }).containsExactly(12L)
        assertThat(page.hasNext).isFalse()
        assertThat(page.nextCursor).isNull()
        verify(recommendPort, never()).recommend(anyArg<CourseRecommendCriteria>())
    }

    @Test
    fun `size 는 최소 1 로 보정한다`() {
        enginePage(emptyList(), hasNext = false)
        `when`(persistence.findPlaceStats(anyList())).thenReturn(emptyMap())

        service.recommend(query(size = 0))

        assertThat(capturedCriteria().size).isEqualTo(1)
    }

    @Test
    fun `알 수 없는 카테고리는 거절한다`() {
        assertThatThrownBy { service.recommend(query(category = "UNKNOWN")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("UNKNOWN")
    }
}
