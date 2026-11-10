package com.example.backend.course.application.port.outbound

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.port.inbound.dto.AdminCourseStatus
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CourseStatus
import java.time.Instant

/** 어드민 코스 읽기 모델. 삭제된 코스도 담는다. */
data class AdminCourseRow(
    val id: Long,
    val userId: Long,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val categories: List<CourseCategory>,
    val area: String?,
    val areaCode: String?,
    val status: CourseStatus,
    val isPublished: Boolean,
    val visibility: CourseVisibility,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * 어드민 목록 조건. [status] 가 없으면 삭제되지 않은 코스 전체.
 * [keyword] 는 제목 부분 일치, 작성자 id 목록([keywordAuthorIds]), 코스 id([keywordCourseId]) 중 하나라도 맞으면 통과.
 */
data class AdminCourseFilter(
    val status: AdminCourseStatus?,
    val authorId: Long?,
    val excludeAuthorId: Long?,
    val areaCodePrefix: String?,
    val category: CourseCategory?,
    val keyword: String?,
    val keywordAuthorIds: List<Long>,
    val keywordCourseId: Long?,
)

/** 아웃바운드 포트: 어드민 전용 코스 조회와 상태 전이. 사용자 경로와 달리 숨김, 삭제 코스도 다룬다. */
interface AdminCoursePersistencePort {
    /** id 내림차순 페이지. */
    fun findPage(
        filter: AdminCourseFilter,
        offset: Long,
        limit: Int,
    ): List<AdminCourseRow>

    fun count(filter: AdminCourseFilter): Long

    /** 상태 무관 단건. [lock] 이면 행을 잠가(FOR UPDATE) 같은 코스에 대한 동시 조치를 직렬화한다. */
    fun findById(
        courseId: Long,
        lock: Boolean = false,
    ): AdminCourseRow?

    /** 미삭제 코스의 상태를 [from] 에서 [to] 로 바꾸고 색인용 코스(태그, 카테고리 포함)를 돌려준다. 상태가 다르면 null. */
    fun changeStatus(
        courseId: Long,
        from: CourseStatus,
        to: CourseStatus,
    ): Course?

    /** 삭제된 코스를 숨김(HIDDEN)으로 되돌리고(deleted_at 비움) 색인용 코스를 돌려준다. 삭제 상태가 아니면 null. */
    fun restore(courseId: Long): Course?
}
