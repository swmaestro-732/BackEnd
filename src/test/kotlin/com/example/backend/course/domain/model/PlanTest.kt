package com.example.backend.course.domain.model

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.CourseErrorCode
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [Plan] 도메인 단위 테스트(Spring 컨텍스트 없음).
 * 팩토리 불변식만 본다 — 장소 최소 1곳·orderNo 중복 금지·제목/메모 정규화·장소 정렬, 그리고 소유 정책.
 * 웹 DTO 의 Bean Validation 과 상한이 겹치는 항목이 있지만, 다른 진입점에서도 규칙이 유지되는지는 여기서 보장한다.
 */
class PlanTest {
    @Test
    fun `생성 직후에는 id·타임스탬프가 비어 있고 소유자와 원본 코스를 싣는다`() {
        val plan = plan(sourceCourseId = 12L)

        assertNull(plan.id)
        assertNull(plan.createdAt)
        assertNull(plan.updatedAt)
        assertEquals(USER_ID, plan.userId)
        assertEquals(12L, plan.sourceCourseId)
    }

    @Test
    fun `장소를 한 곳도 담지 않으면 거부한다`() {
        val exception = assertThrows<BusinessException> { plan(places = emptyList()) }

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.errorCode)
    }

    @Test
    fun `코스와 달리 장소 한 곳만으로도 계획을 만들 수 있다`() {
        val plan = plan(places = listOf(place(placeId = 1, orderNo = 0)))

        assertEquals(1, plan.places.size)
    }

    @Test
    fun `orderNo 가 중복되면 거부한다`() {
        val exception =
            assertThrows<BusinessException> {
                plan(places = listOf(place(placeId = 1, orderNo = 0), place(placeId = 2, orderNo = 0)))
            }

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.errorCode)
    }

    @Test
    fun `장소는 orderNo 오름차순으로 정렬해 담는다`() {
        val plan =
            plan(
                places =
                    listOf(
                        place(placeId = 3, orderNo = 2),
                        place(placeId = 1, orderNo = 0),
                        place(placeId = 2, orderNo = 1),
                    ),
            )

        assertEquals(listOf(1L, 2L, 3L), plan.places.map { it.placeId })
        assertEquals(listOf(0, 1, 2), plan.places.map { it.orderNo })
    }

    @Test
    fun `제목은 앞뒤 공백을 잘라 저장한다`() {
        assertEquals("토요일 성수 데이트", plan(title = "  토요일 성수 데이트  ").title)
    }

    @Test
    fun `계획 메모는 트림 후 빈 값이면 null 이다`() {
        assertEquals("3시 전엔 출발", plan(memo = "  3시 전엔 출발  ").memo)
        assertNull(plan(memo = "   ").memo)
        assertNull(plan(memo = null).memo)
    }

    @Test
    fun `장소 메모도 같은 규칙으로 정규화한다`() {
        val plan =
            plan(
                places =
                    listOf(
                        place(placeId = 1, orderNo = 0, memo = "  웨이팅 있으면 옆집  "),
                        place(placeId = 2, orderNo = 1, memo = "  "),
                    ),
            )

        assertEquals("웨이팅 있으면 옆집", plan.places[0].memo)
        assertNull(plan.places[1].memo)
    }

    @Test
    fun `편집은 생성과 같은 불변식을 적용하고 id·원본 코스를 싣는다`() {
        val edited =
            Plan.edit(
                id = 7L,
                userId = USER_ID,
                title = "수정",
                memo = null,
                plannedDate = LocalDate(2026, 9, 21),
                sourceCourseId = 12L,
                places = listOf(place(placeId = 1, orderNo = 0)),
            )

        assertEquals(7L, edited.id)
        assertEquals(12L, edited.sourceCourseId)
        assertEquals(LocalDate(2026, 9, 21), edited.plannedDate)

        assertThrows<BusinessException> {
            Plan.edit(
                id = 7L,
                userId = USER_ID,
                title = "수정",
                memo = null,
                plannedDate = null,
                sourceCourseId = null,
                places = emptyList(),
            )
        }
    }

    @Test
    fun `소유자가 아니면 존재를 드러내지 않고 PLAN_NOT_FOUND 로 막는다`() {
        val exception = assertThrows<BusinessException> { Plan.ensureOwned(ownerId = USER_ID, requesterId = 99L) }

        assertEquals(CourseErrorCode.PLAN_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `소유자 본인은 통과한다`() {
        Plan.ensureOwned(ownerId = USER_ID, requesterId = USER_ID)
    }

    private fun plan(
        title: String = "토요일 성수 데이트",
        memo: String? = null,
        plannedDate: LocalDate? = null,
        sourceCourseId: Long? = null,
        places: List<PlanPlace> = listOf(place(placeId = 1, orderNo = 0)),
    ): Plan =
        Plan.create(
            userId = USER_ID,
            title = title,
            memo = memo,
            plannedDate = plannedDate,
            sourceCourseId = sourceCourseId,
            places = places,
        )

    private fun place(
        placeId: Long,
        orderNo: Int,
        memo: String? = null,
    ) = PlanPlace(placeId = placeId, orderNo = orderNo, memo = memo)

    private companion object {
        const val USER_ID = 1L
    }
}
