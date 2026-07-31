package com.example.backend.user.domain.model

import java.time.Instant

/**
 * 저장 코스 — 사용자가 저장한 코스 레코드(saved_courses).
 * courseId 는 cross-domain(course) 참조라 FK 없이 id 만 들고 있고, folderId 는 폴더 미분류면 null 이다.
 */
data class SavedCourse(
    val id: Long,
    val userId: Long,
    val courseId: Long,
    val folderId: Long?,
    val savedAt: Instant,
)
