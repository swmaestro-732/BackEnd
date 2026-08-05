package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.outbound.AreaCodeLookupPort
import com.example.backend.place.application.port.outbound.ExternalPlaceSearchPort
import com.example.backend.place.application.port.outbound.PlacePersistencePort
import com.example.backend.place.domain.model.ExternalPlace
import com.example.backend.place.domain.model.ExternalPlaceSource
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionOperations

/**
 * 검색 결과 dedup 저장 검증 — 같은 장소를 두 번 검색해도 places 는 한 번만 저장되고 같은 내부 id 를 재사용한다.
 * 실 DB 대신 in-memory 페이크 영속 포트로 dedup 로직만 좁혀 확인한다.
 */
class PlaceSearchServiceTest {
    private val external =
        listOf(
            externalPlace("k1", "콤포트 성수"),
            externalPlace("k2", "블루보틀 성수"),
        )
    private val externalPort =
        object : ExternalPlaceSearchPort {
            override fun search(
                query: String,
                near: Coordinate?,
            ) = external
        }
    private val persistence = FakePlacePersistencePort()

    // 좌표→법정동 변환은 이 테스트의 관심사(dedup)가 아니라 항상 미확인(null)으로 둔다.
    private val areaCodeLookup =
        object : AreaCodeLookupPort {
            override fun findLegalDongCode(coordinate: Coordinate): String? = null
        }

    // 단위 테스트는 실 DB 트랜잭션이 없으므로 콜백만 실행하는 withoutTransaction 을 쓴다.
    private val service =
        PlaceSearchService(externalPort, persistence, areaCodeLookup, TransactionOperations.withoutTransaction())

    @Test
    fun `같은 장소를 두 번 검색하면 저장은 한 번, id 는 재사용된다`() {
        val first = service.search("성수 카페", null)
        assertEquals(2, first.size)
        first.forEach { assertNotNull(it.id) }
        assertEquals(2, persistence.rowCount())

        val second = service.search("성수 카페", null)
        assertEquals(2, second.size)
        // 두 번째 호출은 재삽입 없이 기존 row 를 재사용한다.
        assertEquals(2, persistence.rowCount())
        assertEquals(first.map { it.id }.toSet(), second.map { it.id }.toSet())
    }

    private fun externalPlace(
        kakaoId: String,
        name: String,
    ) = ExternalPlace(
        kakaoPlaceId = kakaoId,
        name = name,
        category = PlaceCategory.CAFE,
        roadAddress = "서울 성동구 서울숲2길 3",
        address = "서울 성동구 성수동1가",
        coordinate = Coordinate(latitude = 37.5432, longitude = 127.0561),
        telephone = "02-9876-5432",
        source = ExternalPlaceSource.KAKAO,
    )

    /** kakao id 로 dedup 하고 삽입 시 순차 id 를 부여하는 in-memory 페이크. */
    private class FakePlacePersistencePort : PlacePersistencePort {
        private val store = mutableMapOf<String, Place>()
        private var seq = 0L

        fun rowCount(): Int = store.size

        override fun findByKakaoIds(kakaoIds: List<String>): List<Place> = kakaoIds.mapNotNull { store[it] }

        // insert ignore 모사 — 이미 있는 kakao id 는 건너뛴다(충돌 무시). id 는 삽입 시에만 부여.
        override fun insertIgnoringConflicts(places: List<Place>) {
            places.forEach { place ->
                val kakaoId = place.kakaoPlaceId!!
                if (store.containsKey(kakaoId)) return@forEach
                store[kakaoId] =
                    Place.reconstitute(
                        id = ++seq,
                        status = PlaceStatus.ACTIVE,
                        name = place.name,
                        description = place.description,
                        category = place.category,
                        location = place.location,
                        address = place.address,
                        imageUrl = place.imageUrl,
                        businessStatus = PlaceBusinessStatus.UNKNOWN,
                        kakaoPlaceId = kakaoId,
                        createdAt = null,
                        updatedAt = null,
                        deletedAt = null,
                    )
            }
        }
    }
}
