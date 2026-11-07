package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.course.application.port.inbound.CourseTraceUseCase
import com.example.backend.mobile.home.application.port.outbound.HomeTracePort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeTracingCourse
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — 진행 중 따라가기 상세를 [CourseTraceUseCase] 로 조회해 BFF 격리 DTO 로 매핑한다.
 * 코스를 더 볼 수 없어도 course 도메인이 isDeleted 로 알려주므로 카드는 그대로 내려준다.
 * (MSA 분리 시 이 어댑터만 course 서비스 HTTP 클라이언트로 교체한다.)
 */
@Component
class HomeTraceAdapter(
    private val courseTraceUseCase: CourseTraceUseCase,
) : HomeTracePort {
    override fun findInProgress(userId: Long): HomeTracingCourse? {
        val trace = courseTraceUseCase.findInProgress(userId) ?: return null
        return HomeTracingCourse(
            traceId = trace.traceId,
            courseId = trace.course.courseId,
            title = trace.course.title,
            coverImageUrl = trace.course.coverImageUrl,
            area = trace.course.area,
            placeCount = trace.places.size,
            visitedPlaceCount = trace.visitedPlaces.size,
            startedAt = trace.startedAt,
            isDeleted = trace.course.isDeleted,
        )
    }
}
