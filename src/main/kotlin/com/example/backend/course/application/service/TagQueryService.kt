package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.TagQueryUseCase
import com.example.backend.course.application.port.outbound.CourseTagQueryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 코스 태그 조회 유스케이스 — 태그 존재 검증(크로스 도메인). */
@Service
@Transactional(readOnly = true)
class TagQueryService(
    private val courseTagQueryPort: CourseTagQueryPort,
) : TagQueryUseCase {
    override fun findExistingTagIds(tagIds: List<Long>): Set<Long> = courseTagQueryPort.findExistingTagIds(tagIds)
}
