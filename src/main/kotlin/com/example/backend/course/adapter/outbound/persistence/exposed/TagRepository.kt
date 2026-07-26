package com.example.backend.course.adapter.outbound.persistence.exposed

import com.example.backend.course.adapter.outbound.persistence.TagTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository

/**
 * tags 테이블 접근 리포지토리 — 태그 이름 기준 find-or-create.
 */
@Repository
class TagRepository {
    /** 태그 이름으로 tags 행을 찾고 없으면 생성해 id 를 반환한다. */
    fun findOrCreate(tagName: String): Long =
        TagTable
            .select(TagTable.id)
            .where { TagTable.name eq tagName }
            .singleOrNull()
            ?.get(TagTable.id)
            ?.value
            ?: TagTable.insert { it[name] = tagName }[TagTable.id].value
}
