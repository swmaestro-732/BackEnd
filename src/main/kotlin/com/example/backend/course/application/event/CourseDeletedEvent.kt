package com.example.backend.course.application.event

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.port.inbound.AuthorCourseCountChanged

/**
 * 도메인 이벤트 — 코스 소프트 삭제를 알린다. 커밋 후(AFTER_COMMIT) 소비자가 각자 필요한 것만 쓴다:
 * - 검색 색인([CourseSearchSyncListener], 같은 course 도메인)은 [courseId] 로 문서 삭제.
 * - 작성자 코스 개수(user 도메인 ACL)는 [AuthorCourseCountChanged] 계약만 보고 old→null 전이로 버킷 델타(감소)를 계산한다.
 *
 * [oldVisibility] 는 삭제 직전 "카운트되던 상태(발행)"의 공개범위이고 카운트 대상이 아니었으면(임시저장) null.
 * 삭제 후 상태는 없으므로 [newVisibility] 는 항상 null.
 */
data class CourseDeletedEvent(
    val courseId: Long,
    override val authorId: Long,
    override val oldVisibility: CourseVisibility?,
) : AuthorCourseCountChanged {
    override val newVisibility: CourseVisibility? get() = null
}
