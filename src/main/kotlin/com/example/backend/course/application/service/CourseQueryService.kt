package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 크로스 도메인 코스 조회 서비스 — [CourseQueryUseCase] 구현.
 * 다른 도메인이 필요로 하는 최소한의 코스 조회(존재 확인)만 노출한다([PlaceQueryService] 선례).
 */
@Service
@Transactional(readOnly = true)
class CourseQueryService(
    private val coursePersistencePort: CoursePersistencePort,
) : CourseQueryUseCase {
    override fun existsById(courseId: Long): Boolean = coursePersistencePort.existsById(courseId)
}
