package com.example.backend.course.adapter.outbound.user

import com.example.backend.course.application.port.inbound.AuthorCourseCountChanged
import com.example.backend.user.application.port.inbound.CourseCountIntakeUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * course 의 공개범위 전이 이벤트([AuthorCourseCountChanged])를 커밋 후 user 에 알린다.
 * 예전에는 user 가 course 이벤트를 직접 구독해 user -> course 의존이 생겼다(course, user 순환).
 * 지금은 course 가 user 의 inbound 포트를 호출하는 한 방향(course -> user)만 남는다.
 */
@Component
class CourseCountNotifier(
    private val courseCountIntakeUseCase: CourseCountIntakeUseCase,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCourseCountChanged(event: AuthorCourseCountChanged) {
        courseCountIntakeUseCase.receive(event.authorId, event.oldVisibility, event.newVisibility)
    }
}
