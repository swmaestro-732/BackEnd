package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.TagTable
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import org.springframework.stereotype.Repository

/**
 * tags 테이블 접근 리포지토리 — 태그 이름 기준 find-or-create(DAO).
 */
@Repository
class TagRepository {
    /**
     * 태그 이름으로 tags 행을 찾고 없으면 생성해 id 를 반환한다.
     *
     * find 후 insert 하는 2단계는 동시 요청에서 경쟁 조건(둘 다 없다고 보고 각각 insert)이 생겨
     * tags.name UNIQUE 제약에 걸린 쪽이 중복 키 오류를 그대로 전파한다.
     * DB 원자적 upsert(`ON CONFLICT (name) DO UPDATE ... RETURNING id`)로 처리해 항상 기존/신규 행의 id 를 돌려준다.
     * (충돌 시 RETURNING 이 행을 돌려주도록 name 을 그대로 다시 쓰는 no-op 업데이트를 건다.)
     */
    fun findOrCreate(tagName: String): Long =
        TagTable
            .upsertReturning(
                TagTable.name,
                returning = listOf(TagTable.id),
                onUpdate = { it[TagTable.name] = tagName },
            ) {
                it[name] = tagName
            }.single()[TagTable.id]
            .value
}
