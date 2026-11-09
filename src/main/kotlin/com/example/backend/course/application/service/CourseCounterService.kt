package com.example.backend.course.application.service

import com.example.backend.course.application.event.CourseCounter
import com.example.backend.course.application.event.CourseCounterChangedEvent
import com.example.backend.course.application.port.inbound.CourseCounterUseCase
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 크로스 도메인 코스 카운터 서비스 — [CourseCounterUseCase] 구현.
 * @Transactional(REQUIRED)라 호출자(user 저장 트랜잭션)에 참여한다 — 저장과 카운터 증가가 한 트랜잭션으로 묶인다.
 * 실제로 갱신된 행이 있을 때만 [CourseCounterChangedEvent] 를 발행해 커밋 후 검색 색인의 저장수를 맞춘다.
 */
@Service
class CourseCounterService(
    private val coursePersistencePort: CoursePersistencePort,
    private val eventPublisher: ApplicationEventPublisher,
) : CourseCounterUseCase {
    @Transactional
    override fun increaseSavesCount(courseId: Long): Int {
        val updated = coursePersistencePort.increaseSavesCount(courseId)
        if (updated >
            0
        ) {
            eventPublisher.publishEvent(CourseCounterChangedEvent(courseId, CourseCounter.SAVES, delta = 1))
        }
        return updated
    }

    @Transactional
    override fun decreaseSavesCount(courseId: Long) {
        val updated = coursePersistencePort.decreaseSavesCount(courseId)
        if (updated >
            0
        ) {
            eventPublisher.publishEvent(CourseCounterChangedEvent(courseId, CourseCounter.SAVES, delta = -1))
        }
    }
}
