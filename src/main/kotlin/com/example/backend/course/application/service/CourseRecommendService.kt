package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseRecommendUseCase
import com.example.backend.course.application.port.inbound.dto.CourseRecommendPage
import com.example.backend.course.application.port.inbound.dto.CourseRecommendQuery
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceStats
import com.example.backend.course.application.port.outbound.CourseRecommendAfter
import com.example.backend.course.application.port.outbound.CourseRecommendCriteria
import com.example.backend.course.application.port.outbound.CourseRecommendQueryPort
import com.example.backend.course.application.port.outbound.FeedCursor
import com.example.backend.course.domain.model.CourseCategory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

/**
 * 추천 코스 서비스 — 검색 엔진 랭킹([CourseRecommendQueryPort])을 우선 쓰고, 엔진이 없거나 실패하면 공개 피드 키셋 조회로 폴백한다.
 * 커서 출처(엔진/DB)는 [CourseRecommendCursorCodec] 이 접두사로 보존해 페이지 사이에 정렬이 바뀌지 않는다.
 * 엔진 커서로 진행 중 엔진이 실패하면 DB 로 갈아타지 않고 빈 페이지를 준다(갈아타면 중복·누락이 생긴다).
 * 카드 메타(장소 수·도보 시간)는 두 경로 모두 DB 집계([CoursePersistencePort.findPlaceStats])로 붙인다.
 * 읽기 트랜잭션은 DB 조회 구간에만 건다 — 엔진 호출 동안 커넥션을 잡아두지 않는다.
 */
@Service
class CourseRecommendService(
    private val courseRecommendQueryPort: CourseRecommendQueryPort,
    private val coursePersistencePort: CoursePersistencePort,
    transactionManager: PlatformTransactionManager,
) : CourseRecommendUseCase {
    private val readTx = TransactionTemplate(transactionManager).apply { isReadOnly = true }

    override fun recommend(query: CourseRecommendQuery): CourseRecommendPage {
        val size = query.size.coerceAtLeast(1)
        val category =
            query.category?.let { name ->
                requireNotNull(CourseCategory.entries.find { it.name == name }) { "알 수 없는 코스 카테고리입니다: $name" }
            }
        return when (val cursor = CourseRecommendCursorCodec.decode(query.cursor)) {
            null -> {
                fromEngine(query, Instant.now(), null, size, category) ?: fromFeed(null, size, category)
            }

            is CourseRecommendCursor.Engine -> {
                fromEngine(query, cursor.origin, CourseRecommendAfter(cursor.score, cursor.id), size, category) ?: EMPTY
            }

            is CourseRecommendCursor.Feed -> {
                fromFeed(cursor.value, size, category)
            }
        }
    }

    /** 엔진 랭킹 페이지. 엔진 미설정·실패면 null. */
    private fun fromEngine(
        query: CourseRecommendQuery,
        origin: Instant,
        after: CourseRecommendAfter?,
        size: Int,
        category: CourseCategory?,
    ): CourseRecommendPage? {
        val criteria =
            CourseRecommendCriteria(
                likeThemes = query.likeThemes.distinct(),
                // 사용자 코드가 동이든 시군구든 앞 5자리는 항상 시군구 매칭에 쓰고, 동 코드(뒤 5자리가 00000 이 아님)만 동 매칭에 더한다.
                sigunguCodes = query.areaCodes.map { it.take(SIGUNGU_CODE_LENGTH) }.distinct(),
                dongCodes = query.areaCodes.filterNot { it.endsWith(SIGUNGU_LEVEL_SUFFIX) }.distinct(),
                category = category?.name,
                origin = origin,
                after = after,
                size = size,
            )
        val page = courseRecommendQueryPort.recommend(criteria) ?: return null
        val statsByCourse =
            readTx
                .execute { _ ->
                    coursePersistencePort.findPlaceStats(page.hits.map { it.row.id })
                }.orEmpty()
        val nextCursor =
            page.hits.lastOrNull()?.takeIf { page.hasNext }?.let {
                CourseRecommendCursorCodec.encode(CourseRecommendCursor.Engine(origin, it.score, it.row.id))
            }
        return CourseRecommendPage(
            items = page.hits.map { it.row.toCourseSummary(statsByCourse[it.row.id] ?: CoursePlaceStats(0, 0)) },
            hasNext = page.hasNext,
            nextCursor = nextCursor,
        )
    }

    /** DB 폴백 — 공개 피드와 같은 정렬(saves_cnt DESC, created_at DESC, id DESC) 키셋 페이지. */
    private fun fromFeed(
        cursor: FeedCursor?,
        size: Int,
        category: CourseCategory?,
    ): CourseRecommendPage {
        val rows = readTx.execute { _ -> coursePersistencePort.findPublishedPublic(cursor, size, category) }.orEmpty()
        val page = rows.take(size)
        val hasNext = rows.size > size
        val statsByCourse = readTx.execute { _ -> coursePersistencePort.findPlaceStats(page.map { it.id }) }.orEmpty()
        val nextCursor =
            page.lastOrNull()?.takeIf { hasNext }?.let {
                CourseRecommendCursorCodec.encode(
                    CourseRecommendCursor.Feed(
                        FeedCursor(savesCnt = it.savesCnt, createdAt = it.createdAt, id = it.id),
                    ),
                )
            }
        return CourseRecommendPage(
            items = page.map { it.toCourseSummary(statsByCourse[it.id] ?: CoursePlaceStats(0, 0)) },
            hasNext = hasNext,
            nextCursor = nextCursor,
        )
    }

    private companion object {
        val EMPTY = CourseRecommendPage(items = emptyList(), hasNext = false, nextCursor = null)
        const val SIGUNGU_CODE_LENGTH = 5
        const val SIGUNGU_LEVEL_SUFFIX = "00000"
    }
}
