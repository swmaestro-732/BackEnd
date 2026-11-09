package com.example.backend.course.adapter.outbound.search

import com.example.backend.course.application.event.CourseCounter
import com.example.backend.course.application.port.inbound.CourseRecommendUseCase
import com.example.backend.course.application.port.inbound.dto.CourseRecommendQuery
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import com.example.backend.support.IntegrationTestBase
import com.example.backend.support.NoriOpenSearchContainer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * 추천 랭킹·저장수 델타 스크립트 실동작 검증(nori 컨테이너). `opensearchIt` 태스크(로컬·워크플로)에서만 실행된다.
 * 관심사 가산은 should 절이라 불일치 코스도 추천에 남아야 하고(minimum_should_match 회귀 방지),
 * painless 델타 스크립트는 실제 엔진에서 가산·0 클램프·미색인 무시가 동작해야 한다.
 */
class OpenSearchCourseRecommendIntegrationTest
    @Autowired
    constructor(
        private val client: OpenSearchClient,
        private val courseRecommendUseCase: CourseRecommendUseCase,
        private val courseSearchIndexPort: CourseSearchIndexPort,
    ) : IntegrationTestBase() {
        @BeforeEach
        @AfterEach
        fun clearIndex() {
            // 추천은 전체 공개 코스가 대상이라 다른 통합 테스트가 남긴 문서에 영향받고, 여기 문서도 뒤 테스트에 새면 안 된다.
            // 스크립트 갱신 직후엔 검색 스냅샷이 옛 버전이라 삭제가 409 로 튄다 — 먼저 refresh 한다.
            client.indices().refresh { r -> r.index(INDEX) }
            client.deleteByQuery { d -> d.index(INDEX).query { q -> q.matchAll { it } }.refresh(true) }
        }

        private fun index(
            id: Long,
            category: String?,
            areaCode: String?,
            savesCnt: Int = 0,
            createdAt: Long = 1_700_000_000_000L,
            visibility: String = "PUBLIC",
        ) {
            val doc =
                CourseDocument(
                    id = id,
                    title = "코스 $id",
                    description = null,
                    area = "성수",
                    areaCode = areaCode,
                    sigunguCode = areaCode?.take(5),
                    category = listOfNotNull(category),
                    tags = emptyList(),
                    coverImageUrl = null,
                    visibility = visibility,
                    isPublished = true,
                    userId = "1",
                    likesCnt = 0,
                    savesCnt = savesCnt,
                    createdAt = createdAt,
                )
            client.index {
                it
                    .index(INDEX)
                    .id(id.toString())
                    .document(doc)
                    .refresh(Refresh.True)
            }
        }

        private fun documentOf(id: Long): CourseDocument? =
            client.get({ g -> g.index(INDEX).id(id.toString()) }, CourseDocument::class.java).source()

        @Test
        fun `관심 테마·지역이 맞는 코스가 앞서지만 맞지 않는 공개 코스도 추천에 남는다`() {
            index(id = 1, category = "CAFETOUR", areaCode = "1168010100") // 테마·시군구 일치
            index(id = 2, category = "FOOD", areaCode = "2611010100") // 불일치
            index(id = 3, category = "CAFETOUR", areaCode = "1168010100", visibility = "PRIVATE")

            val page =
                courseRecommendUseCase.recommend(
                    CourseRecommendQuery(
                        likeThemes = listOf("CAFETOUR"),
                        areaCodes = listOf("1168000000"),
                        cursor = null,
                        size = 10,
                    ),
                )

            assertThat(page.items.map { it.id }).containsExactly(1L, 2L) // 비공개(3)만 제외
            assertThat(page.hasNext).isFalse()
        }

        @Test
        fun `카운터 델타 스크립트는 실제 엔진에서 가산·0 클램프되고 미색인 문서는 무시한다`() {
            index(id = 10, category = "FOOD", areaCode = null, savesCnt = 5)

            courseSearchIndexPort.adjustCounter(10L, CourseCounter.SAVES, 1)
            assertThat(documentOf(10L)?.savesCnt).isEqualTo(6)

            courseSearchIndexPort.adjustCounter(10L, CourseCounter.SAVES, -10)
            assertThat(documentOf(10L)?.savesCnt).isEqualTo(0)

            courseSearchIndexPort.adjustCounter(10L, CourseCounter.LIKES, 2)
            assertThat(documentOf(10L)?.likesCnt).isEqualTo(2)

            assertThatCode {
                courseSearchIndexPort.adjustCounter(
                    999L,
                    CourseCounter.SAVES,
                    1,
                )
            }.doesNotThrowAnyException()
        }

        companion object {
            private const val INDEX = "course"

            @JvmStatic
            @DynamicPropertySource
            fun openSearchProperties(registry: DynamicPropertyRegistry) {
                registry.add("opensearch.endpoint") { NoriOpenSearchContainer.endpoint() }
                registry.add("opensearch.username") { "admin" }
                registry.add("opensearch.password") { "admin" }
            }
        }
    }
