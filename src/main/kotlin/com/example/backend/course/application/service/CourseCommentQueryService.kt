package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.port.inbound.CourseCommentQueryUseCase
import com.example.backend.course.application.port.inbound.dto.CourseCommentPage
import com.example.backend.course.application.port.inbound.dto.CourseCommentRef
import com.example.backend.course.application.port.inbound.dto.CourseCommentResult
import com.example.backend.course.application.port.outbound.CommentAuthorPort
import com.example.backend.course.application.port.outbound.CourseCommentPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CourseCommentQueryService(
    private val commentPersistencePort: CourseCommentPersistencePort,
    private val commentAuthorPort: CommentAuthorPort,
) : CourseCommentQueryUseCase {
    override fun list(
        courseId: Long,
        viewerId: Long?,
        cursor: Long?,
        size: Int,
    ): CourseCommentPage {
        val effectiveSize = size.coerceIn(1, 100)
        val rows = commentPersistencePort.findPage(courseId, cursor, effectiveSize)
        val pageRows = rows.take(effectiveSize)
        val authors = commentAuthorPort.findAuthors(pageRows.map { it.authorId }.distinct())
        return CourseCommentPage(
            items =
                pageRows.map {
                    CourseCommentResult(
                        id = it.id,
                        authorId = it.authorId,
                        authorNickname = authors[it.authorId]?.nickname ?: UNKNOWN_AUTHOR_NICKNAME,
                        authorProfileImageUrl = authors[it.authorId]?.profileImageUrl,
                        content = it.content,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        isMine = viewerId == it.authorId,
                    )
                },
            hasNext = rows.size > effectiveSize,
        )
    }

    override fun getRef(commentId: Long): CourseCommentRef =
        commentPersistencePort.findRef(commentId)
            ?: throw BusinessException(CourseErrorCode.COURSE_COMMENT_NOT_FOUND)

    private companion object {
        /** 탈퇴했거나 없는 작성자의 대체 닉네임 — 후기 목록과 같은 값이다. */
        const val UNKNOWN_AUTHOR_NICKNAME = "알 수 없음"
    }
}
