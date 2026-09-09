package com.example.backend.course.application.port.inbound

import com.example.backend.common.domain.CourseVisibility

/**
 * 인바운드 포트 계약 — 작성자의 "카운트되는 공개범위" 전이(old→new)를 알리는 **공개 이벤트 계약**.
 *
 * course 도메인의 저장·삭제 이벤트([CourseSavedEvent]/[CourseDeletedEvent])가 이 계약을 구현하고,
 * 다른 도메인(user)의 ACL 은 course 내부 이벤트가 아니라 **이 계약만** 참조한다
 * (크로스도메인은 상대 도메인 `application.port.inbound` 만 — architecture.md 규칙 7).
 * old==new 면 카운트 변화 없음. 어느 공개범위가 어느 버킷으로 가는지·델타 계산은 카운트를 소유한 소비자(user) 몫이다.
 */
interface AuthorCourseCountChanged {
    val authorId: Long
    val oldVisibility: CourseVisibility?
    val newVisibility: CourseVisibility?
}
