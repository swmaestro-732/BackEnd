package com.example.backend.bootstrap.search

import com.example.backend.common.persistence.postgis.GeoPoint
import com.example.backend.place.application.port.inbound.PlaceReindexUseCase
import com.example.backend.place.application.port.outbound.PlacePersistencePort
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.support.IntegrationTestBase
import com.example.backend.support.NoriOpenSearchContainer
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * PostgreSQL→OpenSearch 재색인 경로(backfill)의 실동작을 nori 컨테이너로 검증한다.
 * DB 에 장소를 넣고 [PlaceReindexUseCase.reindexAll] 로 색인한 뒤 한글 검색으로 찾히는지 확인한다.
 * 무거워서 `opensearchIt` 태스크에서만 실행된다(클래스명이 *OpenSearch*IntegrationTest 라 태스크가 픽업).
 */
class OpenSearchReindexIntegrationTest
    @Autowired
    constructor(
        private val client: OpenSearchClient,
        private val placeReindexUseCase: PlaceReindexUseCase,
        private val placePersistencePort: PlacePersistencePort,
    ) : IntegrationTestBase() {
        @Test
        fun `DB 의 장소를 재색인하면 한글 검색으로 찾는다`() {
            val place =
                Place.create(
                    name = "성수동 감성 카페",
                    description = null,
                    category = PlaceCategory.CAFE,
                    location = GeoPoint(latitude = 37.544, longitude = 127.055),
                    address = "서울 성동구 성수동",
                    imageUrl = null,
                    kakaoPlaceId = "kakao-reindex-1",
                )
            // 재색인은 자체 트랜잭션(@Transactional)에서 DB 를 읽으므로 삽입을 커밋해 둔다.
            transaction { placePersistencePort.insertIgnoringConflicts(listOf(place)) }

            val indexed = placeReindexUseCase.reindexAll()
            assertTrue(indexed >= 1) { "재색인 문서 수가 0: $indexed" }
            client.indices().refresh { it.index("place") }

            val result =
                client.search(
                    { s ->
                        s.index("place").query { q ->
                            q.match { m -> m.field("name").query { v -> v.stringValue("카페") } }
                        }
                    },
                    Map::class.java,
                )
            assertTrue(result.hits().hits().isNotEmpty()) { "재색인한 place 를 '카페'로 찾지 못함" }
        }

        companion object {
            @JvmStatic
            @DynamicPropertySource
            fun openSearchProperties(registry: DynamicPropertyRegistry) {
                registry.add("opensearch.endpoint") { NoriOpenSearchContainer.endpoint() }
                registry.add("opensearch.username") { "admin" }
                registry.add("opensearch.password") { "admin" }
            }
        }
    }
