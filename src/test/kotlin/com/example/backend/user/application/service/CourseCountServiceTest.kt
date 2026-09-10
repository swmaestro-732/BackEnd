package com.example.backend.user.application.service

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.user.application.port.outbound.CourseCountPersistencePort
import com.example.backend.user.application.port.outbound.ProcessedCourseCountEventPort
import com.example.backend.user.domain.model.CourseCountDelta
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [CourseCountService] 단위 테스트 — 공개범위 전이 → 버킷 델타 계산, old==new no-op, eventId 멱등을 검증한다(포트는 페이크).
 * 동기 ACL·SQS 폴백이 공용으로 부르는 유일한 반영 지점이라, 재전송(같은 eventId)에도 한 번만 반영돼야 한다.
 */
class CourseCountServiceTest {
    /** markProcessedIfAbsent: 같은 eventId 는 처음만 true, 이후 false(PK 충돌 흡수 흉내). */
    private val fakeProcessedPort =
        object : ProcessedCourseCountEventPort {
            val seen = mutableSetOf<String>()

            override fun markProcessedIfAbsent(eventId: String): Boolean = seen.add(eventId)
        }

    private data class Applied(
        val userId: Long,
        val delta: CourseCountDelta,
    )

    private val fakePersistence =
        object : CourseCountPersistencePort {
            val calls = mutableListOf<Applied>()

            override fun applyCourseCountDelta(
                userId: Long,
                delta: CourseCountDelta,
            ) {
                calls += Applied(userId, delta)
            }
        }

    private val service = CourseCountService(fakeProcessedPort, fakePersistence)

    @Test
    fun `PUBLIC 신규 발행은 public 델타 +1 로 반영한다`() {
        service.apply(eventId = "e1", authorId = 7L, oldVisibility = null, newVisibility = CourseVisibility.PUBLIC)

        assertEquals(Applied(7L, CourseCountDelta(1, 0, 0)), fakePersistence.calls.single())
    }

    @Test
    fun `PUBLIC to PRIVATE 변경은 public 델타 -1, private 델타 +1 로 반영한다`() {
        service.apply(
            eventId = "e2",
            authorId = 7L,
            oldVisibility = CourseVisibility.PUBLIC,
            newVisibility = CourseVisibility.PRIVATE,
        )

        assertEquals(Applied(7L, CourseCountDelta(-1, 0, 1)), fakePersistence.calls.single())
    }

    @Test
    fun `FOLLOWER 삭제는 follower 델타 -1 로 반영한다`() {
        service.apply(eventId = "e3", authorId = 7L, oldVisibility = CourseVisibility.FOLLOWER, newVisibility = null)

        assertEquals(Applied(7L, CourseCountDelta(0, -1, 0)), fakePersistence.calls.single())
    }

    @Test
    fun `old 와 new 가 같으면 no-op 이라 반영도 이력 기록도 하지 않는다`() {
        service.apply(
            eventId = "noop",
            authorId = 7L,
            oldVisibility = CourseVisibility.PUBLIC,
            newVisibility = CourseVisibility.PUBLIC,
        )

        assertTrue(fakePersistence.calls.isEmpty(), "델타 반영 없음")
        assertTrue(fakeProcessedPort.seen.isEmpty(), "no-op 은 처리 이력도 남기지 않는다")
    }

    @Test
    fun `재전송된 같은 eventId 는 다시 반영하지 않는다`() {
        service.apply(eventId = "dup", authorId = 7L, oldVisibility = null, newVisibility = CourseVisibility.PUBLIC)
        service.apply(eventId = "dup", authorId = 7L, oldVisibility = null, newVisibility = CourseVisibility.PUBLIC)

        assertEquals(1, fakePersistence.calls.size) // 첫 번째만 반영
    }

    @Test
    fun `서로 다른 eventId 는 각각 반영한다`() {
        service.apply(eventId = "a", authorId = 1L, oldVisibility = null, newVisibility = CourseVisibility.PUBLIC)
        service.apply(eventId = "b", authorId = 2L, oldVisibility = null, newVisibility = CourseVisibility.FOLLOWER)

        assertEquals(2, fakePersistence.calls.size)
        assertEquals(listOf(1L, 2L), fakePersistence.calls.map { it.userId })
    }
}
