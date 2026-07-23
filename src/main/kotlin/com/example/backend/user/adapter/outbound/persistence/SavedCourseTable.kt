package com.example.backend.user.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object SavedCourseTable : Table("saved_courses") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val folderId = long("folder_id").nullable() // NULL = 폴더 미분류
    val courseId = long("course_id") // cross-domain(course): FK 없음
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
