package com.example.backend.course.adapter.outbound.search

import com.example.backend.course.application.port.inbound.CourseSearchCommand
import com.example.backend.course.application.port.inbound.CourseSearchSort
import com.example.backend.course.application.port.inbound.CourseSearchUseCase
import com.example.backend.support.IntegrationTestBase
import com.example.backend.support.NoriOpenSearchContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * 코스 검색 어댑터+서비스 실동작 검증(nori 컨테이너). 3개 코스를 색인하고 키워드·정렬·커서 페이지네이션을 확인한다.
 * 무거워서 `opensearchIt` 태스크(로컬·워크플로)에서만 실행된다.
 */
class OpenSearchCourseSearchIntegrationTest
    @Autowired
    constructor(
        private val client: OpenSearchClient,
        private val courseSearchUseCase: CourseSearchUseCase,
    ) : IntegrationTestBase() {
        private fun index(
            id: Long,
            title: String,
            savesCnt: Int,
            createdAt: Long,
            tags: List<String> = emptyList(),
            category: String? = null,
            visibility: String = "PUBLIC",
            isPublished: Boolean = true,
        ) {
            val doc =
                CourseDocument(
                    id = id,
                    title = title,
                    description = null,
                    area = "성수",
                    category = category,
                    tags = tags,
                    coverImageUrl = null,
                    visibility = visibility,
                    isPublished = isPublished,
                    userId = "1",
                    likesCnt = 0,
                    savesCnt = savesCnt,
                    createdAt = createdAt,
                )
            client.index {
                it
                    .index("course")
                    .id(id.toString())
                    .document(doc)
                    .refresh(Refresh.True)
            }
        }

        private fun command(
            keyword: String? = null,
            sort: CourseSearchSort = CourseSearchSort.LATEST,
            cursor: String? = null,
            size: Int = 20,
            tags: List<String> = emptyList(),
            category: String? = null,
        ) = CourseSearchCommand(
            keyword,
            area = null,
            category = category,
            tags = tags,
            sort = sort,
            cursor = cursor,
            size = size,
        )

        @Test
        fun `키워드로 코스를 찾고 발행 PUBLIC 만 검색된다`() {
            index(
                id = 1,
                title = "성수 감성 카페 코스",
                savesCnt = 5,
                createdAt = 1000,
                category = "CAFETOUR",
                tags = listOf("데이트"),
            )
            index(id = 2, title = "을지로 노포 술집 투어", savesCnt = 10, createdAt = 2000)
            index(id = 3, title = "비공개 카페 코스", savesCnt = 99, createdAt = 3000, visibility = "PRIVATE")

            val result = courseSearchUseCase.search(command(keyword = "카페"))

            assertThat(result.courses.map { it.id }).containsExactly(1L) // 비공개(3)는 제외
        }

        @Test
        fun `LATEST 는 최신순, POPULAR 는 저장수순으로 정렬한다`() {
            index(id = 1, title = "코스 A", savesCnt = 5, createdAt = 1000)
            index(id = 2, title = "코스 B", savesCnt = 10, createdAt = 2000)
            index(id = 3, title = "코스 C", savesCnt = 1, createdAt = 3000)

            assertThat(courseSearchUseCase.search(command(sort = CourseSearchSort.LATEST)).courses.map { it.id })
                .containsExactly(3L, 2L, 1L)
            assertThat(courseSearchUseCase.search(command(sort = CourseSearchSort.POPULAR)).courses.map { it.id })
                .containsExactly(2L, 1L, 3L)
        }

        @Test
        fun `tags 필터는 태그를 가진 코스만 남긴다`() {
            index(id = 1, title = "코스 A", savesCnt = 5, createdAt = 1000, tags = listOf("데이트", "힐링"))
            index(id = 2, title = "코스 B", savesCnt = 10, createdAt = 2000, tags = listOf("맛집"))

            val result = courseSearchUseCase.search(command(tags = listOf("힐링")))

            assertThat(result.courses.map { it.id }).containsExactly(1L)
        }

        @Test
        fun `커서로 다음 페이지를 이어 조회한다`() {
            index(id = 1, title = "코스 A", savesCnt = 5, createdAt = 1000)
            index(id = 2, title = "코스 B", savesCnt = 10, createdAt = 2000)
            index(id = 3, title = "코스 C", savesCnt = 1, createdAt = 3000)

            val first = courseSearchUseCase.search(command(sort = CourseSearchSort.LATEST, size = 2))
            assertThat(first.courses.map { it.id }).containsExactly(3L, 2L)
            assertThat(first.hasNext).isTrue()

            val second =
                courseSearchUseCase.search(
                    command(sort = CourseSearchSort.LATEST, size = 2, cursor = first.nextCursor),
                )
            assertThat(second.courses.map { it.id }).containsExactly(1L)
            assertThat(second.hasNext).isFalse()
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
