package com.example.backend.course.application.port.outbound

import com.example.backend.course.domain.model.Course
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
    /** 코스 카테고리(응답 theme 의 출처). 미선택 draft 는 null. */
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
 * 아웃바운드 포트 — 코스 애그리거트 영속(조회·생성). 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 * user 도메인 [com.example.backend.user.application.port.outbound.UserPersistencePort] 와 동일하게
 * 한 포트에서 읽기·쓰기를 함께 다루고, 저장은 도메인 애그리거트([Course])를 받는다.
 */
interface CoursePersistencePort {
    fun findCourseDetail(courseId: Long): CourseDetailRow?

    /** 미삭제(deleted_at IS NULL) 코스가 존재하는지 확인한다(fork 원본 검증 등). */
    fun existsById(courseId: Long): Boolean

    fun findPlaces(courseId: Long): List<CoursePlaceRow>

    /** 코스 애그리거트(코스·장소·이미지·태그)를 한 트랜잭션으로 저장하고, 저장된 코스(생성 id·DB 생성값 포함)를 반환한다. */
    fun save(course: Course): Course

    /**
     * 이미 영속화된 코스([Course.id] 필수)를 전체 치환한다 — 코스 본문을 갱신하고
     * 장소·이미지·태그 연결을 지운 뒤 요청 값으로 다시 심고, 갱신된 코스(DB 생성값 포함)를 반환한다(한 트랜잭션).
     */
    fun update(course: Course): Course
}
