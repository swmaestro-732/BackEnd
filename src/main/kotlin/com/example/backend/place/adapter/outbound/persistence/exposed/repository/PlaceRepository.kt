package com.example.backend.place.adapter.outbound.persistence.exposed.repository

import com.example.backend.place.adapter.outbound.persistence.PlaceEntity
import com.example.backend.place.adapter.outbound.persistence.PlaceTable
import com.example.backend.place.domain.model.Place
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.springframework.stereotype.Repository

/**
 * places 테이블 접근 리포지토리 — 삭제되지 않은 장소의 조회를 담당한다.
 * 조회는 DAO([PlaceEntity])로 하고, 읽기 모델은 순수 도메인 [Place] 로 변환해 반환한다.
 */
@Repository
class PlaceRepository {
    /** deleted_at IS NULL 인 장소들을 id 목록으로 읽어 도메인으로 변환한다. */
    fun findByIds(placeIds: List<Long>): List<Place> =
        PlaceEntity
            .find { (PlaceTable.id inList placeIds) and PlaceTable.deletedAt.isNull() }
            .map { it.toDomain() }

    /**
     * 이름 부분 일치(`name LIKE '%query%'`) + deleted_at IS NULL 로 장소를 id 오름차순 [limit] 개 조회한다.
     * [afterId] 가 주어지면 그보다 id 가 큰 장소부터(커서 seek). 삭제된 장소는 제외.
     * 사용자 입력의 LIKE 와일드카드(`%`·`_`·`\`)는 이스케이프해 리터럴로 취급한다(의도치 않은 전체 매칭 방지).
     */
    fun searchByName(
        query: String,
        cursor: Long?,
        limit: Int,
    ): List<Place> =
        PlaceEntity
            .find { nameMatches(query) and (cursor?.let { PlaceTable.id greater it } ?: Op.TRUE) }
            .orderBy(PlaceTable.id to SortOrder.ASC)
            .limit(limit)
            .map { it.toDomain() }

    /** 이름 부분 일치 + deleted_at IS NULL 인 장소의 전체 개수. */
    fun countByName(query: String): Long = PlaceEntity.count(nameMatches(query))

    private fun nameMatches(query: String) =
        (PlaceTable.name like "%${query.escapeLikeWildcards()}%") and PlaceTable.deletedAt.isNull()

    // Postgres LIKE 의 기본 이스케이프 문자(백슬래시)를 이용해 와일드카드를 리터럴화한다. 백슬래시를 먼저 치환해야 한다.
    private fun String.escapeLikeWildcards(): String = replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
