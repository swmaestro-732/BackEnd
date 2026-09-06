package com.example.backend.bootstrap.search

import com.example.backend.common.geo.Coordinate
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
import org.opensearch.client.opensearch._types.Refresh
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
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
        private val initializer: OpenSearchIndexInitializer,
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
        fun `인덱스는 있고 alias 만 없으면 initializer 가 alias 를 복구한다`() {
            // 인덱스 재생성 여부를 판별하려고 course_v1 에 마커 문서를 넣어둔다(재생성되면 사라진다).
            client.index {
                it
                    .index(
                        "course_v1",
                    ).id("__marker__")
                    .document(mapOf("title" to "marker"))
                    .refresh(Refresh.True)
            }
            // 이전 부팅이 create 후 putAlias 전에 죽은 상태를 재현 — course_v1 은 두고 course alias 만 없앤다.
            client.indices().deleteAlias { it.index("course_v1").name("course") }
            assertTrue(!client.indices().existsAlias { it.name("course") }.value()) { "선조건: alias 제거 실패" }

            initializer.run(DefaultApplicationArguments())

            // alias 가 복구되고, 정확히 course_v1 을 가리키며(다른 인덱스가 아니라), 인덱스가 재생성되지 않았다(마커 생존).
            assertTrue(client.indices().existsAlias { it.index("course_v1").name("course") }.value()) {
                "course alias 가 course_v1 을 가리키지 않음"
            }
            assertTrue(client.exists { it.index("course_v1").id("__marker__") }.value()) {
                "course_v1 이 재생성됨(마커 문서 소실)"
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
                    location = Coordinate(latitude = 37.544, longitude = 127.055),
                    address = "서울 성동구 성수동",
                    areaCode = null,
                    imageUrl = null,
                    businessStatus = PlaceBusinessStatus.UNKNOWN,
                    kakaoPlaceId = null,
                    createdAt = null,
                    updatedAt = null,
                    deletedAt = null,
                )
            placeSearchIndexPort.save(listOf(place))
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
