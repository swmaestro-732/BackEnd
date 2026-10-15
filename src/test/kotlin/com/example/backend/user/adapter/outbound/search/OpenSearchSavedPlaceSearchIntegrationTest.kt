package com.example.backend.user.adapter.outbound.search

import com.example.backend.bootstrap.config.OpenSearchConfig
import com.example.backend.common.geo.Coordinate
import com.example.backend.support.IntegrationTestBase
import com.example.backend.support.NoriOpenSearchContainer
import com.example.backend.user.application.port.inbound.SavedPlaceSearchQueryPort
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import com.example.backend.user.application.port.outbound.SavedPlaceSearchIndexPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant

/** 실제 매핑·alias 와 색인/조회 어댑터의 왕복을 검증한다. `opensearchIt` 에서만 실행한다. */
class OpenSearchSavedPlaceSearchIntegrationTest
    @Autowired
    constructor(
        @Qualifier(OpenSearchConfig.INDEX_CLIENT) private val indexClient: OpenSearchClient,
        private val indexPort: SavedPlaceSearchIndexPort,
        private val queryPort: SavedPlaceSearchQueryPort,
    ) : IntegrationTestBase() {
        private val origin = Coordinate(37.544, 127.056)

        @BeforeEach
        fun clearIndex() {
            indexClient.deleteByQuery { it.index(ALIAS).query { q -> q.matchAll { m -> m } }.refresh(true) }
        }

        @Test
        fun `initializer 가 접두사와 저장 관계 매핑으로 saved_place alias 를 만든다`() {
            assertThat(indexClient.indices().existsAlias { it.index(INDEX).name(ALIAS) }.value()).isTrue()
            val fields =
                indexClient
                    .indices()
                    .getMapping {
                        it.index(
                            INDEX,
                        )
                    }.result()
                    .getValue(INDEX)
                    .mappings()
                    .properties()

            assertThat(fields.keys).containsExactlyInAnyOrder(
                "userId",
                "placeId",
                "location",
                "savedAt",
                "name",
                "category",
                "imageUrl",
                "rating",
                "ratingCount",
            )
            assertThat(fields.getValue("location").isGeoPoint).isTrue()
            assertThat(fields.getValue("savedAt").isDate).isTrue()
        }

        @Test
        fun `저장 관계를 사용자와 2km 반경으로 격리하고 거리순 size 개만 반환한다`() {
            index(
                document(7L, 30L, latitude = 37.5618),
                document(7L, 40L, latitude = 37.563),
                document(8L, 50L, latitude = origin.latitude),
                document(7L, 10L, latitude = 37.549),
                document(7L, 20L, latitude = 37.545),
            )

            val nearest = queryPort.nearestSaved(7L, origin, radiusMeters = 2000.0, size = 2)

            assertThat(nearest.map { it.placeId }).containsExactly(20L, 10L)
            assertThat(nearest.map { it.distanceMeters }).isSorted()
            assertThat(nearest.first().distanceMeters).isBetween(100.0, 120.0)
            assertThat(nearest.last().distanceMeters).isBetween(540.0, 570.0)
            // 표시용 정적 정보는 색인에 중복 저장된 값을 그대로 돌려준다(DB 조회 없음).
            assertThat(nearest.first().name).isEqualTo("장소 20")
            assertThat(nearest.first().category).isEqualTo("CAFE")
            assertThat(nearest.first().imageUrl).isEqualTo("https://img/20.jpg")
            assertThat(nearest.first().rating).isEqualTo(4.8)
            assertThat(nearest.first().ratingCount).isEqualTo(212)
            val allWithinRadius = queryPort.nearestSaved(7L, origin, radiusMeters = 2000.0, size = 10)
            assertThat(allWithinRadius.map { it.placeId }).containsExactly(20L, 10L, 30L)
            assertThat(allWithinRadius.all { it.distanceMeters <= 2000.0 }).isTrue()
            assertThat(queryPort.nearestSaved(8L, origin, 2000.0, 10).map { it.placeId }).containsExactly(50L)
        }

        @Test
        fun `반경 밖 문서와 다른 사용자 문서가 있어도 반경 안 두 개를 모두 반환한다`() {
            index(
                document(7L, 100L, latitude = 37.58),
                document(7L, 200L, latitude = 37.563),
                document(8L, 300L, latitude = origin.latitude),
                document(7L, 20L, latitude = 37.56),
                document(7L, 10L, latitude = 37.55),
            )

            val nearest = queryPort.nearestSaved(7L, origin, radiusMeters = 2010.0, size = 2)

            assertThat(nearest).hasSize(2)
            assertThat(nearest.map { it.placeId }).containsExactly(10L, 20L)
            assertThat(nearest.all { it.distanceMeters <= 2010.0 }).isTrue()
        }

        @Test
        fun `같은 사용자 장소 재색인은 중복 없이 좌표를 갱신한다`() {
            index(document(7L, 10L, latitude = 37.58))
            assertThat(queryPort.nearestSaved(7L, origin, 2000.0, 2)).isEmpty()

            index(document(7L, 10L, latitude = 37.545))

            val nearest = queryPort.nearestSaved(7L, origin, 2000.0, 2)
            assertThat(nearest).hasSize(1)
            assertThat(nearest.single().placeId).isEqualTo(10L)
            assertThat(indexClient.count { it.index(ALIAS) }.count()).isEqualTo(1L)
        }

        @Test
        fun `저장 취소는 같은 장소를 저장한 다른 사용자 문서를 유지한다`() {
            index(document(7L, 10L, latitude = 37.545), document(8L, 10L, latitude = 37.545))

            indexPort.delete(7L, 10L)
            indexClient.indices().refresh { it.index(ALIAS) }

            assertThat(queryPort.nearestSaved(7L, origin, 2000.0, 2)).isEmpty()
            assertThat(queryPort.nearestSaved(8L, origin, 2000.0, 2).map { it.placeId }).containsExactly(10L)
        }

        private fun index(vararg documents: SavedPlaceIndexDocument) {
            indexPort.index(documents.toList())
            indexClient.indices().refresh { it.index(ALIAS) }
        }

        private fun document(
            userId: Long,
            placeId: Long,
            latitude: Double,
        ) = SavedPlaceIndexDocument(
            userId = userId,
            placeId = placeId,
            location = Coordinate(latitude, origin.longitude),
            savedAt = Instant.parse("2026-10-11T08:00:00Z"),
            name = "장소 $placeId",
            category = "CAFE",
            imageUrl = "https://img/$placeId.jpg",
            rating = 4.8,
            ratingCount = 212,
        )

        companion object {
            private const val PREFIX = "saved-place-it-"
            private const val ALIAS = "${PREFIX}saved_place"
            private const val INDEX = "${PREFIX}saved_place_v1"

            @JvmStatic
            @DynamicPropertySource
            fun openSearchProperties(registry: DynamicPropertyRegistry) {
                registry.add("opensearch.endpoint") { NoriOpenSearchContainer.endpoint() }
                registry.add("opensearch.username") { "admin" }
                registry.add("opensearch.password") { "admin" }
                registry.add("opensearch.index-prefix") { PREFIX }
            }
        }
    }
