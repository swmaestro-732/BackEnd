package com.example.backend.user.application.service

import com.example.backend.user.application.port.outbound.CourseInteractionPort
import com.example.backend.user.application.port.outbound.FollowPersistencePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant

class CourseInteractionServiceTest {
    private val interactionPort = mock(CourseInteractionPort::class.java)
    private val followPort = mock(FollowPersistencePort::class.java)
    private val service = CourseInteractionService(interactionPort, followPort)

    @Test
    fun `courseIds 가 비어 있으면 port 를 호출하지 않고 빈 목록을 반환한다`() {
        val result = service.getViewerStates(userId = 1L, courseIds = emptyList())

        assertTrue(result.isEmpty())
        verify(interactionPort, never()).findSavedCourseIds(1L, emptyList())
        verify(interactionPort, never()).findCompletedAt(1L, emptyList())
    }

    @Test
    fun `저장된 코스는 hasSaved=true, 미저장은 false 다`() {
        `when`(interactionPort.findSavedCourseIds(1L, listOf(10L, 20L))).thenReturn(setOf(10L))
        `when`(interactionPort.findCompletedAt(1L, listOf(10L, 20L))).thenReturn(emptyMap())

        val result = service.getViewerStates(1L, listOf(10L, 20L))

        assertEquals(2, result.size)
        val saved = result.first { it.courseId == 10L }
        val notSaved = result.first { it.courseId == 20L }
        assertTrue(saved.hasSaved)
        assertFalse(notSaved.hasSaved)
    }

    @Test
    fun `완주한 코스는 completedAt 이 채워진다`() {
        val completedTime = Instant.parse("2026-08-01T00:00:00Z")
        `when`(interactionPort.findSavedCourseIds(1L, listOf(10L))).thenReturn(setOf(10L))
        `when`(interactionPort.findCompletedAt(1L, listOf(10L))).thenReturn(mapOf(10L to completedTime))

        val result = service.getViewerStates(1L, listOf(10L))

        assertEquals(completedTime, result.first().completedAt)
    }

    @Test
    fun `isFollowing 은 FollowPersistencePort 에 위임한다`() {
        `when`(followPort.isFollowing(1L, 2L)).thenReturn(true)

        assertTrue(service.isFollowing(1L, 2L))

        `when`(followPort.isFollowing(1L, 3L)).thenReturn(false)
        assertFalse(service.isFollowing(1L, 3L))
    }
}
