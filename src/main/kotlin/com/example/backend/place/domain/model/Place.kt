package com.example.backend.place.domain.model

import com.example.backend.common.geo.Coordinate
import kotlin.time.Instant

/**
 * Place 도메인. places 테이블의 모든 컬럼을 필드로 보유한다.
 * 신규 생성은 [create] 팩토리로, 저장된 상태 복원(영속 계층 → 도메인)은 [reconstitute] 팩토리로만 한다.
 * 팩토리 우회는 [ConsistentCopyVisibility] 로 차단한다(copy() 도 private).
 *
 * 생성 시점(insert 전)에 미정인 값(id·created_at·updated_at)은 null 로 두고, DB 가 채운 값은 [reconstitute] 로 되돌려 받는다.
 */
@ConsistentCopyVisibility // copy() 도 private 으로 — 팩토리 우회 차단
data class Place private constructor(
    val id: Long?,
    val status: PlaceStatus,
    val name: String,
    val description: String?,
    val category: PlaceCategory,
    val location: Coordinate,
    val address: String,
    val areaCode: String?,
    val imageUrl: String?,
    val businessStatus: PlaceBusinessStatus,
    val kakaoPlaceId: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val deletedAt: Instant?,
) {
    companion object {
        /** 신규 생성 — 도메인 불변식을 검증한다. 생성 시점에 미정인 값(id·타임스탬프)은 null, 상태는 pre-persist 기본값. */
        fun create(
            name: String,
            description: String?,
            category: PlaceCategory,
            location: Coordinate,
            address: String,
            areaCode: String? = null,
            imageUrl: String?,
            businessStatus: PlaceBusinessStatus = PlaceBusinessStatus.UNKNOWN,
            kakaoPlaceId: String? = null,
        ): Place {
            require(name.isNotBlank()) { "이름은 비어 있을 수 없습니다." }
            require(address.isNotBlank()) { "주소는 비어 있을 수 없습니다." }
            return Place(
                id = null,
                status = PlaceStatus.ACTIVE,
                name = name,
                description = description,
                category = category,
                location = location,
                address = address,
                areaCode = areaCode,
                imageUrl = imageUrl,
                businessStatus = businessStatus,
                kakaoPlaceId = kakaoPlaceId,
                createdAt = null,
                updatedAt = null,
                deletedAt = null,
            )
        }

        /**
         * 영속 저장소에서 읽어온 상태로 복원한다(이미 검증된 신뢰 값이라 불변식 재검증 없이 그대로 싣는다).
         * copy() 가 막혀 있어(팩토리 우회 차단) id·DB 생성값을 채운 [Place] 를 만드는 유일한 통로다.
         */
        @Suppress("LongParameterList")
        fun reconstitute(
            id: Long,
            status: PlaceStatus,
            name: String,
            description: String?,
            category: PlaceCategory,
            location: Coordinate,
            address: String,
            areaCode: String? = null,
            imageUrl: String?,
            businessStatus: PlaceBusinessStatus,
            kakaoPlaceId: String?,
            createdAt: Instant?,
            updatedAt: Instant?,
            deletedAt: Instant?,
        ): Place =
            Place(
                id = id,
                status = status,
                name = name,
                description = description,
                category = category,
                location = location,
                address = address,
                areaCode = areaCode,
                imageUrl = imageUrl,
                businessStatus = businessStatus,
                kakaoPlaceId = kakaoPlaceId,
                createdAt = createdAt,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
            )
    }
}
