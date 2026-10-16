package com.example.backend.course.application.service

import com.example.backend.course.domain.model.CourseCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CourseCategoryQueryServiceTest {
    @Test
    fun `도메인 카테고리를 선언 순서와 이름 그대로 반환한다`() {
        assertEquals(CourseCategory.entries.map { it.name }, CourseCategoryQueryService().listCategoryNames())
    }
}
