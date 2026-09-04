package com.example.backend.user.application.service

import com.example.backend.user.application.port.inbound.UserCourseCountUseCase
import com.example.backend.user.application.port.outbound.ProcessedCourseCountEventPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * [CourseCountMessageHandler] 단위 테스트 — 멱등 처리만 검증한다(포트는 페이크로 대체).
 * 처음 보는 eventId 는 한 번 반영하고, 재전송된 같은 eventId 는 반영하지 않는다.
 */
class CourseCountMessageHandlerTest {
    /** markProcessedIfAbsent: 같은 eventId 는 처음만 true, 이후 false(PK 충돌 흡수 흉내). */
    private val fakeProcessedPort =
        object : ProcessedCourseCountEventPort {
            val seen = mutableSetOf<String>()

            override fun markProcessedIfAbsent(eventId: String): Boolean = seen.add(eventId)
        }

    private data class Applied(
        val userId: Long,
        val publicDelta: Int,
        val followerDelta: Int,
        val privateDelta: Int,
    )

    private val fakeUseCase =
        object : UserCourseCountUseCase {
            val calls = mutableListOf<Applied>()

            override fun applyCourseCountDelta(
                userId: Long,
                publicDelta: Int,
                followerDelta: Int,
                privateDelta: Int,
            ) {
                calls += Applied(userId, publicDelta, followerDelta, privateDelta)
            }
        }

    private val handler = CourseCountMessageHandler(fakeProcessedPort, fakeUseCase)

    @Test
    fun `처음 보는 eventId 는 델타를 한 번 반영한다`() {
        handler.handle(eventId = "e1", userId = 7L, publicDelta = 1, followerDelta = 0, privateDelta = -1)

        assertEquals(1, fakeUseCase.calls.size)
        val applied = fakeUseCase.calls.single()
        assertEquals(7L, applied.userId)
        assertEquals(1, applied.publicDelta)
        assertEquals(0, applied.followerDelta)
        assertEquals(-1, applied.privateDelta)
    }

    @Test
    fun `재전송된 같은 eventId 는 델타를 다시 반영하지 않는다`() {
        handler.handle(eventId = "dup", userId = 7L, publicDelta = 1, followerDelta = 0, privateDelta = 0)
        handler.handle(eventId = "dup", userId = 7L, publicDelta = 1, followerDelta = 0, privateDelta = 0)

        assertEquals(1, fakeUseCase.calls.size) // 첫 번째만 반영
    }

    @Test
    fun `서로 다른 eventId 는 각각 반영한다`() {
        handler.handle(eventId = "a", userId = 1L, publicDelta = 1, followerDelta = 0, privateDelta = 0)
        handler.handle(eventId = "b", userId = 2L, publicDelta = 0, followerDelta = 1, privateDelta = 0)

        assertEquals(2, fakeUseCase.calls.size)
        assertEquals(listOf(1L, 2L), fakeUseCase.calls.map { it.userId })
        assertNull(fakeUseCase.calls.firstOrNull { it.userId == 3L })
    }
}
