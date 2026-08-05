package com.example.backend.mobile.user.adapter.outbound

import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.dto.AuthorCourseCursor
import com.example.backend.mobile.user.application.MyPageCursorCodec
import com.example.backend.mobile.user.application.port.outbound.MyPageCoursePort
import com.example.backend.mobile.user.application.port.outbound.dto.AuthoredCourse
import com.example.backend.mobile.user.application.port.outbound.dto.AuthoredCoursePage
import com.example.backend.mobile.user.application.port.outbound.dto.CourseCounts
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — 작성자 발행 코스 조회를 course 도메인 인바운드 포트에 위임하고 BFF 격리 DTO 로 매핑한다.
 * (MSA 분리 시 이 어댑터만 course 서비스 HTTP 클라이언트로 교체한다.)
 */
@Component
class MyPageCourseAdapter(
    private val courseQueryUseCase: CourseQueryUseCase,
) : MyPageCoursePort {
    override fun listByAuthor(
        authorId: Long,
        viewerId: Long?,
        cursor: String?,
        size: Int,
    ): AuthoredCoursePage {
        val page =
            courseQueryUseCase.listByAuthor(
                authorId = authorId,
                viewerId = viewerId,
                cursor =
                    MyPageCursorCodec.decode(cursor)?.let {
                        AuthorCourseCursor(createdAt = it.createdAt, id = it.id)
                    },
                size = size,
            )
        return AuthoredCoursePage(
            courses =
                page.items.map {
                    AuthoredCourse(
                        id = it.id,
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

    override fun countByVisibility(authorId: Long): CourseCounts =
        courseQueryUseCase.countByAuthorGroupedByVisibility(authorId).let {
            CourseCounts(
                publicCount = it.publicCount,
                followerCount = it.followerCount,
                privateCount = it.privateCount,
            )
        }
}
