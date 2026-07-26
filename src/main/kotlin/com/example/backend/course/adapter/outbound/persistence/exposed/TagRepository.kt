package com.example.backend.course.adapter.outbound.persistence.exposed

import com.example.backend.course.adapter.outbound.persistence.TagEntity
import com.example.backend.course.adapter.outbound.persistence.TagTable
import org.jetbrains.exposed.v1.core.eq
import org.springframework.stereotype.Repository

/**
 * tags 테이블 접근 리포지토리 — 태그 이름 기준 find-or-create(DAO).
 */
@Repository
class TagRepository {
    /** 태그 이름으로 tags 행을 찾고 없으면 생성해 id 를 반환한다. */
    fun findOrCreate(tagName: String): Long =
        (
            TagEntity.find { TagTable.name eq tagName }.firstOrNull()
                ?: TagEntity.new { name = tagName }
        ).id.value
}
