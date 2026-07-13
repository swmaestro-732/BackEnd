package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.dto.CourseDetailResult

interface CourseUseCase {
    fun getDetail(courseId: Long): CourseDetailResult
}
