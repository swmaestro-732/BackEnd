package com.example.backend.course.application.event

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.port.inbound.AuthorCourseCountChanged
import com.example.backend.course.domain.model.Course

/**
 * 도메인 이벤트 — 코스 생성·편집을 한 번에 알린다(한 비즈니스 로직 = 이벤트 하나).
 *
 * 커밋 후(AFTER_COMMIT) 두 소비자가 각자 필요한 것만 쓴다:
 * - 검색 색인([CourseSearchSyncListener], 같은 course 도메인)은 [newCourse] 로 upsert.
 * - 작성자 코스 개수(user 도메인 ACL)는 [AuthorCourseCountChanged] **계약만** 보고 old→new 전이로 버킷 델타를 계산한다.
 *
 * user 어댑터가 course 내부 이벤트/[Course] 를 직접 만지지 않도록, 카운트에 필요한 값은 계약 필드로만 노출한다.
 * [oldVisibility]/[newVisibility] 는 "카운트되는 상태(발행)"의 공개범위이고 아니면 null([Course.countedVisibility]).
 * 작성자와 새 공개범위는 저장된 [newCourse] 에서 도출한다 — 호출자가 서로 다른 상태를 조합할 수 없다.
 */
data class CourseSavedEvent(
    val newCourse: Course,
    override val oldVisibility: CourseVisibility?,
) : AuthorCourseCountChanged {
    override val authorId: Long get() = newCourse.userId
    override val newVisibility: CourseVisibility? get() = newCourse.countedVisibility
}
