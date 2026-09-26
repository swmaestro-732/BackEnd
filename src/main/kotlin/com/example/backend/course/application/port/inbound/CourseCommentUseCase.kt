package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CourseCommentResult
import com.example.backend.course.application.port.inbound.dto.CreateCourseCommentCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommentCommand

interface CourseCommentUseCase {
    fun create(command: CreateCourseCommentCommand): CourseCommentResult

    fun edit(command: EditCourseCommentCommand)

    fun delete(
        userId: Long,
        courseId: Long,
        commentId: Long,
    )
}
