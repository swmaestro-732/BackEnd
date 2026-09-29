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

            val placeMapping = client.indices().getMapping { it.index("place_v2") }
            val props = placeMapping.result()["place_v2"]!!.mappings().properties()
            // 매핑 필드가 place.json 대로 만들어졌는지(핵심 필드 존재) 확인. geo_point 실동작은 색인 라운드트립이 검증.
            assertTrue(props.keys.containsAll(listOf("name", "category", "location", "areaCode"))) {
                "place 매핑 필드 누락: ${props.keys}"
            }
            // 텍스트 필드는 복합어 정확도용 커스텀 korean analyzer 로 매핑됐는지(SCRUM-552).
            assertTrue(props.getValue("name").text().analyzer() == "korean") {
                "place.name analyzer 가 korean 이 아님: ${props.getValue("name").text().analyzer()}"
            }
        }

        @Test
        fun `인덱스는 있고 alias 만 없으면 initializer 가 alias 를 복구한다`() {
            // 인덱스 재생성 여부를 판별하려고 course_v2 에 마커 문서를 넣어둔다(재생성되면 사라진다).
            client.index {
                it
                    .index(
                        "course_v2",
                    ).id("__marker__")
                    .document(mapOf("title" to "marker"))
                    .refresh(Refresh.True)
            }
            // 이전 부팅이 create 후 putAlias 전에 죽은 상태를 재현 — course_v2 은 두고 course alias 만 없앤다.
            client.indices().deleteAlias { it.index("course_v2").name("course") }
            assertTrue(!client.indices().existsAlias { it.name("course") }.value()) { "선조건: alias 제거 실패" }

            initializer.run(DefaultApplicationArguments())

            // alias 가 복구되고, 정확히 course_v2 을 가리키며(다른 인덱스가 아니라), 인덱스가 재생성되지 않았다(마커 생존).
            assertTrue(client.indices().existsAlias { it.index("course_v2").name("course") }.value()) {
                "course alias 가 course_v2 을 가리키지 않음"
            }
            assertTrue(client.exists { it.index("course_v2").id("__marker__") }.value()) {
                "course_v2 이 재생성됨(마커 문서 소실)"
            }
        }

        @Test
        fun `korean 분석기가 복합어를 원형과 분해형으로 함께 색인한다(mixed + 사용자 사전)`() {
            // 사용자 사전 규칙 "서울대공원 서울 대공원" 과 decompound_mode=mixed 조합으로
            // 원형(서울대공원)과 분해형(서울, 대공원)이 모두 토큰으로 나와야 부분어 검색이 잡힌다(SCRUM-552).
            val tokens =
                client
                    .indices()
                    .analyze { a -> a.index("course_v2").analyzer("korean").text("서울대공원") }
                    .tokens()
                    .map { it.token() }
            assertTrue(tokens.containsAll(listOf("서울대공원", "서울", "대공원"))) {
                "복합어 원형, 분해형 토큰 누락(mixed/사용자 사전 미적용): $tokens"
            }
        }

        @Test
        fun `korean 분석기가 사용자 사전 단어를 한 토큰으로 유지한다`() {
            // user_dictionary_rules 에 넣은 "샤로수길"(mecab 기본 사전에 없는 상권명)이 통째로 토큰이 돼야
            // 지명 검색이 깨지지 않는다. 사용자 사전 로딩 여부를 확정적으로 검증한다(SCRUM-552).
            val tokens =
                client
                    .indices()
                    .analyze { a -> a.index("course_v2").analyzer("korean").text("샤로수길 맛집") }
                    .tokens()
                    .map { it.token() }
            assertTrue(tokens.contains("샤로수길")) { "사용자 사전 단어가 분해됨(미적용): $tokens" }
        }

        @Test
        fun `korean 분석기가 mecab 기본 사전으로 복합어를 분해하고 조사를 제거한다`() {
            // 사용자 사전에 없는 "카페에서"도 내장 mecab 사전이 카페와 조사 에서로 나누고,
            // nori_part_of_speech 필터가 조사를 걷어낸다. 즉 단어를 추가로 안 넣어도 대부분 동작함을 검증한다.
            val tokens =
                client
                    .indices()
                    .analyze { a -> a.index("course_v2").analyzer("korean").text("카페에서") }
                    .tokens()
                    .map { it.token() }
            assertTrue(tokens.contains("카페")) { "명사 토큰 누락: $tokens" }
            assertTrue(!tokens.contains("에서")) { "조사(에서)가 제거되지 않음: $tokens" }
        }

        @Test
        fun `course 텍스트 필드도 korean 분석기로 매핑된다`() {
            val props =
                client
                    .indices()
                    .getMapping { it.index("course_v2") }
                    .result()["course_v2"]!!
                    .mappings()
                    .properties()
            listOf("title", "description").forEach { field ->
                assertTrue(props.getValue(field).text().analyzer() == "korean") {
                    "course.$field analyzer 가 korean 이 아님: ${props.getValue(field).text().analyzer()}"
                }
            }
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

        @Test
        fun `복합어 부분어로 색인한 place 를 검색해 찾는다`() {
            // 엔드투엔드: 복합 지명이 든 이름을 색인하고 부분어(대공원)로 검색하면 매칭돼야 한다.
            // 이것이 SCRUM-552 가 노리는 실사용 효과다(mixed 분해형 색인 + 질의 분석 일치).
            val place =
                Place.reconstitute(
                    id = 1002L,
                    status = PlaceStatus.ACTIVE,
                    name = "서울대공원 나들이 코스",
                    description = null,
                    category = PlaceCategory.CAFE,
                    location = Coordinate(latitude = 37.427, longitude = 127.019),
                    address = "경기 과천시",
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
                            q.match { m -> m.field("name").query { v -> v.stringValue("대공원") } }
                        }
                    },
                    Map::class.java,
                )
            assertTrue(result.hits().hits().any { (it.source() as Map<*, *>)["name"] == "서울대공원 나들이 코스" }) {
                "복합어 부분어 '대공원'으로 색인 place 를 찾지 못함"
            }
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
