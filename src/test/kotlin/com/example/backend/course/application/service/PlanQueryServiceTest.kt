package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.port.inbound.dto.PlansQuery
import com.example.backend.course.application.port.outbound.PlanCursor
import com.example.backend.course.application.port.outbound.PlanPersistencePort
import com.example.backend.course.application.port.outbound.PlanSummaryRow
import com.example.backend.course.domain.model.Plan
import com.example.backend.course.domain.model.PlanPlace
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant
import kotlin.time.Instant as KotlinInstant

/**
 * [PlanQueryService] 단위 테스트 — 소유권 은닉(404), kotlinx→java 시간 타입 변환, 커서 페이지 조립을 본다.
 * hasNext 판정은 size + 1 건을 조회해 잘라내는 규칙이라, 포트가 몇 건을 요청받는지까지 검증한다.
 */
class PlanQueryServiceTest {
    private val planPersistencePort = mock(PlanPersistencePort::class.java)
    private val service = PlanQueryService(planPersistencePort)

    @Test
    fun `상세는 저장된 값을 java 시간 타입으로 옮겨 내려준다`() {
        `when`(planPersistencePort.findById(PLAN_ID)).thenReturn(storedPlan())

        val detail = service.getDetail(planId = PLAN_ID, userId = USER_ID)

        assertEquals(PLAN_ID, detail.id)
        assertEquals("토요일 성수 데이트", detail.title)
        assertEquals("3시 전엔 출발", detail.memo)
        assertEquals(java.time.LocalDate.parse("2026-09-20"), detail.plannedDate)
        assertEquals(901L, detail.sourceCourseId)
        assertEquals(Instant.parse("2026-09-18T07:00:00Z"), detail.createdAt)
        assertEquals(Instant.parse("2026-09-18T08:00:00Z"), detail.updatedAt)
        assertEquals(listOf(1L, 2L), detail.places.map { it.placeId })
        assertEquals(listOf(0, 1), detail.places.map { it.orderNo })
        assertEquals("웨이팅 있으면 옆집", detail.places[0].memo)
        assertNull(detail.places[1].memo)
        assertEquals(listOf(7, null), detail.places.map { it.walkingMinutesToNext })
    }

    @Test
    fun `없는 계획 상세는 4047 이다`() {
        `when`(planPersistencePort.findById(PLAN_ID)).thenReturn(null)

        assertEquals(
            CourseErrorCode.PLAN_NOT_FOUND,
            assertThrows<BusinessException> { service.getDetail(PLAN_ID, USER_ID) }.errorCode,
        )
    }

    @Test
    fun `타인 계획 상세는 존재를 드러내지 않고 같은 4047 이다`() {
        `when`(planPersistencePort.findById(PLAN_ID)).thenReturn(storedPlan(userId = OTHER_USER_ID))

        assertEquals(
            CourseErrorCode.PLAN_NOT_FOUND,
            assertThrows<BusinessException> { service.getDetail(PLAN_ID, USER_ID) }.errorCode,
        )
    }

    @Test
    fun `다음 페이지가 있으면 초과분을 잘라내고 마지막 행으로 커서를 만든다`() {
        // size 2 를 요청하면 포트에는 3건(= size + 1)을 요청해 hasNext 를 판정한다.
        `when`(planPersistencePort.findSummariesByOwner(USER_ID, null, 3))
            .thenReturn(listOf(summary(12L), summary(11L), summary(10L)))

        val result = service.list(PlansQuery(userId = USER_ID, cursor = null, size = 2))

        assertTrue(result.hasNext)
        assertEquals(listOf(12L, 11L), result.plans.map { it.id })
        // 커서는 잘라낸 뒤 마지막 행(11)을 가리킨다 — 초과분(10)이 아니다.
        assertEquals(PlanCursor(updatedAt = updatedAt(11L), id = 11L), PlanCursorCodec.decode(result.nextCursor))
    }

    @Test
    fun `마지막 페이지면 커서가 없다`() {
        `when`(planPersistencePort.findSummariesByOwner(USER_ID, null, 3))
            .thenReturn(listOf(summary(12L), summary(11L)))

        val result = service.list(PlansQuery(userId = USER_ID, cursor = null, size = 2))

        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
        assertEquals(2, result.plans.size)
    }

    @Test
    fun `계획이 없으면 빈 목록이다`() {
        `when`(planPersistencePort.findSummariesByOwner(USER_ID, null, 11)).thenReturn(emptyList())

        val result = service.list(PlansQuery(userId = USER_ID, cursor = null, size = 10))

        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
        assertTrue(result.plans.isEmpty())
    }

    @Test
    fun `받은 커서는 디코딩해 포트로 넘긴다`() {
        val cursor = PlanCursor(updatedAt = updatedAt(11L), id = 11L)
        `when`(planPersistencePort.findSummariesByOwner(USER_ID, cursor, 3)).thenReturn(listOf(summary(10L)))

        val result = service.list(PlansQuery(USER_ID, PlanCursorCodec.encode(cursor), 2))

        assertEquals(listOf(10L), result.plans.map { it.id })
    }

    @Test
    fun `잘못된 커서는 400 이다`() {
        assertEquals(
            CommonErrorCode.INVALID_INPUT,
            assertThrows<BusinessException> { service.list(PlansQuery(USER_ID, "broken", 10)) }.errorCode,
        )
    }

    private fun storedPlan(userId: Long = USER_ID): Plan =
        Plan.reconstitute(
            id = PLAN_ID,
            userId = userId,
            title = "토요일 성수 데이트",
            memo = "3시 전엔 출발",
            plannedDate = LocalDate(2026, 9, 20),
            sourceCourseId = 901L,
            createdAt = KotlinInstant.parse("2026-09-18T07:00:00Z"),
            updatedAt = KotlinInstant.parse("2026-09-18T08:00:00Z"),
            places = listOf(PlanPlace(1L, 0, "웨이팅 있으면 옆집", 7), PlanPlace(2L, 1, null, null)),
        )

    private fun summary(id: Long) =
        PlanSummaryRow(
            id = id,
            title = "계획 $id",
            plannedDate = null,
            placeCount = 2,
            updatedAt = updatedAt(id),
        )

    private fun updatedAt(id: Long): Instant = Instant.parse("2026-09-18T08:00:00Z").plusSeconds(id)

    private companion object {
        const val USER_ID = 1L
        const val OTHER_USER_ID = 2L
        const val PLAN_ID = 7L
    }
}
