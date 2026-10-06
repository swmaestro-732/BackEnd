package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseEntity
import com.example.backend.course.adapter.outbound.persistence.exposed.CoursePlaceTable
import com.example.backend.course.application.port.outbound.CoursePlaceStats
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * [CoursePersistenceAdapter.findPlaceStats] 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * 홈 코스 카드 메타(장소 수, 총 도보 시간)를 group by 한 번으로 집계하는지 검증한다 —
 * NULL 구간은 합에서 빠지고, 전부 NULL 이면 0, 요청하지 않은 코스와 장소 없는 코스는 결과에 없다.
 * 각 테스트는 transaction { ... rollback() } 으로 격리한다.
 */
class CoursePersistenceAdapterPlaceStatsTest
    @Autowired
    constructor(
        private val adapter: CoursePersistenceAdapter,
    ) : IntegrationTestBase() {
        @Test
        fun `코스별 장소 수와 도보 시간 합을 집계하고 NULL, 도보 불가(-1) 구간은 더하지 않는다`() {
            transaction {
                val mixed = insertCourse()
                val allNull = insertCourse()
                val unwalkableOnly = insertCourse()
                val empty = insertCourse()
                val notRequested = insertCourse()
                insertPlaces(mixed, 5, -1, 7, null)
                insertPlaces(allNull, null, null)
                insertPlaces(unwalkableOnly, -1, null)
                insertPlaces(notRequested, 30)

                val stats = adapter.findPlaceStats(listOf(mixed, allNull, unwalkableOnly, empty))

                assertThat(stats).containsOnly(
                    entry(mixed, CoursePlaceStats(placeCount = 4, walkingMinutes = 12)),
                    entry(allNull, CoursePlaceStats(placeCount = 2, walkingMinutes = 0)),
                    entry(unwalkableOnly, CoursePlaceStats(placeCount = 2, walkingMinutes = 0)),
                )

                rollback()
            }
        }

        @Test
        fun `빈 id 목록이면 쿼리 없이 빈 결과다`() {
            transaction {
                assertThat(adapter.findPlaceStats(emptyList())).isEmpty()
            }
        }

        private fun insertPlaces(
            courseId: Long,
            vararg walkingMinutes: Int?,
        ) {
            walkingMinutes.forEachIndexed { index, minutes ->
                CoursePlaceTable.insert {
                    it[CoursePlaceTable.courseId] = courseId
                    it[placeId] = 1000L + index
                    it[orderNo] = index.toShort()
                    it[CoursePlaceTable.walkingMinutes] = minutes
                }
            }
        }

        /** course_places 의 FK(course_id→courses) 를 만족시킬 실제 course 하나. */
        private fun insertCourse(): Long =
            CourseEntity
                .new {
                    status = CourseStatus.ACTIVE
                    userId = 1L
                    title = "집계 대상 코스"
                    coverImageUrl = null
                    description = null
                    category = null
                    area = null
                    areaCode = null
                    visitDate = null
                    isPublished = true
                    visibility = CourseVisibility.PUBLIC
                }.id.value
    }
