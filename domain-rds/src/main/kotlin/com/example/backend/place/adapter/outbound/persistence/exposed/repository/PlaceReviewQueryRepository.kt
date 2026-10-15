package com.example.backend.place.adapter.outbound.persistence.exposed.repository

import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceReviewPhotoTable
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceReviewTable
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceReviewTagLinkTable
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceTable
import com.example.backend.place.application.port.inbound.dto.PlaceReviewSortKey
import com.example.backend.place.application.port.outbound.PlaceReviewCounters
import com.example.backend.place.application.port.outbound.PlaceReviewCursor
import com.example.backend.place.application.port.outbound.PlaceReviewRow
import com.example.backend.place.domain.model.PlaceReviewStatus
import com.example.backend.place.domain.model.PlaceReviewTag
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/**
 * place_reviews 읽기 리포지토리 — 후기 목록(커서 페이지)과 장소 전체 집계를 담당한다.
 * 쓰기는 [PlaceReviewRepository] 가 맡는다(커맨드/쿼리 분리).
 *
 * 정렬은 어느 기준이든 (기준값, created_at, id) 로 완전히 순서를 정해 동점에서도 커서가 흔들리지 않는다.
 */
@Repository
class PlaceReviewQueryRepository {
    fun findReviewsByPlace(
        placeId: Long,
        sort: PlaceReviewSortKey,
        descending: Boolean,
        cursor: PlaceReviewCursor?,
        limit: Int,
    ): List<PlaceReviewRow> {
        var condition = alive(placeId)
        cursor?.let { condition = condition and afterCursor(sort, descending, it) }

        val direction = if (descending) SortOrder.DESC else SortOrder.ASC
        val orderBy =
            buildList {
                if (sort == PlaceReviewSortKey.RATING) add(PlaceReviewTable.rating to direction)
                add(PlaceReviewTable.createdAt to direction)
                add(PlaceReviewTable.id to direction)
            }.toTypedArray()

        return PlaceReviewTable
            .selectAll()
            .where(condition)
            .orderBy(*orderBy)
            .limit(limit)
            .map {
                PlaceReviewRow(
                    id = it[PlaceReviewTable.id].value,
                    userId = it[PlaceReviewTable.userId],
                    rating = it[PlaceReviewTable.rating].toInt(),
                    content = it[PlaceReviewTable.content],
                    createdAt = it[PlaceReviewTable.createdAt].toJavaInstant(),
                )
            }
    }

    fun findPhotoUrls(reviewIds: List<Long>): Map<Long, List<String>> {
        if (reviewIds.isEmpty()) return emptyMap()
        return PlaceReviewPhotoTable
            .selectAll()
            .where { PlaceReviewPhotoTable.placeReviewId inList reviewIds }
            .orderBy(PlaceReviewPhotoTable.orderNo to SortOrder.ASC)
            .groupBy({ it[PlaceReviewPhotoTable.placeReviewId] }, { it[PlaceReviewPhotoTable.imageUrl] })
    }

    fun findTags(reviewIds: List<Long>): Map<Long, List<PlaceReviewTag>> {
        if (reviewIds.isEmpty()) return emptyMap()
        return PlaceReviewTagLinkTable
            .selectAll()
            .where { PlaceReviewTagLinkTable.placeReviewId inList reviewIds }
            .groupBy({ it[PlaceReviewTagLinkTable.placeReviewId] }, { it[PlaceReviewTagLinkTable.tag] })
    }

    /** places 리뷰 카운터(V6·V13) 단건 조회 — 삭제된 장소는 제외한다. */
    fun findCounters(placeId: Long): PlaceReviewCounters? =
        PlaceTable
            .select(
                PlaceTable.ratingSum,
                PlaceTable.ratingCnt,
                PlaceTable.reviewPhotoCnt,
                PlaceTable.rating1Cnt,
                PlaceTable.rating2Cnt,
                PlaceTable.rating3Cnt,
                PlaceTable.rating4Cnt,
                PlaceTable.rating5Cnt,
            ).where { (PlaceTable.id eq placeId) and PlaceTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let { row ->
                PlaceReviewCounters(
                    ratingSum = row[PlaceTable.ratingSum],
                    ratingCnt = row[PlaceTable.ratingCnt],
                    reviewPhotoCnt = row[PlaceTable.reviewPhotoCnt],
                    ratingCounts = (1..5).associateWith { row[PlaceTable.ratingCountColumn(it)] },
                )
            }

    /** 커서 뒤(내림차순이면 더 작은 쪽)만 남기는 키셋 조건. 정렬 컬럼 순서와 반드시 일치해야 한다. */
    private fun afterCursor(
        sort: PlaceReviewSortKey,
        descending: Boolean,
        cursor: PlaceReviewCursor,
    ): Op<Boolean> {
        val createdAt = cursor.createdAt.toKotlinInstant()
        val afterCreatedAt =
            if (descending) {
                (PlaceReviewTable.createdAt less createdAt) or
                    ((PlaceReviewTable.createdAt eq createdAt) and (PlaceReviewTable.id less cursor.id))
            } else {
                (PlaceReviewTable.createdAt greater createdAt) or
                    ((PlaceReviewTable.createdAt eq createdAt) and (PlaceReviewTable.id greater cursor.id))
            }
        if (sort == PlaceReviewSortKey.LATEST) return afterCreatedAt

        val rating = cursor.rating.toShort()
        return if (descending) {
            (PlaceReviewTable.rating less rating) or ((PlaceReviewTable.rating eq rating) and afterCreatedAt)
        } else {
            (PlaceReviewTable.rating greater rating) or ((PlaceReviewTable.rating eq rating) and afterCreatedAt)
        }
    }

    /** 장소의 살아있는(소프트 삭제·숨김 제외) 리뷰 필터. */
    private fun alive(placeId: Long): Op<Boolean> =
        (PlaceReviewTable.placeId eq placeId) and
            (PlaceReviewTable.status eq PlaceReviewStatus.PUBLISHED) and
            PlaceReviewTable.deletedAt.isNull()
}
