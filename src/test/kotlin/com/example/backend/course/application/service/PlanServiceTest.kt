package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.course.application.port.inbound.dto.CreatePlanCommand
import com.example.backend.course.application.port.inbound.dto.EditPlanCommand
import com.example.backend.course.application.port.inbound.dto.PlanPlaceCommand
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.PlaceLookupPort
import com.example.backend.course.application.port.outbound.PlaceRef
import com.example.backend.course.application.port.outbound.PlanPersistencePort
import com.example.backend.course.domain.model.Plan
import com.example.backend.course.domain.model.PlanPlace
import kotlinx.datetime.toKotlinLocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.LocalDate
import kotlin.time.Clock

/**
 * [PlanService] 단위 테스트 — 포트를 목으로 대체해 서비스 규칙만 검증한다.
 * 검증 대상: 원본 코스·장소 존재 검증(404), 커맨드→도메인 조립 후 영속 포트 위임, 소유권 은닉(404), 탈퇴 정리 위임.
 * 도메인 [Plan] 은 data class 라 같은 팩토리로 만든 기대값과 동등성 비교로 스텁한다(매처 불필요, [CourseReviewServiceTest] 와 같은 방식).
 */
class PlanServiceTest {
    private val planPersistencePort = mock(PlanPersistencePort::class.java)
    private val coursePersistencePort = mock(CoursePersistencePort::class.java)
    private val placeLookupPort = mock(PlaceLookupPort::class.java)
    private val service = PlanService(planPersistencePort, coursePersistencePort, placeLookupPort)

    @Test
    fun `계획을 저장하고 생성된 id 를 담은 도메인을 돌려준다`() {
        stubPlaces(1L, 2L)
        val expected = newPlan()
        `when`(planPersistencePort.save(expected)).thenReturn(saved(expected))

        val created = service.create(createCommand())

        assertEquals(SAVED_PLAN_ID, created.id)
        verify(planPersistencePort).save(expected) // 커맨드가 도메인으로 그대로 조립돼 위임됐다
    }

    @Test
    fun `원본 코스를 지정하지 않으면 코스 존재를 확인하지 않는다`() {
        stubPlaces(1L, 2L)
        val expected = newPlan()
        `when`(planPersistencePort.save(expected)).thenReturn(saved(expected))

        service.create(createCommand(sourceCourseId = null))

        verifyNoInteractions(coursePersistencePort)
    }

    @Test
    fun `원본 코스가 없으면 4041 이고 저장하지 않는다`() {
        `when`(coursePersistencePort.existsById(999L)).thenReturn(false)

        val exception = assertThrows<BusinessException> { service.create(createCommand(sourceCourseId = 999L)) }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, exception.errorCode)
        verify(planPersistencePort, never()).save(newPlan())
        verifyNoInteractions(placeLookupPort)
    }

    @Test
    fun `없는 장소가 섞여 있으면 4043 이고 저장하지 않는다`() {
        `when`(placeLookupPort.findPlacesByIds(listOf(1L, 2L))).thenReturn(listOf(placeRef(1L)))

        val exception = assertThrows<BusinessException> { service.create(createCommand(sourceCourseId = null)) }

        assertEquals(PlaceErrorCode.PLACE_NOT_FOUND, exception.errorCode)
        verify(planPersistencePort, never()).save(newPlan())
    }

    @Test
    fun `같은 장소를 두 번 담아도 장소 조회는 한 번만 요청한다`() {
        `when`(placeLookupPort.findPlacesByIds(listOf(1L))).thenReturn(listOf(placeRef(1L)))
        val places = listOf(PlanPlaceCommand(placeId = 1L, orderNo = 0, memo = null), PlanPlaceCommand(1L, 1, null))
        val expected =
            Plan.create(
                userId = USER_ID,
                title = TITLE,
                memo = null,
                plannedDate = null,
                sourceCourseId = null,
                places = listOf(PlanPlace(1L, 0, null), PlanPlace(1L, 1, null)),
            )
        `when`(planPersistencePort.save(expected)).thenReturn(saved(expected))

        service.create(createCommand(sourceCourseId = null, memo = null, plannedDate = null, places = places))

        verify(placeLookupPort).findPlacesByIds(listOf(1L))
    }

    @Test
    fun `편집은 저장된 원본 코스를 그대로 잇고 갱신 포트에 위임한다`() {
        val stored = saved(newPlan(sourceCourseId = 901L))
        `when`(planPersistencePort.findById(SAVED_PLAN_ID)).thenReturn(stored)
        stubPlaces(1L, 2L)
        val expected =
            Plan.edit(
                id = SAVED_PLAN_ID,
                userId = USER_ID,
                title = "수정된 제목",
                memo = null,
                plannedDate = null,
                sourceCourseId = 901L, // 요청에 없지만 저장된 값이 유지된다
                places = listOf(PlanPlace(1L, 0, null), PlanPlace(2L, 1, null)),
            )
        `when`(planPersistencePort.update(expected)).thenReturn(expected)

        val edited = service.edit(editCommand(title = "수정된 제목"))

        assertEquals(901L, edited.sourceCourseId)
        verify(planPersistencePort).update(expected)
    }

    @Test
    fun `없는 계획을 편집하면 4047 이다`() {
        `when`(planPersistencePort.findById(SAVED_PLAN_ID)).thenReturn(null)

        val exception = assertThrows<BusinessException> { service.edit(editCommand()) }

        assertEquals(CourseErrorCode.PLAN_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(placeLookupPort)
    }

    @Test
    fun `타인 계획을 편집하면 존재를 드러내지 않고 같은 4047 이다`() {
        `when`(planPersistencePort.findById(SAVED_PLAN_ID)).thenReturn(saved(newPlan(userId = OTHER_USER_ID)))

        val exception = assertThrows<BusinessException> { service.edit(editCommand()) }

        assertEquals(CourseErrorCode.PLAN_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `내 계획을 지우면 소프트 삭제 포트에 위임한다`() {
        `when`(planPersistencePort.findById(SAVED_PLAN_ID)).thenReturn(saved(newPlan()))

        service.delete(userId = USER_ID, planId = SAVED_PLAN_ID)

        verify(planPersistencePort).softDelete(SAVED_PLAN_ID)
    }

    @Test
    fun `없거나 타인 계획을 지우면 4047 이고 삭제하지 않는다`() {
        `when`(planPersistencePort.findById(SAVED_PLAN_ID)).thenReturn(null)
        assertEquals(
            CourseErrorCode.PLAN_NOT_FOUND,
            assertThrows<BusinessException> { service.delete(USER_ID, SAVED_PLAN_ID) }.errorCode,
        )

        `when`(planPersistencePort.findById(SAVED_PLAN_ID)).thenReturn(saved(newPlan(userId = OTHER_USER_ID)))
        assertEquals(
            CourseErrorCode.PLAN_NOT_FOUND,
            assertThrows<BusinessException> { service.delete(USER_ID, SAVED_PLAN_ID) }.errorCode,
        )

        verify(planPersistencePort, never()).softDelete(SAVED_PLAN_ID)
    }

    @Test
    fun `탈퇴 정리는 소유자 기준 일괄 소프트 삭제로 위임한다`() {
        service.deleteAllByOwner(USER_ID)

        verify(planPersistencePort).softDeleteAllByOwner(USER_ID)
    }

    private fun stubPlaces(vararg placeIds: Long) {
        `when`(placeLookupPort.findPlacesByIds(placeIds.toList())).thenReturn(placeIds.map(::placeRef))
    }

    private fun placeRef(id: Long) = PlaceRef(id = id, category = "CAFE", areaCode = AREA_CODE)

    private fun createCommand(
        sourceCourseId: Long? = null,
        memo: String? = "3시 전엔 출발",
        plannedDate: LocalDate? = LocalDate.parse("2026-09-20"),
        places: List<PlanPlaceCommand> = defaultPlaceCommands(),
    ) = CreatePlanCommand(
        userId = USER_ID,
        title = TITLE,
        memo = memo,
        plannedDate = plannedDate,
        sourceCourseId = sourceCourseId,
        places = places,
    )

    private fun editCommand(title: String = TITLE) =
        EditPlanCommand(
            planId = SAVED_PLAN_ID,
            userId = USER_ID,
            title = title,
            memo = null,
            plannedDate = null,
            places = defaultPlaceCommands().map { it.copy(memo = null) },
        )

    private fun defaultPlaceCommands() =
        listOf(
            PlanPlaceCommand(placeId = 1L, orderNo = 0, memo = null),
            PlanPlaceCommand(placeId = 2L, orderNo = 1, memo = null),
        )

    /** 생성 커맨드가 조립하는 것과 같은 도메인 값(스텁 인자·기대값 공용). */
    private fun newPlan(
        userId: Long = USER_ID,
        sourceCourseId: Long? = null,
    ) = Plan.create(
        userId = userId,
        title = TITLE,
        memo = "3시 전엔 출발",
        plannedDate = LocalDate.parse("2026-09-20").toKotlinLocalDate(),
        sourceCourseId = sourceCourseId,
        places = listOf(PlanPlace(1L, 0, null), PlanPlace(2L, 1, null)),
    )

    /** 영속 계층이 돌려주는 모양(생성 id·타임스탬프가 채워진 상태). */
    private fun saved(plan: Plan): Plan {
        val now = Clock.System.now()
        return Plan.reconstitute(
            id = SAVED_PLAN_ID,
            userId = plan.userId,
            title = plan.title,
            memo = plan.memo,
            plannedDate = plan.plannedDate,
            sourceCourseId = plan.sourceCourseId,
            createdAt = now,
            updatedAt = now,
            places = plan.places,
        )
    }

    private companion object {
        const val USER_ID = 1L
        const val OTHER_USER_ID = 2L
        const val SAVED_PLAN_ID = 7L
        const val TITLE = "토요일 성수 데이트"
        const val AREA_CODE = "1120011400"
    }
}
