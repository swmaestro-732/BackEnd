package com.example.backend.course.application.port.outbound

import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility

/** 코스 단건 읽기 모델. deleted_at IS NULL 인 행만 반환한다(상태·공개범위 판정은 서비스가 수행). */
data class CourseDetailRow(
    val id: Long,
    val userId: Long,
    val title: String,
    val coverImageUrl: String?,
    val description: String?,
    /** 코스 카테고리(응답 themes 의 출처). 미선택 draft 는 null. */
    val category: CourseCategory?,
    val tracingsCnt: Int,
    val status: CourseStatus,
    val visibility: CourseVisibility,
)

/** 코스에 담긴 장소 읽기 모델(장소별 이미지 포함, orderNo 오름차순). */
data class CoursePlaceRow(
    val id: Long,
    val placeId: Long,
    val orderNo: Int,
    val caption: String?,
    val walkingMinutes: Int?,
    val images: List<CoursePlaceImageRow>,
)

data class CoursePlaceImageRow(
    val imageUrl: String,
    val orderNo: Int,
)

/**
 * 아웃바운드 포트 — 코스 상세 조회에 필요한 읽기 모델 계약.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface CourseQueryPort {
    fun findCourseDetail(courseId: Long): CourseDetailRow?

    fun findPlaces(courseId: Long): List<CoursePlaceRow>
}
