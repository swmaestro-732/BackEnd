package com.example.backend.course.application.port.outbound

import java.time.Instant

/**
 * 공개 코스 키셋 커서([CoursePersistencePort.findPublishedPublic]) — 정렬 키 savesCnt DESC, createdAt DESC, id DESC 의 마지막 항목.
 */
data class FeedCursor(
    val savesCnt: Int,
    val createdAt: Instant,
    val id: Long,
)
