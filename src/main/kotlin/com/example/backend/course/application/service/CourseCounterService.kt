package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseCounterUseCase
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 크로스 도메인 코스 카운터 서비스 — [CourseCounterUseCase] 구현([CourseQueryService] 선례).
 * @Transactional(REQUIRED)라 호출자(user 저장 트랜잭션)에 참여한다 — 저장과 카운터 증가가 한 트랜잭션으로 묶인다.
 */
@Service
class CourseCounterService(
    private val coursePersistencePort: CoursePersistencePort,
) : CourseCounterUseCase {
    @Transactional
    override fun increaseSavesCount(courseId: Long) {
        coursePersistencePort.increaseSavesCount(courseId)
    }

    @Transactional
    override fun decreaseSavesCount(courseId: Long) {
        coursePersistencePort.decreaseSavesCount(courseId)
    }
}
