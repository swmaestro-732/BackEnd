package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.CourseTraceUseCase
import com.example.backend.course.application.port.inbound.dto.CourseTracesQuery
import com.example.backend.course.domain.model.CourseTraceStatus
import com.example.backend.mobile.home.application.port.outbound.HomeTracePort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeTracingCourse
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — 진행 중 따라가기는 [CourseTraceUseCase], 코스 요약은 [CourseQueryUseCase] 로 조회해 BFF 격리 DTO 로 매핑한다.
 * (MSA 분리 시 이 어댑터만 course 서비스 HTTP 클라이언트로 교체한다.)
 */
@Component
class HomeTraceAdapter(
    private val courseTraceUseCase: CourseTraceUseCase,
    private val courseQueryUseCase: CourseQueryUseCase,
) : HomeTracePort {
    override fun findLatestInProgress(userId: Long): HomeTracingCourse? {
        val trace =
            courseTraceUseCase
                .list(
                    CourseTracesQuery(userId = userId, status = CourseTraceStatus.IN_PROGRESS, cursor = null, size = 1),
                ).traces
                .firstOrNull() ?: return null
        val course = courseQueryUseCase.getDetails(listOf(trace.courseId), userId).firstOrNull() ?: return null
        return HomeTracingCourse(
            traceId = trace.id,
            courseId = course.id,
            title = course.title,
            coverImageUrl = course.coverImageUrl,
            area = course.area,
            placeCount = course.places.size,
            startedAt = trace.startedAt,
        )
    }
}
