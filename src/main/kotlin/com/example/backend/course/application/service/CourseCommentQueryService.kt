package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseCommentQueryUseCase
import com.example.backend.course.application.port.inbound.dto.CourseCommentPage
import com.example.backend.course.application.port.inbound.dto.CourseCommentResult
import com.example.backend.course.application.port.outbound.CourseCommentPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CourseCommentQueryService(
    private val commentPersistencePort: CourseCommentPersistencePort,
) : CourseCommentQueryUseCase {
    override fun list(
        courseId: Long,
        viewerId: Long?,
        cursor: Long?,
        size: Int,
    ): CourseCommentPage {
        val effectiveSize = size.coerceIn(1, 100)
        val rows = commentPersistencePort.findPage(courseId, cursor, effectiveSize)
        return CourseCommentPage(
            items =
                rows.take(effectiveSize).map {
                    CourseCommentResult(
                        id = it.id,
                        authorId = it.authorId,
                        content = it.content,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        isMine = viewerId == it.authorId,
                    )
                },
            hasNext = rows.size > effectiveSize,
        )
    }
}
