package com.example.backend.place.adapter.outbound.search

import com.example.backend.common.geo.Coordinate
import com.example.backend.common.geo.Viewport
import com.example.backend.place.application.port.inbound.dto.PlaceMapSort
import com.example.backend.place.application.port.outbound.PlaceMapSearchPort
import com.example.backend.place.application.port.outbound.PlaceSearchCriteria
import com.example.backend.place.application.port.outbound.PlaceSearchQueryPort
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.support.IntegrationTestBase
import com.example.backend.support.NoriOpenSearchContainer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.opensearch.core.bulk.BulkOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.UUID

class OpenSearchPlaceSearchIntegrationTest
    @Autowired
    constructor(
        private val client: OpenSearchClient,
        private val searchPort: PlaceSearchQueryPort,
        private val mapPort: PlaceMapSearchPort,
    ) : IntegrationTestBase() {
        @Test
        fun `같은 이름은 가까운 장소가 먼저지만 먼 정확명도 가까운 부분일치보다 우선한다`() {
            val query = "거리회귀${UUID.randomUUID()}"
            val baseId = nextBaseId()
            val farExact = baseId + 1
            val nearExact = baseId + 2
            val nearPartial = baseId + 3
            val documents =
                listOf(
                    document(farExact, query, Coordinate(35.1, 129.0)),
                    document(nearPartial, "$query 서울점", Coordinate(37.5, 127.0)),
                    document(nearExact, query, Coordinate(37.503, 127.0)),
                )
            try {
                index(documents)

                val hits =
                    searchPort.search(
                        PlaceSearchCriteria(
                            textTokens = listOf(query),
                            categories = emptyList(),
                            areaCodePrefixGroups = emptyList(),
                            viewport = null,
                            from = 0,
                            size = 10,
                            anchor = Coordinate(37.5, 127.0),
                            originalQuery = query,
                        ),
                    )

                assertEquals(3L, hits.totalCount)
                assertEquals(listOf(nearExact, farExact, nearPartial), hits.ids)
            } finally {
                delete(documents.map { it.first })
            }
        }

        @Test
        fun `지도는 카테고리와 뷰포트로 거른 101건 전체를 geo 집계에 포함한다`() {
            val query = "지도회귀${UUID.randomUUID()}"
            val baseId = nextBaseId()
            val documents =
                buildList {
                    repeat(60) { add(document(baseId + it, query, Coordinate(37.2, 126.5))) }
                    repeat(40) { add(document(baseId + 60 + it, query, Coordinate(37.6, 127.2))) }
                    add(document(baseId + 100, query, Coordinate(37.8, 127.8)))
                    add(document(baseId + 101, query, Coordinate(37.2, 126.5), "RESTAURANT"))
                    add(document(baseId + 102, query, Coordinate(38.001, 127.0)))
                    add(document(baseId + 103, "불일치${UUID.randomUUID()}", Coordinate(37.2, 126.5)))
                }
            try {
                index(documents)

                val hits =
                    mapPort.searchMap(
                        PlaceSearchCriteria(
                            textTokens = listOf(query),
                            categories = listOf(PlaceCategory.CAFE),
                            areaCodePrefixGroups = listOf(listOf("11")),
                            viewport = Viewport(Coordinate(37.0, 126.0), Coordinate(38.0, 128.0)),
                            from = 0,
                            size = 100,
                        ),
                        precision = 5,
                        sort = PlaceMapSort.RELEVANCE,
                    )

                assertEquals(101L, hits.totalCount)
                assertEquals(101L, hits.buckets.sumOf { it.count })
                assertEquals(listOf(1L, 40L, 60L), hits.buckets.map { it.count }.sorted())
                assertEquals(baseId + 100, hits.buckets.single { it.count == 1L }.singlePlaceId)
                assertTrue(hits.ids.size <= 100)
            } finally {
                delete(documents.map { it.first })
            }
        }

        @Test
        fun `지도 거리순은 기준점에서 가까운 히트부터 준다`() {
            val query = "거리정렬${UUID.randomUUID()}"
            val baseId = nextBaseId()
            val far = baseId + 1
            val near = baseId + 2
            val mid = baseId + 3
            val documents =
                listOf(
                    document(far, query, Coordinate(37.9, 127.9)),
                    document(near, query, Coordinate(37.51, 127.01)),
                    document(mid, query, Coordinate(37.7, 127.5)),
                )
            try {
                index(documents)

                val hits =
                    mapPort.searchMap(
                        PlaceSearchCriteria(
                            textTokens = listOf(query),
                            categories = emptyList(),
                            areaCodePrefixGroups = emptyList(),
                            viewport = Viewport(Coordinate(37.0, 126.0), Coordinate(38.0, 128.0)),
                            from = 0,
                            size = 100,
                            anchor = Coordinate(37.5, 127.0),
                        ),
                        precision = 5,
                        sort = PlaceMapSort.DISTANCE,
                    )

                assertEquals(listOf(near, mid, far), hits.ids)
                assertEquals(listOf(near, mid, far), hits.buckets.map { it.singlePlaceId })
            } finally {
                delete(documents.map { it.first })
            }
        }

        @Test
        fun `목록과 지도는 지역 그룹을 모두 만족하는 같은 토큰의 후보만 검색한다`() {
            val query = "지역교집합${UUID.randomUUID()}"
            val baseId = nextBaseId()
            val seoulJung = baseId + 1
            val seoulJungnang = baseId + 2
            val documents =
                listOf(
                    document(seoulJung, query, Coordinate(37.5, 127.0), areaCode = "1114010100"),
                    document(seoulJungnang, query, Coordinate(37.6, 127.1), areaCode = "1126010100"),
                    document(baseId + 3, query, Coordinate(37.5, 127.0), areaCode = "1168010100"),
                    document(baseId + 4, query, Coordinate(37.5, 127.0), areaCode = "2611010100"),
                )
            try {
                index(documents)
                val criteria =
                    PlaceSearchCriteria(
                        textTokens = listOf(query),
                        categories = listOf(PlaceCategory.CAFE),
                        areaCodePrefixGroups = listOf(listOf("11"), listOf("11140", "11260", "26110")),
                        viewport = null,
                        from = 0,
                        size = 10,
                    )

                val listHits = searchPort.search(criteria)
                val mapHits =
                    mapPort.searchMap(
                        criteria.copy(viewport = Viewport(Coordinate(37.0, 126.0), Coordinate(38.0, 128.0))),
                        precision = 5,
                        sort = PlaceMapSort.RELEVANCE,
                    )

                assertEquals(2L, listHits.totalCount)
                assertEquals(setOf(seoulJung, seoulJungnang), listHits.ids.toSet())
                assertEquals(2L, mapHits.totalCount)
                assertEquals(setOf(seoulJung, seoulJungnang), mapHits.ids.toSet())
                assertEquals(2L, mapHits.buckets.sumOf { it.count })
            } finally {
                delete(documents.map { it.first })
            }
        }

        private fun document(
            id: Long,
            name: String,
            coordinate: Coordinate,
            category: String = "CAFE",
            areaCode: String = "1168010100",
        ): Pair<Long, Map<String, Any>> =
            id to
                mapOf(
                    "name" to name,
                    "category" to category,
                    "status" to "ACTIVE",
                    "areaCode" to areaCode,
                    "address" to "검색 회귀 테스트 주소",
                    "location" to mapOf("lat" to coordinate.latitude, "lon" to coordinate.longitude),
                )

        private fun index(documents: List<Pair<Long, Map<String, Any>>>) {
            val result =
                client.bulk { bulk ->
                    bulk.refresh(Refresh.True).operations(
                        documents.map { (id, document) ->
                            BulkOperation
                                .Builder()
                                .index {
                                    it
                                        .index(
                                            "place",
                                        ).id(id.toString())
                                        .document(document)
                                }.build()
                        },
                    )
                }
            assertFalse(result.errors(), "테스트 문서 색인 실패: ${result.items().mapNotNull { it.error() }}")
        }

        private fun delete(ids: List<Long>) {
            val result =
                client.bulk { bulk ->
                    bulk.refresh(Refresh.True).operations(
                        ids.map { id ->
                            BulkOperation.Builder().delete { it.index("place").id(id.toString()) }.build()
                        },
                    )
                }
            assertFalse(result.errors(), "테스트 문서 삭제 실패")
        }

        private fun nextBaseId(): Long =
            9_000_000_000_000L + (UUID.randomUUID().mostSignificantBits and 0x7FFFFFFFL) * 1000

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
