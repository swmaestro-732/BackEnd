package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.outbound.CourseAccessPort
import com.example.backend.user.application.port.outbound.TracingPersistencePort
import com.example.backend.user.application.port.outbound.TracingRow
import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.application.port.outbound.UserProfileRow
import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class TraceCourseServiceTest {
    private val fakeCourseAccess =
        object : CourseAccessPort {
            var existing: Set<Long> = emptySet()
            var coursePlaceIds: Map<Long, List<Long>> = emptyMap()
            val increasedTracingCourseIds = mutableListOf<Long>()

            override fun existsCourse(courseId: Long): Boolean = courseId in existing

            override fun increaseSavesCount(courseId: Long): Int = 1

            override fun decreaseSavesCount(courseId: Long) {}

            override fun getCoursePlaceIds(courseId: Long): List<Long> = coursePlaceIds[courseId] ?: emptyList()

            override fun increaseTracingsCount(courseId: Long): Int {
                increasedTracingCourseIds += courseId
                return 1
            }
        }

    private val fakePort =
        object : TracingPersistencePort {
            var activeByUserCourse: Map<Pair<Long, Long>, Long> = emptyMap()

            // tracingId -> row (completedAt 은 markCompleted 로 갱신), tracingId -> ownerUserId
            val rows = mutableMapOf<Long, TracingRow>()
            val owners = mutableMapOf<Long, Long>()

            // tracingId -> 체크인된 서로 다른 place 집합(멱등 dedup 을 그대로 흉내낸다)
            val checked = mutableMapOf<Long, MutableSet<Long>>()

            var nextId = 100L
            var insertArgs: Pair<Long, Long>? = null
            var markedCompletedAt: Instant? = null

            fun seedOwned(
                tracingId: Long,
                userId: Long,
                courseId: Long,
                completedAt: Instant? = null,
            ) {
                rows[tracingId] = TracingRow(id = tracingId, courseId = courseId, completedAt = completedAt)
                owners[tracingId] = userId
            }

            override fun findActiveByUserCourse(
                userId: Long,
                courseId: Long,
            ): Long? = activeByUserCourse[userId to courseId]

            override fun insertTracing(
                userId: Long,
                courseId: Long,
            ): Long {
                insertArgs = userId to courseId
                return nextId
            }

            override fun findOwned(
                userId: Long,
                tracingId: Long,
            ): TracingRow? = rows[tracingId]?.takeIf { owners[tracingId] == userId }

            override fun checkInPlace(
                tracingId: Long,
                placeId: Long,
            ) {
                checked.getOrPut(tracingId) { mutableSetOf() }.add(placeId)
            }

            override fun countCheckedPlaces(tracingId: Long): Int = checked[tracingId]?.size ?: 0

            override fun markCompleted(
                tracingId: Long,
                at: Instant,
            ) {
                markedCompletedAt = at
                rows[tracingId] = rows.getValue(tracingId).copy(completedAt = at)
            }
        }

    private val fakeUserPort =
        object : UserPersistencePort {
            var activeUserIds: Set<Long>? = null

            override fun lockActive(userIds: List<Long>): Set<Long> =
                activeUserIds?.let { active -> userIds.filterTo(mutableSetOf()) { it in active } }
                    ?: userIds.toSet()

            override fun findAll(): List<User> = TODO()

            override fun findById(id: Long): User? = TODO()

            override fun findByHandle(handle: String): User? = TODO()

            override fun findProfile(userId: Long): UserProfileRow? = TODO()

            override fun findProfiles(userIds: List<Long>): List<UserProfileRow> = TODO()

            override fun save(user: User): User = TODO()

            override fun update(user: User) = TODO()

            override fun applyCourseCountDelta(
                userId: Long,
                publicDelta: Int,
                followerDelta: Int,
                privateDelta: Int,
            ) = TODO()

            override fun softDelete(user: User) = TODO()

            override fun existsByNickname(nickname: String): Boolean = TODO()

            override fun existsByHandle(handle: String): Boolean = TODO()

            override fun findBySocial(
                provider: SocialProvider,
                socialId: String,
            ): User? = TODO()

            override fun findWithdrawnBySocial(
                provider: SocialProvider,
                socialId: String,
            ): User? = TODO()

            override fun existsByNicknameExcludingUser(
                nickname: String,
                excludeUserId: Long,
            ): Boolean = TODO()

            override fun existsByHandleExcludingUser(
                handle: String,
                excludeUserId: Long,
            ): Boolean = TODO()

            override fun saveWithSocial(user: User): User = TODO()

            override fun reactivate(user: User): User = TODO()
        }

    private val service = TraceCourseService(fakePort, fakeCourseAccess, fakeUserPort)

    // --- start ---

    @Test
    fun `유효하면 따라가기를 시작하고 생성된 tracing 을 반환한다`() {
        fakeCourseAccess.existing = setOf(42L)

        val result = service.start(userId = 1L, courseId = 42L)

        assertEquals(1L to 42L, fakePort.insertArgs)
        assertEquals(100L, result.tracingId)
        assertEquals(42L, result.courseId)
        assertNotNull(result.startedAt)
    }

    @Test
    fun `이미 진행중인 코스면 TRACING_ALREADY_STARTED 를 던진다`() {
        fakeCourseAccess.existing = setOf(42L)
        fakePort.activeByUserCourse = mapOf((1L to 42L) to 100L)

        val ex = assertThrows<BusinessException> { service.start(userId = 1L, courseId = 42L) }

        assertEquals(ErrorCode.TRACING_ALREADY_STARTED, ex.errorCode)
        assertNull(fakePort.insertArgs)
    }

    @Test
    fun `따라갈 코스가 없으면 COURSE_NOT_FOUND 를 던진다`() {
        fakeCourseAccess.existing = emptySet()

        val ex = assertThrows<BusinessException> { service.start(userId = 1L, courseId = 42L) }

        assertEquals(ErrorCode.COURSE_NOT_FOUND, ex.errorCode)
        assertNull(fakePort.insertArgs)
    }

    @Test
    fun `탈퇴(비활성) 사용자면 USER_NOT_FOUND 를 던지고 시작하지 않는다`() {
        fakeUserPort.activeUserIds = emptySet()
        fakeCourseAccess.existing = setOf(42L)

        val ex = assertThrows<BusinessException> { service.start(userId = 1L, courseId = 42L) }

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)
        assertNull(fakePort.insertArgs)
    }

    // --- checkInPlace ---

    @Test
    fun `코스 장소를 체크인하면 진행에 반영된다`() {
        fakeCourseAccess.coursePlaceIds = mapOf(42L to listOf(10L, 20L, 30L))
        fakePort.seedOwned(tracingId = 100L, userId = 1L, courseId = 42L)

        val progress = service.checkInPlace(userId = 1L, tracingId = 100L, placeId = 10L)

        assertEquals(3, progress.totalPlaces)
        assertEquals(1, progress.checkedPlaces)
        assertFalse(progress.completed)
        assertNull(progress.completedAt)
    }

    @Test
    fun `코스에 없는 장소를 체크인하면 PLACE_NOT_IN_COURSE 를 던진다`() {
        fakeCourseAccess.coursePlaceIds = mapOf(42L to listOf(10L, 20L))
        fakePort.seedOwned(tracingId = 100L, userId = 1L, courseId = 42L)

        val ex = assertThrows<BusinessException> { service.checkInPlace(userId = 1L, tracingId = 100L, placeId = 99L) }

        assertEquals(ErrorCode.PLACE_NOT_IN_COURSE, ex.errorCode)
        assertEquals(0, fakePort.countCheckedPlaces(100L))
    }

    @Test
    fun `마지막 장소를 체크인하면 완주 처리하고 tracings_cnt 를 증가시킨다`() {
        fakeCourseAccess.coursePlaceIds = mapOf(42L to listOf(10L, 20L))
        fakePort.seedOwned(tracingId = 100L, userId = 1L, courseId = 42L)
        fakePort.checkInPlace(100L, 10L) // 이미 하나 체크인된 상태

        val progress = service.checkInPlace(userId = 1L, tracingId = 100L, placeId = 20L)

        assertTrue(progress.completed)
        assertNotNull(progress.completedAt)
        assertEquals(2, progress.checkedPlaces)
        assertEquals(2, progress.totalPlaces)
        assertNotNull(fakePort.markedCompletedAt)
        assertEquals(listOf(42L), fakeCourseAccess.increasedTracingCourseIds)
    }

    @Test
    fun `같은 장소를 다시 체크인해도 멱등이라 진행 수가 늘지 않는다`() {
        fakeCourseAccess.coursePlaceIds = mapOf(42L to listOf(10L, 20L))
        fakePort.seedOwned(tracingId = 100L, userId = 1L, courseId = 42L)

        service.checkInPlace(userId = 1L, tracingId = 100L, placeId = 10L)
        val progress = service.checkInPlace(userId = 1L, tracingId = 100L, placeId = 10L)

        assertEquals(1, progress.checkedPlaces)
        assertFalse(progress.completed)
        // 2장소 코스가 아직 미완주라 tracings_cnt 증가는 없다.
        assertTrue(fakeCourseAccess.increasedTracingCourseIds.isEmpty())
    }

    @Test
    fun `타인 또는 부재 tracing 을 체크인하면 TRACING_NOT_FOUND 를 던진다`() {
        fakeCourseAccess.coursePlaceIds = mapOf(42L to listOf(10L))
        fakePort.seedOwned(tracingId = 100L, userId = 2L, courseId = 42L) // 소유자는 2L

        val ex = assertThrows<BusinessException> { service.checkInPlace(userId = 1L, tracingId = 100L, placeId = 10L) }

        assertEquals(ErrorCode.TRACING_NOT_FOUND, ex.errorCode)
    }
}
