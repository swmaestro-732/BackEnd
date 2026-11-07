package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseCounterUseCase
import com.example.backend.course.application.port.inbound.CourseLikeUseCase
import com.example.backend.course.application.port.inbound.CourseTraceUseCase
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.course.application.port.inbound.PlanUseCase
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class UserWithdrawalCleanupServiceTest {
    private val courseCounterUseCase = mock(CourseCounterUseCase::class.java)
    private val courseLikeUseCase = mock(CourseLikeUseCase::class.java)
    private val courseTraceUseCase = mock(CourseTraceUseCase::class.java)
    private val courseUseCase = mock(CourseUseCase::class.java)
    private val planUseCase = mock(PlanUseCase::class.java)
    private val service =
        UserWithdrawalCleanupService(
            courseCounterUseCase,
            courseLikeUseCase,
            courseTraceUseCase,
            courseUseCase,
            planUseCase,
        )

    @Test
    fun `저장 수 보정, 좋아요와 따라가기 삭제, 작성 코스와 계획 소프트 삭제를 모두 한다`() {
        service.cleanUpWithdrawnUser(userId = 7L, savedCourseIds = listOf(10L, 11L))

        verify(courseCounterUseCase).decreaseSavesCount(10L)
        verify(courseCounterUseCase).decreaseSavesCount(11L)
        verify(courseLikeUseCase).purgeByUser(7L)
        verify(courseTraceUseCase).purgeByUser(7L)
        verify(courseUseCase).deleteAllByAuthor(7L)
        verify(planUseCase).deleteAllByOwner(7L)
    }
}
