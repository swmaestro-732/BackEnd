package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.dto.FeedCursor
import com.example.backend.mobile.home.application.port.outbound.HomeFeedPort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCourse
import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCoursePage
import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCursor
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — 공개 코스 피드 후보 조회를 course 도메인 인바운드 포트에 위임하고 BFF 격리 DTO 로 매핑한다.
 * (MSA 분리 시 이 어댑터만 course 서비스 HTTP 클라이언트로 교체한다.)
 */
@Component
class HomeFeedAdapter(
    private val courseQueryUseCase: CourseQueryUseCase,
) : HomeFeedPort {
    override fun listPublicCandidates(
        cursor: HomeFeedCursor?,
        size: Int,
    ): HomeFeedCoursePage {
        val page =
            courseQueryUseCase.listPublic(
                cursor =
                    cursor?.let {
                        FeedCursor(
                            savesCnt = it.savesCnt,
                            createdAt = it.createdAt,
                            id = it.id,
                        )
                    },
                size = size,
            )
        return HomeFeedCoursePage(
            courses =
                page.items.map {
                    HomeFeedCourse(
                        id = it.id,
                        authorId = it.authorId,
                        title = it.title,
                        coverImageUrl = it.coverImageUrl,
                        theme = it.theme,
                        likesCnt = it.likesCnt,
                        savesCnt = it.savesCnt,
                        createdAt = it.createdAt,
                    )
                },
            hasNext = page.hasNext,
        )
    }
}
