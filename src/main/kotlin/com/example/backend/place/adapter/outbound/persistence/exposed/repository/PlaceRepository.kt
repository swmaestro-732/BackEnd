package com.example.backend.place.adapter.outbound.persistence.exposed.repository

import com.example.backend.place.adapter.outbound.persistence.PlaceEntity
import com.example.backend.place.adapter.outbound.persistence.PlaceTable
import com.example.backend.place.domain.model.Place
import org.jetbrains.exposed.v1.core.and
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
     * 이름 부분 일치(`name LIKE '%query%'`) + deleted_at IS NULL 로 장소를 조회한다.
     * 사용자 입력의 LIKE 와일드카드(`%`·`_`·`\`)는 이스케이프해 리터럴로 취급한다(의도치 않은 전체 매칭 방지).
     */
    fun searchByName(query: String): List<Place> =
        PlaceEntity
            .find { (PlaceTable.name like "%${query.escapeLikeWildcards()}%") and PlaceTable.deletedAt.isNull() }
            .map { it.toDomain() }

    // Postgres LIKE 의 기본 이스케이프 문자(백슬래시)를 이용해 와일드카드를 리터럴화한다. 백슬래시를 먼저 치환해야 한다.
    private fun String.escapeLikeWildcards(): String = replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
