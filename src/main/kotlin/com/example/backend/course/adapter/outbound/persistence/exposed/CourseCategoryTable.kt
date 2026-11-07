package com.example.backend.course.adapter.outbound.persistence.exposed

import com.example.backend.course.domain.model.CourseCategory
import org.jetbrains.exposed.v1.core.Table

internal object CourseCategoryTable : Table("course_categories") {
    val courseId = long("course_id")
    val category = enumerationByName<CourseCategory>("category", 50)
    override val primaryKey = PrimaryKey(courseId, category)
}
