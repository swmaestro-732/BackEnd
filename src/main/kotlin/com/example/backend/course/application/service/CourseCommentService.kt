package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.port.inbound.CourseCommentUseCase
import com.example.backend.course.application.port.inbound.dto.CourseCommentResult
import com.example.backend.course.application.port.inbound.dto.CreateCourseCommentCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommentCommand
import com.example.backend.course.application.port.outbound.CourseCommentPersistencePort
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CourseCommentService(
    private val coursePersistencePort: CoursePersistencePort,
    private val commentPersistencePort: CourseCommentPersistencePort,
) : CourseCommentUseCase {
    override fun create(command: CreateCourseCommentCommand): CourseCommentResult {
        if (!coursePersistencePort.existsById(command.courseId)) {
            throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND)
        }
        val saved = commentPersistencePort.save(command.courseId, command.userId, command.content)
        // 존재 확인 뒤 코스가 삭제되었다면 댓글 insert도 같은 트랜잭션에서 롤백한다.
        if (coursePersistencePort.increaseCommentsCount(command.courseId) == 0) {
            throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND)
        }
        return CourseCommentResult(
            id = saved.id,
            authorId = saved.authorId,
            content = saved.content,
            createdAt = saved.createdAt,
            updatedAt = saved.updatedAt,
            isMine = command.userId == saved.authorId,
        )
    }

    override fun edit(command: EditCourseCommentCommand) {
        val affected =
            commentPersistencePort.updateContent(command.courseId, command.commentId, command.userId, command.content)
        if (affected == 0) {
            throw BusinessException(CourseErrorCode.COURSE_COMMENT_NOT_FOUND)
        }
    }

    override fun delete(
        userId: Long,
        courseId: Long,
        commentId: Long,
    ) {
        if (commentPersistencePort.softDelete(courseId, commentId, userId) == 0) {
            throw BusinessException(CourseErrorCode.COURSE_COMMENT_NOT_FOUND)
        }
        coursePersistencePort.decreaseCommentsCount(courseId)
    }
}
