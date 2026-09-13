package com.example.backend.user.adapter.outbound.persistence.exposed

import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.domain.model.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * added_places(코스 따라가기 중 방문 체크인) Table 매핑 영속성 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * [AddedPlaceTable] 의 DSL insert 가 autoIncrement id 를 발급하고 컬럼을 왕복 매핑하는지 검증한다.
 * FK(added_places.tracing_course_id→tracing_courses, tracing_courses.user_id→users)를 만족시키려
 * user·tracing_course 행을 먼저 만든다. transaction { ... rollback() } 로 격리한다.
 */
class AddedPlacePersistenceTest : IntegrationTestBase() {
    @Test
    fun `체크인을 저장하면 autoIncrement id 를 발급하고 컬럼을 되읽는다`() {
        transaction {
            val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            val tracingCourseId = insertTracingCourse(insertUser("따라가기유저1"))

            val addedId =
                AddedPlaceTable.insert {
                    it[AddedPlaceTable.tracingCourseId] = tracingCourseId
                    it[placeId] = 333L
                    it[createdAt] = now
                }[AddedPlaceTable.id]

            assertThat(addedId).isPositive()

            val row =
                AddedPlaceTable
                    .selectAll()
                    .where { AddedPlaceTable.id eq addedId }
                    .single()
            assertThat(row[AddedPlaceTable.tracingCourseId]).isEqualTo(tracingCourseId)
            assertThat(row[AddedPlaceTable.placeId]).isEqualTo(333L)
            assertThat(row[AddedPlaceTable.createdAt]).isEqualTo(now)
            rollback()
        }
    }

    @Test
    fun `같은 따라가기에 여러 장소를 체크인할 수 있다`() {
        transaction {
            val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            val tracingCourseId = insertTracingCourse(insertUser("따라가기유저2"))

            listOf(100L, 101L, 102L).forEach { pid ->
                AddedPlaceTable.insert {
                    it[AddedPlaceTable.tracingCourseId] = tracingCourseId
                    it[placeId] = pid
                    it[createdAt] = now
                }
            }

            val placeIds =
                AddedPlaceTable
                    .selectAll()
                    .where { AddedPlaceTable.tracingCourseId eq tracingCourseId }
                    .map { it[AddedPlaceTable.placeId] }
            assertThat(placeIds).containsExactlyInAnyOrder(100L, 101L, 102L)
            rollback()
        }
    }

    private fun insertTracingCourse(userId: Long): Long =
        TracingCourseTable.insert {
            it[TracingCourseTable.userId] = userId
            it[courseId] = 22L // cross-domain(course): FK 없음
            it[createdAt] = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        }[TracingCourseTable.id]

    /** tracing_courses.user_id 가 참조하는 활성 사용자 한 명. */
    private fun insertUser(nickname: String): Long =
        UserTable
            .insert {
                it[UserTable.nickname] = nickname
                it[handle] = "h_$nickname"
                it[status] = UserStatus.ACTIVE
                it[followersCnt] = 0
                it[followingsCnt] = 0
                it[publicCoursesCnt] = 0
                it[followerCoursesCnt] = 0
                it[privateCoursesCnt] = 0
            }[UserTable.id]
            .value
}
