package com.example.backend.course.adapter.outbound.user

import com.example.backend.course.application.port.inbound.AuthorCourseCountChanged
import com.example.backend.user.application.port.inbound.CourseCountIntakeUseCase
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

/**
 * course 의 공개범위 전이 이벤트([AuthorCourseCountChanged])를 커밋 후 user 에 알린다.
 * 예전에는 user 가 course 이벤트를 직접 구독해 user -> course 의존이 생겼다(course, user 순환).
 * 지금은 course 가 user 의 inbound 포트를 호출하는 한 방향(course -> user)만 남는다.
 *
 * 이벤트 저장소(Modulith)가 발행과 같은 트랜잭션에 이 리스너 몫의 기록을 남기고, 커밋 후 비동기로 부른다.
 * 동기 반영과 SQS 폴백이 모두 실패하면 예외가 올라와 기록이 미완료로 남고 같은 eventId 로 재발행된다.
 */
@Component
class CourseCountNotifier(
    private val courseCountIntakeUseCase: CourseCountIntakeUseCase,
) {
    @ApplicationModuleListener
    fun onCourseCountChanged(event: AuthorCourseCountChanged) {
        courseCountIntakeUseCase.receive(event.eventId, event.authorId, event.oldVisibility, event.newVisibility)
    }
}
