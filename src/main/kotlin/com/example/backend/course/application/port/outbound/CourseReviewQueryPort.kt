package com.example.backend.course.application.port.outbound

import com.example.backend.course.application.port.inbound.dto.CourseReviewSortKey
import com.example.backend.course.domain.model.CourseReviewTag
import java.time.Instant
import kotlin.math.round

/**
 * 아웃바운드 포트 — 코스 리뷰(course_reviews) 읽기 계약.
 * 모든 조회는 살아있는(deleted_at IS NULL, status=PUBLISHED) 리뷰만 대상으로 한다.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface CourseReviewQueryPort {
    /**
     * 리뷰 한 페이지를 정렬 기준대로 읽는다. [cursor] 가 있으면 그 뒤부터, [limit] 개까지.
     * 자식(사진·태그)은 담지 않는다 — 항목별 조회(N+1)를 피하려고 [findPhotoUrls]·[findTags] 로 따로 모은다.
     */
    fun findReviewsByCourse(
        courseId: Long,
        sort: CourseReviewSortKey,
        descending: Boolean,
        cursor: CourseReviewCursor?,
        limit: Int,
    ): List<CourseReviewRow>

    /** 리뷰 id 별 사진 URL — 노출 순서(order_no) 오름차순. */
    fun findPhotoUrls(reviewIds: List<Long>): Map<Long, List<String>>

    /** 리뷰 id 별 태그. 저장된 값이 곧 enum 이름이라 마스터 조회가 없다(V7). */
    fun findTags(reviewIds: List<Long>): Map<Long, List<CourseReviewTag>>

    /** 코스 전체의 별점별 리뷰 수. 실제 존재하는 별점만 반환하며 페이지와 무관하다. */
    fun countReviewsByRating(courseId: Long): Map<Int, Long>

    /** 코스 전체의 살아있는 리뷰에 달린 사진 수. 페이지와 무관하다. */
    fun countPhotosByCourse(courseId: Long): Long

    /**
     * 코스의 별점 비정규화 카운터(courses.rating_sum·rating_cnt — V8). 없는(삭제된) 코스면 null.
     * 장소는 Place 애그리거트가 카운터를 들지만, 코스는 Course reconstitute 전 경로 파급을 피해 읽기 포트로 따로 조회한다.
     */
    fun findRatingCounters(courseId: Long): CourseRatingCounters?
}

/** 리뷰 읽기 모델 — 자식(사진·태그) 없이 본문만. */
data class CourseReviewRow(
    val id: Long,
    val userId: Long,
    val rating: Int,
    val content: String?,
    val createdAt: Instant,
)

/**
 * 커서 키셋 — 정렬 기준값과 함께 (작성일, id)까지 담아 동점에서도 페이지 경계가 흔들리지 않게 한다.
 * [rating] 은 평점 정렬일 때만 쓴다(최신순이면 무시).
 */
data class CourseReviewCursor(
    val rating: Int,
    val createdAt: Instant,
    val id: Long,
)

/** 별점 비정규화 카운터 읽기 모델. 평균은 소수 첫째 자리 반올림(Place.averageRating 과 같은 규칙). */
data class CourseRatingCounters(
    val ratingSum: Long,
    val ratingCnt: Int,
) {
    val averageRating: Double
        get() = if (ratingCnt == 0) 0.0 else round(ratingSum * 10.0 / ratingCnt) / 10.0
}
