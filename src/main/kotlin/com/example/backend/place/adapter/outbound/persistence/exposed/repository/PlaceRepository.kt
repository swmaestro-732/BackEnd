package com.example.backend.place.adapter.outbound.persistence.exposed.repository

import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceEntity
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceTable
import com.example.backend.place.domain.model.Place
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.springframework.stereotype.Repository

/**
 * places 테이블 접근 리포지토리 — 삭제되지 않은 장소의 조회를 담당한다.
 * 조회는 DAO([PlaceEntity])로 하고, 엔티티의 도메인 변환은 어댑터가 담당한다.
 */
@Repository
class PlaceRepository {
    /** deleted_at IS NULL 인 장소 한 건을 id 로 읽어 엔티티로 반환한다. 없거나 삭제됐으면 null. */
    internal fun findById(placeId: Long): PlaceEntity? =
        PlaceEntity
            .find { (PlaceTable.id eq placeId) and PlaceTable.deletedAt.isNull() }
            .firstOrNull()

    /** deleted_at IS NULL 인 장소들을 id 목록으로 읽어 엔티티로 반환한다. */
    internal fun findByIds(placeIds: List<Long>): List<PlaceEntity> =
        PlaceEntity
            .find { (PlaceTable.id inList placeIds) and PlaceTable.deletedAt.isNull() }
            .toList()

    /** 재색인용 — 활성(deleted_at IS NULL) 장소를 id 오름차순으로 afterId 다음부터 limit개 읽어 엔티티로 반환한다. */
    internal fun findForIndex(
        afterId: Long?,
        limit: Int,
    ): List<PlaceEntity> =
        PlaceEntity
            .find { (PlaceTable.id greater (afterId ?: 0L)) and PlaceTable.deletedAt.isNull() }
            .orderBy(PlaceTable.id to SortOrder.ASC)
            .limit(limit)
            .toList()

    /** deleted_at IS NULL 인 장소들을 카카오 place id 목록으로 읽어 엔티티로 반환한다(검색 결과 dedup 조회용). */
    internal fun findByKakaoIds(kakaoIds: List<String>): List<PlaceEntity> {
        val ids = kakaoIds.filter { it.isNotBlank() }
        if (ids.isEmpty()) return emptyList()
        return PlaceEntity
            .find { (PlaceTable.kakaoPlaceId inList ids) and PlaceTable.deletedAt.isNull() }
            .toList()
    }

    /**
     * 신규 장소들을 삽입한다. targetless insert ignore(ON CONFLICT DO NOTHING) — kakao_place_id 유니크 인덱스를 걸면
     * 그때부터 동시 검색 경합의 중복 삽입을 무시하는 최종 방어선이 된다. 인덱스 전까지는 no-op 이고,
     * dedup 은 앱 레벨 findByKakaoIds 조회로만 보장한다(동시 경합 시 중복 삽입 가능 — 인덱스 추가로 해소).
     * created_at·updated_at 은 클라이언트 기본값이 채운다. 삽입 id 는 반환하지 않고 호출부가 findByKakaoIds 로 재조회해 확정한다.
     */
    fun insertIgnoringConflicts(places: List<Place>) {
        // kakao id 가 비어 있으면 dedup 키로 쓸 수 없어 삽입하지 않는다(방어 — 어댑터에서 이미 걸러지지만 이중 안전).
        PlaceTable.batchInsert(places.filterNot { it.kakaoPlaceId.isNullOrBlank() }, ignore = true) { place ->
            this[PlaceTable.status] = place.status
            this[PlaceTable.name] = place.name
            this[PlaceTable.description] = place.description
            this[PlaceTable.category] = place.category
            this[PlaceTable.location] = place.location
            this[PlaceTable.address] = place.address
            this[PlaceTable.areaCode] = place.areaCode
            this[PlaceTable.imageUrl] = place.imageUrl
            this[PlaceTable.businessStatus] = place.businessStatus
            this[PlaceTable.kakaoPlaceId] = place.kakaoPlaceId
        }
    }
}
