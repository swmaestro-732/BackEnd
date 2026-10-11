package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand
import java.time.Instant

/**
 * 인바운드 포트 — 코스 따라가기 종료·완주 조회.
 * 따라가기 기록(tracing_courses)은 course 도메인 소유 데이터다. 다른 도메인(user 저장함·mobile 화면)이 완주 여부를 필요로 하면 이 포트로만 접근한다.
 */
interface CourseTraceUseCase {
    /** 따라가기 종료. 볼 수 없는(비활성·비공개·미팔로우) 코스는 404. 같은 코스를 여러 번 따라가도 매번 기록한다. */
    fun end(command: EndCourseTraceCommand)

    /** 주어진 코스들 중 사용자가 따라간(=완주한) 코스의 완주 시각(가장 이른 기록)을 courseId 별로 반환한다. 따라가지 않은 코스는 결과에 없다. */
    fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant>

    /** 사용자가 한 번이라도 따라간 코스 id 집합 — 저장함 완주 필터·완주 수에 쓴다. */
    fun findTracedCourseIds(userId: Long): Set<Long>
}
