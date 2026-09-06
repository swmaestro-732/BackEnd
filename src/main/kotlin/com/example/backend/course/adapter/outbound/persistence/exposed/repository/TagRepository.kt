package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.exposed.TagTable
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.springframework.stereotype.Repository

/**
 * tags 테이블 접근 리포지토리 — 태그 이름 기준 find-or-create.
 */
@Repository
class TagRepository {
    fun findOrCreateAll(tagNames: List<String>): List<Long> {
        if (tagNames.isEmpty()) return emptyList()
        return TagTable
            .batchUpsert(
                tagNames,
                TagTable.name,
                onUpdate = { it[TagTable.name] = insertValue(TagTable.name) },
            ) { tagName ->
                this[TagTable.name] = tagName
            }.map { it[TagTable.id].value }
    }
}
