package com.example.backend.course.application.event

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.port.inbound.AuthorCourseCountChanged
import com.example.backend.course.domain.model.Course
import java.util.UUID

/**
 * 도메인 이벤트 — 코스 생성·편집을 한 번에 알린다(한 비즈니스 로직 = 이벤트 하나).
 *
 * 커밋 후(AFTER_COMMIT) 두 소비자가 각자 필요한 것만 쓴다:
 * - 검색 색인([CourseSearchSyncListener], 같은 course 도메인)은 [courseId] 로 코스를 다시 읽어 upsert.
 * - 작성자 코스 개수(user 도메인 ACL)는 [AuthorCourseCountChanged] **계약만** 보고 old→new 전이로 버킷 델타를 계산한다.
 *
 * 이벤트 저장소(event_publication)에 JSON 으로 남으므로 id 와 값만 싣는다(애그리거트를 싣지 않는다).
 * [oldVisibility]/[newVisibility] 는 "카운트되는 상태(발행)"의 공개범위이고 아니면 null([Course.countedVisibility]).
 * 작성자와 새 공개범위는 [from] 이 저장된 Course 에서 도출한다. 호출자가 서로 다른 상태를 조합하지 않게 한다.
 */
data class CourseSavedEvent(
    val courseId: Long,
    override val authorId: Long,
    override val oldVisibility: CourseVisibility?,
    override val newVisibility: CourseVisibility?,
    override val eventId: String = UUID.randomUUID().toString(),
) : AuthorCourseCountChanged {
    companion object {
        fun from(
            savedCourse: Course,
            oldVisibility: CourseVisibility?,
        ) = CourseSavedEvent(
            courseId = savedCourse.id!!,
            authorId = savedCourse.userId,
            oldVisibility = oldVisibility,
            newVisibility = savedCourse.countedVisibility,
        )
    }
}
