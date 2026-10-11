package com.example.backend.course.application.port.outbound

import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand
import java.time.Instant

/** 아웃바운드 포트 — 코스 따라가기 기록(tracing_courses·tracing_course_places) 접근. 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다. */
interface CourseTracePersistencePort {
    /** 따라가기 기록과 방문 장소를 삽입한다. */
    fun insert(command: EndCourseTraceCommand)

    /** 주어진 코스들 중 사용자가 따라간 코스의 완주 시각(가장 이른 기록)을 courseId 별로 반환한다. 따라가지 않은 코스는 없다. */
    fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant>

    /** 사용자가 한 번이라도 따라간 코스 id 집합. */
    fun findTracedCourseIds(userId: Long): Set<Long>
}
