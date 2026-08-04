package com.example.backend.mobile.course.adapter.outbound

import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.mobile.course.application.port.outbound.FeedCoursePort
import com.example.backend.mobile.course.application.port.outbound.dto.FeedCourse
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — 공개 코스 피드 후보 조회를 course 도메인 인바운드 포트에 위임하고 BFF 격리 DTO 로 매핑한다.
 * (MSA 분리 시 이 어댑터만 course 서비스 HTTP 클라이언트로 교체한다.)
 */
@Component
class FeedCourseAdapter(
    private val courseQueryUseCase: CourseQueryUseCase,
) : FeedCoursePort {
    override fun listPublicCandidates(limit: Int): List<FeedCourse> =
        courseQueryUseCase.listPublic(limit).map {
            FeedCourse(
                id = it.id,
                authorId = it.authorId,
                title = it.title,
                coverImageUrl = it.coverImageUrl,
                theme = it.theme,
                likesCnt = it.likesCnt,
                savesCnt = it.savesCnt,
                createdAt = it.createdAt,
            )
        }
}
