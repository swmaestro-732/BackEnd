package com.example.backend.user.adapter.outbound.course

import com.example.backend.course.application.port.inbound.CourseCounterUseCase
import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.user.application.port.outbound.CourseAccessPort
import org.springframework.stereotype.Component

/**
 * ACL(anti-corruption layer) 어댑터 — user 의 [CourseAccessPort] 를 course 도메인의 인바운드 포트로 위임한다.
 * course 를 아는 유일한 지점이며, MSA 분리 시 이 클래스만 REST/이벤트 클라이언트로 교체하면 된다.
 */
@Component
class CourseAccessAdapter(
    private val courseQueryUseCase: CourseQueryUseCase,
    private val courseCounterUseCase: CourseCounterUseCase,
) : CourseAccessPort {
    override fun existsCourse(courseId: Long): Boolean = courseQueryUseCase.existsById(courseId)

    override fun increaseSavesCount(courseId: Long): Int = courseCounterUseCase.increaseSavesCount(courseId)

    override fun decreaseSavesCount(courseId: Long) = courseCounterUseCase.decreaseSavesCount(courseId)
}
