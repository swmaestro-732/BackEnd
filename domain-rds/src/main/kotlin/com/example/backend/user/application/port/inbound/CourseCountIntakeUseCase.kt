package com.example.backend.user.application.port.inbound

import com.example.backend.common.domain.CourseVisibility

/**
 * 작성자의 "카운트되는 공개범위" 전이(old -> new)를 받아 코스 개수에 반영한다. course 가 커밋 후 호출한다.
 * 동기 반영을 먼저 시도하고, 실패하면 SQS 폴백에 실어 같은 eventId 로 재시도한다(멱등은 [CourseCountUseCase] 가 지킨다).
 * 폴백 발송마저 실패하면 예외를 던진다. 호출한 리스너의 이벤트 저장소 기록이 미완료로 남아 같은 eventId 로 재발행된다.
 */
interface CourseCountIntakeUseCase {
    fun receive(
        eventId: String,
        authorId: Long,
        oldVisibility: CourseVisibility?,
        newVisibility: CourseVisibility?,
    )
}
