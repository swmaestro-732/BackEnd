package com.example.backend.mobile.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.port.inbound.PlanQueryUseCase
import com.example.backend.course.application.port.inbound.dto.PlanDetailResult
import com.example.backend.course.application.port.inbound.dto.PlanPlaceResult
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant

/**
 * [PlanScreenService] 단위 테스트 — 조합 서비스가 도메인 포트를 어떻게 부르는지만 본다.
 * 소유권 판정은 계획 도메인 몫이라 여기선 예외가 그대로 전파되는지와, 장소를 배치로 한 번만 조회하는지를 검증한다.
 */
class PlanScreenServiceTest {
    private val planQueryUseCase = mock(PlanQueryUseCase::class.java)
    private val placeQueryUseCase = mock(PlaceQueryUseCase::class.java)
    private val service = PlanScreenService(planQueryUseCase, placeQueryUseCase)

    @Test
    fun `계획 상세와 장소 요약을 묶어 돌려준다`() {
        `when`(planQueryUseCase.getDetail(PLAN_ID, USER_ID)).thenReturn(planDetail())
        `when`(placeQueryUseCase.findPlacesById(listOf(1L, 2L))).thenReturn(listOf(place(1L), place(2L)))

        val screen = service.getScreen(PLAN_ID, USER_ID)

        assertEquals(PLAN_ID, screen.plan.id)
        assertEquals(listOf(1L, 2L), screen.places.map { it.id })
    }

    @Test
    fun `같은 장소를 두 번 담아도 장소 조회는 한 번만 요청한다`() {
        `when`(planQueryUseCase.getDetail(PLAN_ID, USER_ID))
            .thenReturn(
                planDetail(
                    places =
                        listOf(
                            PlanPlaceResult(placeId = 1L, orderNo = 0, memo = null, walkingMinutesToNext = 5),
                            PlanPlaceResult(placeId = 1L, orderNo = 1, memo = null, walkingMinutesToNext = null),
                        ),
                ),
            )
        `when`(placeQueryUseCase.findPlacesById(listOf(1L))).thenReturn(listOf(place(1L)))

        service.getScreen(PLAN_ID, USER_ID)

        verify(placeQueryUseCase).findPlacesById(listOf(1L))
    }

    @Test
    fun `삭제된 장소가 섞여도 빠진 채로 그대로 돌려준다`() {
        `when`(planQueryUseCase.getDetail(PLAN_ID, USER_ID)).thenReturn(planDetail())
        // place 2 가 삭제돼 요약이 빠진 상황 — 조합은 실패하지 않고 웹 매퍼가 null 로 채운다.
        `when`(placeQueryUseCase.findPlacesById(listOf(1L, 2L))).thenReturn(listOf(place(1L)))

        val screen = service.getScreen(PLAN_ID, USER_ID)

        assertEquals(2, screen.plan.places.size)
        assertEquals(1, screen.places.size)
    }

    @Test
    fun `계획이 없거나 타인 것이면 장소를 조회하지 않고 404 가 그대로 올라온다`() {
        `when`(planQueryUseCase.getDetail(PLAN_ID, USER_ID))
            .thenThrow(BusinessException(CourseErrorCode.PLAN_NOT_FOUND))

        val exception = assertThrows<BusinessException> { service.getScreen(PLAN_ID, USER_ID) }

        assertEquals(CourseErrorCode.PLAN_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(placeQueryUseCase)
    }

    private fun planDetail(
        places: List<PlanPlaceResult> =
            listOf(
                PlanPlaceResult(placeId = 1L, orderNo = 0, memo = "첫 장소", walkingMinutesToNext = 5),
                PlanPlaceResult(placeId = 2L, orderNo = 1, memo = null, walkingMinutesToNext = null),
            ),
    ) = PlanDetailResult(
        id = PLAN_ID,
        title = "토요일 성수 데이트",
        memo = null,
        plannedDate = null,
        sourceCourseId = null,
        createdAt = Instant.parse("2026-09-18T07:00:00Z"),
        updatedAt = Instant.parse("2026-09-18T08:00:00Z"),
        places = places,
    )

    private fun place(id: Long) =
        PlaceSummary(
            id = id,
            name = "장소 $id",
            category = "CAFE",
            imageUrl = null,
            latitude = 37.5445,
            longitude = 127.0575,
            address = "서울 성동구 $id",
            areaCode = null,
        )

    private companion object {
        const val PLAN_ID = 1L
        const val USER_ID = 1L
    }
}
