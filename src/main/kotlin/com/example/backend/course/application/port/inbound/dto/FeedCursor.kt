package com.example.backend.course.application.port.inbound.dto

import java.time.Instant

/**
 * 공개 코스 피드 키셋 커서.
 * 피드 정렬 키인 savesCnt DESC, createdAt DESC, id DESC의 마지막 항목을 가리킨다.
 */
data class FeedCursor(
    val savesCnt: Int,
    val createdAt: Instant,
    val id: Long,
)
