package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseEntity
import com.example.backend.course.application.port.outbound.CourseRegionFilter
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * [CoursePersistenceAdapter.findPublishedPublic] 의 지역(region) 필터 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 * include prefix 는 OR, exclude prefix 는 AND NOT 으로 area_code 에 걸리는지 검증한다. 각 테스트는 transaction { ... rollback() } 으로 격리한다.
 */
class CoursePersistenceAdapterFindPublishedPublicTest
    @Autowired
    constructor(
        private val adapter: CoursePersistenceAdapter,
    ) : IntegrationTestBase() {
        @Test
        fun `region include prefix 중 하나로 시작하는 공개 코스만 남고 exclude prefix 는 뺀다`() {
            transaction {
                val gangnam = insertCourse("1168010100")
                val seongdong = insertCourse("1120011400")
                val gwangjuDong = insertCourse("1221010100")
                val jeonnam = insertCourse("1211010100")
                val noArea = insertCourse(null)

                val seoul =
                    adapter
                        .findPublishedPublic(
                            null,
                            PAGE,
                            null,
                            CourseRegionFilter(includePrefixes = listOf("11")),
                        ).map {
                            it.id
                        }
                assertThat(seoul).contains(gangnam, seongdong).doesNotContain(gwangjuDong, jeonnam, noArea)

                val jeonnamOnly =
                    adapter
                        .findPublishedPublic(
                            null,
                            PAGE,
                            null,
                            CourseRegionFilter(includePrefixes = listOf("12"), excludePrefixes = listOf("12210")),
                        ).map { it.id }
                assertThat(jeonnamOnly).contains(jeonnam).doesNotContain(gwangjuDong, gangnam, seongdong, noArea)

                val twoSido =
                    adapter
                        .findPublishedPublic(
                            null,
                            PAGE,
                            null,
                            CourseRegionFilter(includePrefixes = listOf("1168", "1221")),
                        ).map {
                            it.id
                        }
                assertThat(twoSido).contains(gangnam, gwangjuDong).doesNotContain(seongdong, jeonnam, noArea)

                val all = adapter.findPublishedPublic(null, PAGE, null, null).map { it.id }
                assertThat(all).contains(gangnam, seongdong, gwangjuDong, jeonnam, noArea)

                rollback()
            }
        }

        private fun insertCourse(areaCode: String?): Long =
            CourseEntity
                .new {
                    status = CourseStatus.ACTIVE
                    userId = 1L
                    title = "지역 필터 대상 코스"
                    coverImageUrl = null
                    description = null
                    area = null
                    this.areaCode = areaCode
                    visitDate = null
                    isPublished = true
                    visibility = CourseVisibility.PUBLIC
                }.id.value

        private companion object {
            /** 기존 데이터가 있어도 삽입한 코스가 전부 첫 페이지에 들어오게 넉넉히 잡는다. */
            const val PAGE = 10_000
        }
    }
