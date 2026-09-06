package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.exposed.CourseReviewPhotoTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseReviewTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseReviewTagLinkTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTable
import com.example.backend.course.application.port.inbound.dto.CourseReviewSortKey
import com.example.backend.course.application.port.outbound.CourseRatingCounters
import com.example.backend.course.application.port.outbound.CourseReviewCursor
import com.example.backend.course.application.port.outbound.CourseReviewRow
import com.example.backend.course.domain.model.CourseReviewStatus
import com.example.backend.course.domain.model.CourseReviewTag
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
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
 * course_reviews 읽기 리포지토리 — 후기 목록(커서 페이지)과 코스 전체 집계를 담당한다.
 * 쓰기는 [CourseReviewRepository] 가 맡는다(커맨드/쿼리 분리).
 *
 * 정렬은 어느 기준이든 (기준값, created_at, id) 로 완전히 순서를 정해 동점에서도 커서가 흔들리지 않는다.
 */
@Repository
class CourseReviewQueryRepository {
    fun findReviewsByCourse(
        courseId: Long,
        sort: CourseReviewSortKey,
        descending: Boolean,
        cursor: CourseReviewCursor?,
        limit: Int,
    ): List<CourseReviewRow> {
        var condition = alive(courseId)
        cursor?.let { condition = condition and afterCursor(sort, descending, it) }

        val direction = if (descending) SortOrder.DESC else SortOrder.ASC
        val orderBy =
            buildList {
                if (sort == CourseReviewSortKey.RATING) add(CourseReviewTable.rating to direction)
                add(CourseReviewTable.createdAt to direction)
                add(CourseReviewTable.id to direction)
            }.toTypedArray()

        return CourseReviewTable
            .selectAll()
            .where(condition)
            .orderBy(*orderBy)
            .limit(limit)
            .map {
                CourseReviewRow(
                    id = it[CourseReviewTable.id].value,
                    userId = it[CourseReviewTable.userId],
                    rating = it[CourseReviewTable.rating].toInt(),
                    content = it[CourseReviewTable.content],
                    createdAt = it[CourseReviewTable.createdAt].toJavaInstant(),
                )
            }
    }

    fun findPhotoUrls(reviewIds: List<Long>): Map<Long, List<String>> {
        if (reviewIds.isEmpty()) return emptyMap()
        return CourseReviewPhotoTable
            .selectAll()
            .where { CourseReviewPhotoTable.courseReviewId inList reviewIds }
            .orderBy(CourseReviewPhotoTable.orderNo to SortOrder.ASC)
            .groupBy({ it[CourseReviewPhotoTable.courseReviewId] }, { it[CourseReviewPhotoTable.imageUrl] })
    }

    fun findTags(reviewIds: List<Long>): Map<Long, List<CourseReviewTag>> {
        if (reviewIds.isEmpty()) return emptyMap()
        return CourseReviewTagLinkTable
            .selectAll()
            .where { CourseReviewTagLinkTable.courseReviewId inList reviewIds }
            .groupBy({ it[CourseReviewTagLinkTable.courseReviewId] }, { it[CourseReviewTagLinkTable.tag] })
    }

    /** 코스 전체의 살아있는 리뷰를 별점별로 센다. */
    fun countReviewsByRating(courseId: Long): Map<Int, Long> {
        val reviewCount = CourseReviewTable.id.count()
        return CourseReviewTable
            .select(CourseReviewTable.rating, reviewCount)
            .where(alive(courseId))
            .groupBy(CourseReviewTable.rating)
            .associate { it[CourseReviewTable.rating].toInt() to it[reviewCount] }
    }

    /** 코스 전체의 살아있는 리뷰에 달린 사진을 센다. */
    fun countPhotosByCourse(courseId: Long): Long =
        CourseReviewPhotoTable
            .join(
                CourseReviewTable,
                JoinType.INNER,
                CourseReviewPhotoTable.courseReviewId,
                CourseReviewTable.id,
            ).selectAll()
            .where(alive(courseId))
            .count()

    /** courses 별점 카운터(V8) 단건 조회 — 삭제된 코스는 제외한다. */
    fun findRatingCounters(courseId: Long): CourseRatingCounters? =
        CourseTable
            .select(CourseTable.ratingSum, CourseTable.ratingCnt)
            .where { (CourseTable.id eq courseId) and CourseTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let {
                CourseRatingCounters(
                    ratingSum = it[CourseTable.ratingSum],
                    ratingCnt = it[CourseTable.ratingCnt],
                )
            }

    /** 커서 뒤(내림차순이면 더 작은 쪽)만 남기는 키셋 조건. 정렬 컬럼 순서와 반드시 일치해야 한다. */
    private fun afterCursor(
        sort: CourseReviewSortKey,
        descending: Boolean,
        cursor: CourseReviewCursor,
    ): Op<Boolean> {
        val createdAt = cursor.createdAt.toKotlinInstant()
        val afterCreatedAt =
            if (descending) {
                (CourseReviewTable.createdAt less createdAt) or
                    ((CourseReviewTable.createdAt eq createdAt) and (CourseReviewTable.id less cursor.id))
            } else {
                (CourseReviewTable.createdAt greater createdAt) or
                    ((CourseReviewTable.createdAt eq createdAt) and (CourseReviewTable.id greater cursor.id))
            }
        if (sort == CourseReviewSortKey.LATEST) return afterCreatedAt

        val rating = cursor.rating.toShort()
        return if (descending) {
            (CourseReviewTable.rating less rating) or ((CourseReviewTable.rating eq rating) and afterCreatedAt)
        } else {
            (CourseReviewTable.rating greater rating) or ((CourseReviewTable.rating eq rating) and afterCreatedAt)
        }
    }

    /** 코스의 살아있는(소프트 삭제·숨김 제외) 리뷰 필터. */
    private fun alive(courseId: Long): Op<Boolean> =
        (CourseReviewTable.courseId eq courseId) and
            (CourseReviewTable.status eq CourseReviewStatus.PUBLISHED) and
            CourseReviewTable.deletedAt.isNull()
}
