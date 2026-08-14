package com.example.backend.bootstrap.search

import com.example.backend.common.persistence.postgis.GeoPoint
import com.example.backend.place.application.port.outbound.PlaceSearchIndexPort
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import com.example.backend.support.IntegrationTestBase
import com.example.backend.support.NoriOpenSearchContainer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * [OpenSearchIndexInitializer] 와 색인 어댑터의 실동작을 nori 설치 컨테이너로 검증한다.
 * 부팅 시 initializer 가 인덱스·alias 를 만들고, place 색인→한글 검색 라운드트립이 성립하는지 확인한다.
 * 무거워서 `opensearchIt` 태스크(로컬·워크플로)에서만 실행된다.
 */
class OpenSearchIndexIntegrationTest
    @Autowired
    constructor(
        private val client: OpenSearchClient,
        private val placeSearchIndexPort: PlaceSearchIndexPort,
    ) : IntegrationTestBase() {
        @Test
        fun `initializer 가 place course 인덱스와 alias 를 만든다`() {
            assertTrue(client.indices().existsAlias { it.name("place") }.value()) { "place alias 미생성" }
            assertTrue(client.indices().existsAlias { it.name("course") }.value()) { "course alias 미생성" }

            val placeMapping = client.indices().getMapping { it.index("place_v1") }
            val props = placeMapping.result()["place_v1"]!!.mappings().properties()
            // 매핑 필드가 place.json 대로 만들어졌는지(핵심 필드 존재) 확인. geo_point 실동작은 색인 라운드트립이 검증.
            assertTrue(props.keys.containsAll(listOf("name", "category", "location", "areaCode"))) {
                "place 매핑 필드 누락: ${props.keys}"
            }
        }

        @Test
        fun `nori 분석기가 한글을 형태소로 분해한다`() {
            val response =
                client.indices().analyze { a ->
                    a.index("place_v1").analyzer("nori").text("성수동 감성카페거리")
                }
            assertTrue(response.tokens().size > 1) { "nori 토큰이 분해되지 않음: ${response.tokens()}" }
        }

        @Test
        fun `place 를 색인하면 한글 검색으로 찾는다`() {
            val place =
                Place.reconstitute(
                    id = 1001L,
                    status = PlaceStatus.ACTIVE,
                    name = "성수동 감성 카페",
                    description = null,
                    category = PlaceCategory.CAFE,
                    location = GeoPoint(latitude = 37.544, longitude = 127.055),
                    address = "서울 성동구 성수동",
                    areaCode = null,
                    imageUrl = null,
                    businessStatus = PlaceBusinessStatus.UNKNOWN,
                    kakaoPlaceId = null,
                    createdAt = null,
                    updatedAt = null,
                    deletedAt = null,
                )
            placeSearchIndexPort.index(listOf(place))
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
            assertTrue(result.hits().hits().isNotEmpty()) { "색인한 place 를 '카페'로 찾지 못함" }
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
