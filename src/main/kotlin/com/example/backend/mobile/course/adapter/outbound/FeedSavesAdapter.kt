package com.example.backend.mobile.course.adapter.outbound

import com.example.backend.mobile.course.application.port.outbound.FeedSavesPort
import com.example.backend.user.application.port.inbound.CourseInteractionUseCase
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — 코스별 저장수 조회를 user 도메인 인바운드 포트에 위임한다.
 * (MSA 분리 시 이 어댑터만 user 서비스 HTTP 클라이언트로 교체한다.)
 */
@Component
class FeedSavesAdapter(
    private val courseInteractionUseCase: CourseInteractionUseCase,
) : FeedSavesPort {
    override fun countSaves(courseIds: List<Long>): Map<Long, Int> =
        courseInteractionUseCase.countSavesByCourseIds(courseIds)
}
