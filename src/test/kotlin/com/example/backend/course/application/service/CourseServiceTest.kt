package com.example.backend.course.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.area.application.port.inbound.dto.AreaDescriptor
import com.example.backend.area.domain.model.AreaLevel
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.inbound.dto.CreateCoursePlaceCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommand
import com.example.backend.course.application.port.inbound.dto.ForkCourseCommand
import com.example.backend.course.application.port.outbound.AuthorCourseCountPort
import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceImageRow
import com.example.backend.course.application.port.outbound.CoursePlaceRow
import com.example.backend.course.application.port.outbound.PlaceLookupPort
import com.example.backend.course.application.port.outbound.PlaceRef
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher

class CourseServiceTest {
    private val persistence = mock(CoursePersistencePort::class.java)
    private val query = mock(CourseQueryUseCase::class.java)
    private val places = mock(PlaceLookupPort::class.java)
    private val areas = mock(AreaQueryUseCase::class.java)
    private val counts = mock(AuthorCourseCountPort::class.java)
    private val events = mock(ApplicationEventPublisher::class.java)
    private val service = CourseService(persistence, query, places, areas, counts, events)

    @Test
    fun `발행 코스 생성 시 카테고리 지역을 도출하고 카운터를 증가시킨다`() {
        stubPlaces()
        `when`(areas.findAreaByCode(AREA_CODE)).thenReturn(area("성수동1가"))
        `when`(persistence.save(anyValue())).thenAnswer { it.arguments[0] as Course }

        val result = service.create(createCommand(isPublished = true))

        assertEquals(CourseCategory.CAFETOUR, result.category)
        assertEquals(AREA_CODE, result.areaCode)
        assertEquals("성수동1가", result.area)
        verify(counts).applyDelta(1L, 1, 0, 0)
    }

    @Test
    fun `임시저장 생성 시 지역을 조회하지 않고 파생값을 비운다`() {
        stubPlaces()
        `when`(persistence.save(anyValue())).thenAnswer { it.arguments[0] as Course }

        val result = service.create(createCommand(isPublished = false))

        assertNull(result.category)
        assertNull(result.areaCode)
        assertNull(result.area)
        verifyNoInteractions(areas, counts)
    }

    @Test
    fun `존재하지 않는 장소가 포함되면 저장하지 않는다`() {
        `when`(places.findPlacesByIds(listOf(1L, 2L))).thenReturn(listOf(placeRef(1L)))

        val exception = assertThrows(BusinessException::class.java) { service.create(createCommand(true)) }

        assertEquals(PlaceErrorCode.PLACE_NOT_FOUND, exception.errorCode)
        verify(persistence, never()).save(anyValue())
    }

    @Test
    fun `기존 발행 코스 수정 시 기존 지역 코드를 유지하고 이름은 다시 조회한다`() {
        val existing = detail(isPublished = true, areaCode = AREA_CODE, category = CourseCategory.CAFETOUR)
        stubEdit(existing)
        stubPlaces(areaCode = "1117013100")
        `when`(areas.findAreaByCode(AREA_CODE)).thenReturn(area("성수동1가"))
        `when`(persistence.update(anyValue())).thenAnswer { it.arguments[0] as Course }

        val result = service.edit(editCommand(isPublished = true))

        assertEquals(AREA_CODE, result.areaCode)
        assertEquals("성수동1가", result.area)
        verify(areas).findAreaByCode(AREA_CODE)
        verify(counts).applyDelta(1L, 0, 0, 0)
    }

    @Test
    fun `발행 코스의 누락된 파생값은 수정 시 장소 정보로 복구한다`() {
        stubEdit(detail(isPublished = true, areaCode = null, category = null))
        stubPlaces()
        `when`(areas.findAreaByCode(AREA_CODE)).thenReturn(area("성수동1가"))
        `when`(persistence.update(anyValue())).thenAnswer { it.arguments[0] as Course }

        val result = service.edit(editCommand(isPublished = true))

        assertEquals(CourseCategory.CAFETOUR, result.category)
        assertEquals(AREA_CODE, result.areaCode)
        assertEquals("성수동1가", result.area)
    }

    @Test
    fun `발행 코스의 장소 구성을 바꾸면 수정하지 않는다`() {
        `when`(persistence.findCourseDetail(10L)).thenReturn(detail(isPublished = true))
        `when`(persistence.findPlaces(10L)).thenReturn(storedPlaces().dropLast(1))

        val exception = assertThrows(BusinessException::class.java) { service.edit(editCommand(true)) }

        assertEquals(CourseErrorCode.PUBLISHED_COURSE_PLACES_IMMUTABLE, exception.errorCode)
        verify(persistence, never()).update(anyValue())
    }

    @Test
    fun `발행 코스를 삭제하면 공개 카운터를 감소시킨다`() {
        `when`(persistence.findCourseDetail(10L)).thenReturn(detail(isPublished = true))

        service.delete(1L, 10L)

        verify(persistence).softDelete(10L)
        verify(counts).applyDelta(1L, -1, 0, 0)
    }

    @Test
    fun `fork - 원본 코스가 없으면 COURSE_NOT_FOUND 를 던진다`() {
        `when`(query.getDetails(listOf(10L), 1L)).thenReturn(emptyList())

        val ex = assertThrows(BusinessException::class.java) { service.fork(forkCommand()) }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, ex.errorCode)
        verify(persistence, never()).save(anyValue())
    }

    @Test
    fun `fork - 원본 장소를 충분히 유지하지 않으면 FORK_PLACES_NOT_KEPT 를 던진다`() {
        // 원본에 장소 2개 → requiredKeptPlaceCount(2) = 2, 포크 명령에 전혀 다른 장소만 담음
        `when`(query.getDetails(listOf(10L), 1L)).thenReturn(listOf(detailWithPlaces(listOf(1L, 2L))))

        val ex = assertThrows(BusinessException::class.java) { service.fork(forkCommand(placeIds = listOf(99L, 100L))) }

        assertEquals(CourseErrorCode.FORK_PLACES_NOT_KEPT, ex.errorCode)
    }

    @Test
    fun `fork - 원본 장소를 모두 유지하면 새 코스를 생성한다`() {
        `when`(query.getDetails(listOf(10L), 1L)).thenReturn(listOf(detailWithPlaces(listOf(1L, 2L))))
        `when`(persistence.existsById(10L)).thenReturn(true) // requireForkOriginExists 통과
        stubPlaces()
        `when`(persistence.save(anyValue())).thenAnswer { it.arguments[0] as Course }

        val result = service.fork(forkCommand())

        assertNotNull(result)
        verify(persistence).save(anyValue())
    }

    @Test
    fun `delete - 다른 사용자 코스를 삭제하려 하면 COURSE_NOT_FOUND 를 던진다`() {
        `when`(persistence.findCourseDetail(10L)).thenReturn(detail(isPublished = true)) // userId=1L 소유

        val ex = assertThrows(BusinessException::class.java) { service.delete(userId = 2L, courseId = 10L) }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, ex.errorCode)
        verify(persistence, never()).softDelete(anyLong())
    }

    private fun forkCommand(placeIds: List<Long> = listOf(1L, 2L)): ForkCourseCommand =
        ForkCourseCommand(
            userId = 1L,
            forkedFromId = 10L,
            title = "포크 코스",
            description = null,
            coverImageUrl = null,
            tags = emptyList(),
            visibility = CourseVisibility.PUBLIC,
            isPublished = false, // 이미지 없는 장소로 테스트 — 발행 시 이미지 필수 제약 우회
            places = placeIds.mapIndexed { i, id -> CreateCoursePlaceCommand(id, i, null, emptyList(), null) },
        )

    private fun detailWithPlaces(placeIds: List<Long>): CourseDetailResult =
        CourseDetailResult(
            id = 10L,
            title = "원본 코스",
            coverImageUrl = "cover",
            theme = "CAFETOUR",
            area = "성수동",
            tags = emptyList(),
            description = "",
            visibility = CourseVisibility.PUBLIC,
            authorId = 99L,
            tracingsCnt = 0,
            places =
                placeIds.mapIndexed { i, pid ->
                    CoursePlaceResult(
                        id = i.toLong(),
                        placeId = pid,
                        orderNo = i,
                        caption = null,
                        walkingMinutesToNext = null,
                        images = emptyList(),
                    )
                },
            hasSaved = false,
            hasStartedCourse = false,
        )

    private fun stubPlaces(areaCode: String? = AREA_CODE) {
        `when`(places.findPlacesByIds(listOf(1L, 2L)))
            .thenReturn(listOf(placeRef(1L, areaCode), placeRef(2L, areaCode)))
    }

    private fun stubEdit(existing: CourseDetailRow) {
        `when`(persistence.findCourseDetail(10L)).thenReturn(existing)
        `when`(persistence.findPlaces(10L)).thenReturn(storedPlaces())
    }

    private fun createCommand(isPublished: Boolean) =
        CreateCourseCommand(
            1L,
            "성수 코스",
            null,
            if (isPublished) "cover" else null,
            emptyList(),
            CourseVisibility.PUBLIC,
            isPublished,
            null,
            commandPlaces(),
        )

    private fun editCommand(isPublished: Boolean) =
        EditCourseCommand(
            10L,
            1L,
            "성수 코스",
            null,
            if (isPublished) "cover" else null,
            emptyList(),
            CourseVisibility.PUBLIC,
            isPublished,
            commandPlaces(),
        )

    private fun commandPlaces() =
        listOf(
            CreateCoursePlaceCommand(1L, 0, null, listOf("a"), 5),
            CreateCoursePlaceCommand(2L, 1, null, listOf("b"), null),
        )

    private fun storedPlaces() =
        listOf(
            CoursePlaceRow(1L, 1L, 0, null, 5, listOf(CoursePlaceImageRow("a", 0))),
            CoursePlaceRow(2L, 2L, 1, null, null, listOf(CoursePlaceImageRow("b", 0))),
        )

    private fun detail(
        isPublished: Boolean,
        areaCode: String? = AREA_CODE,
        category: CourseCategory? = CourseCategory.CAFETOUR,
    ) = CourseDetailRow(
        10L,
        1L,
        "기존 코스",
        "cover",
        null,
        category,
        null,
        areaCode,
        0,
        CourseStatus.ACTIVE,
        CourseVisibility.PUBLIC,
        isPublished,
    )

    private fun placeRef(
        id: Long,
        areaCode: String? = AREA_CODE,
    ) = PlaceRef(id, "CAFE", areaCode)

    private fun area(name: String) = AreaDescriptor(AREA_CODE, name, "서울특별시 성동구 $name", AreaLevel.DONG)

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyValue(): T {
        any<T>()
        return null as T
    }

    private companion object {
        const val AREA_CODE = "1120011400"
    }
}
