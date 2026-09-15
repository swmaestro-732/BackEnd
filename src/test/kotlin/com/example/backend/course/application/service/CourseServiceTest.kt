package com.example.backend.course.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.area.application.port.inbound.dto.AreaDescriptor
import com.example.backend.area.domain.model.AreaLevel
import com.example.backend.common.domain.CourseVisibility
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.course.application.event.CourseAuthorWithdrawnEvent
import com.example.backend.course.application.event.CourseDeletedEvent
import com.example.backend.course.application.event.CourseSavedEvent
import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.inbound.dto.CreateCoursePlaceCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommand
import com.example.backend.course.application.port.inbound.dto.ForkCourseCommand
import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceImageRow
import com.example.backend.course.application.port.outbound.CoursePlaceRow
import com.example.backend.course.application.port.outbound.PlaceLookupPort
import com.example.backend.course.application.port.outbound.PlaceRef
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CoursePlace
import com.example.backend.course.domain.model.CourseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
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

    // 카운트는 이제 별도 포트가 아니라 발행 이벤트로 전달된다 → 발행된 이벤트를 기록해 검증한다.
    private val publishedEvents = mutableListOf<Any>()
    private val events = ApplicationEventPublisher { publishedEvents.add(it) }
    private val service = CourseService(persistence, query, places, areas, events)

    @Test
    fun `발행 코스 생성 시 카테고리 지역을 도출하고 공개범위 전이를 이벤트로 알린다`() {
        stubPlaces()
        `when`(areas.findAreaByCode(AREA_CODE)).thenReturn(area("성수동1가"))
        `when`(persistence.save(anyValue())).thenAnswer { it.arguments[0] as Course }

        val result = service.create(createCommand(isPublished = true))

        assertEquals(CourseCategory.CAFETOUR, result.category)
        assertEquals(AREA_CODE, result.areaCode)
        assertEquals("성수동1가", result.area)
        val saved = publishedEvents.filterIsInstance<CourseSavedEvent>().single()
        assertEquals(1L, saved.authorId)
        assertNull(saved.oldVisibility)
        assertEquals(CourseVisibility.PUBLIC, saved.newVisibility)
    }

    @Test
    fun `임시저장 생성 시 지역을 조회하지 않고 파생값을 비운다`() {
        stubPlaces()
        `when`(persistence.save(anyValue())).thenAnswer { it.arguments[0] as Course }

        val result = service.create(createCommand(isPublished = false))

        assertNull(result.category)
        assertNull(result.areaCode)
        assertNull(result.area)
        verifyNoInteractions(areas)
        // 임시저장은 카운트 대상이 아니다 → 발행 이벤트의 newVisibility 는 null.
        val saved = publishedEvents.filterIsInstance<CourseSavedEvent>().single()
        assertNull(saved.newVisibility)
    }

    @Test
    fun `생성 이벤트의 작성자와 공개범위는 요청이 아니라 저장 결과를 따른다`() {
        stubPlaces()
        val savedCourse =
            Course.create(
                userId = 7L,
                title = "저장된 코스",
                description = null,
                coverImageUrl = "cover",
                visibility = CourseVisibility.PRIVATE,
                isPublished = true,
                forkedFromId = null,
                tags = emptyList(),
                places = listOf(CoursePlace(1L, 0, null, listOf("a")), CoursePlace(2L, 1, null, listOf("b"))),
                placeCategoryByPlaceId = emptyMap(),
                areaCode = null,
                area = null,
            )
        `when`(persistence.save(anyValue())).thenReturn(savedCourse)

        service.create(createCommand(isPublished = false))

        val event = publishedEvents.filterIsInstance<CourseSavedEvent>().single()
        assertEquals(savedCourse, event.newCourse)
        assertEquals(7L, event.authorId)
        assertNull(event.oldVisibility)
        assertEquals(CourseVisibility.PRIVATE, event.newVisibility)
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
        // PUBLIC → PUBLIC 은 버킷 변화 없음(전이만 전달, 델타 계산은 user 도메인 몫).
        val saved = publishedEvents.filterIsInstance<CourseSavedEvent>().single()
        assertEquals(CourseVisibility.PUBLIC, saved.oldVisibility)
        assertEquals(CourseVisibility.PUBLIC, saved.newVisibility)
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
    fun `발행 코스를 삭제하면 삭제 이벤트로 공개범위 감소를 알린다`() {
        `when`(persistence.findCourseDetail(10L)).thenReturn(detail(isPublished = true))
        `when`(persistence.softDelete(10L)).thenReturn(1) // 실제 1행 삭제됨(동시삭제 아님)

        service.delete(1L, 10L)

        verify(persistence).softDelete(10L)
        val deleted = publishedEvents.filterIsInstance<CourseDeletedEvent>().single()
        assertEquals(1L, deleted.authorId)
        assertEquals(CourseVisibility.PUBLIC, deleted.oldVisibility)
    }

    @Test
    fun `softDelete 가 0 을 반환하면(동시 삭제) 이벤트를 발행하지 않는다`() {
        `when`(persistence.findCourseDetail(10L)).thenReturn(detail(isPublished = true))
        `when`(persistence.softDelete(10L)).thenReturn(0) // 이미 다른 요청이 삭제함

        service.delete(1L, 10L)

        verify(persistence).softDelete(10L)
        assert(publishedEvents.filterIsInstance<CourseDeletedEvent>().isEmpty())
    }

    @Test
    fun `임시저장 코스를 발행으로 수정하면 oldVisibility=null, newVisibility=PUBLIC 이벤트를 발행한다`() {
        stubEdit(detail(isPublished = false, areaCode = null, category = null))
        stubPlaces()
        `when`(areas.findAreaByCode(AREA_CODE)).thenReturn(area("성수동1가"))
        `when`(persistence.update(anyValue())).thenAnswer { it.arguments[0] as Course }

        service.edit(editCommand(isPublished = true))

        val saved = publishedEvents.filterIsInstance<CourseSavedEvent>().single()
        assertNull(saved.oldVisibility) // 임시저장은 카운트 안 됨
        assertEquals(CourseVisibility.PUBLIC, saved.newVisibility)
    }

    @Test
    fun `발행 코스를 임시저장으로 수정하면 oldVisibility=PUBLIC, newVisibility=null 이벤트를 발행한다`() {
        stubEdit(detail(isPublished = true))
        stubPlaces()
        `when`(areas.findAreaByCode(AREA_CODE)).thenReturn(area("성수동1가"))
        `when`(persistence.update(anyValue())).thenAnswer { it.arguments[0] as Course }

        service.edit(editCommand(isPublished = false))

        val saved = publishedEvents.filterIsInstance<CourseSavedEvent>().single()
        assertEquals(CourseVisibility.PUBLIC, saved.oldVisibility)
        assertNull(saved.newVisibility) // 임시저장으로 전환 → 카운트 대상 아님
    }

    @Test
    fun `deleteAllByAuthor 는 소프트 삭제 후 탈퇴 이벤트를 발행한다`() {
        service.deleteAllByAuthor(7L)

        verify(persistence).softDeleteAllByAuthor(7L)
        val event = publishedEvents.filterIsInstance<CourseAuthorWithdrawnEvent>().single()
        assertEquals(7L, event.authorId)
    }

    @Test
    fun `포크 원본이 존재하지 않으면 예외를 던진다`() {
        `when`(persistence.existsById(99L)).thenReturn(false)
        stubPlaces()

        val exception =
            assertThrows(BusinessException::class.java) {
                service.create(createCommand(isPublished = true, forkedFromId = 99L))
            }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, exception.errorCode)
        verify(persistence, never()).save(anyValue())
    }

    @Test
    fun `포크 시 원본 코스가 없으면 예외를 던진다`() {
        `when`(query.getDetails(listOf(50L), 1L)).thenReturn(emptyList())

        val exception =
            assertThrows(BusinessException::class.java) {
                service.fork(forkCommand())
            }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `포크 시 원본 장소를 충분히 유지하지 않으면 예외를 던진다`() {
        `when`(query.getDetails(listOf(50L), 1L)).thenReturn(listOf(originDetail(listOf(1L, 2L))))
        // 원본 2곳 → 전부 유지해야 함, 1곳만 제공
        val fewPlaces = listOf(CreateCoursePlaceCommand(1L, 0, null, listOf("a"), null))

        val exception =
            assertThrows(BusinessException::class.java) {
                service.fork(forkCommand(places = fewPlaces))
            }

        assertEquals(CourseErrorCode.FORK_PLACES_NOT_KEPT, exception.errorCode)
    }

    @Test
    fun `포크 성공 시 CourseSavedEvent 를 발행한다`() {
        `when`(query.getDetails(listOf(50L), 1L)).thenReturn(listOf(originDetail(listOf(1L, 2L))))
        `when`(persistence.existsById(50L)).thenReturn(true) // toCreateCommand 의 requireForkOriginExists
        stubPlaces()
        `when`(areas.findAreaByCode(AREA_CODE)).thenReturn(area("성수동1가"))
        `when`(persistence.save(anyValue())).thenAnswer { it.arguments[0] as Course }

        service.fork(forkCommand())

        val saved = publishedEvents.filterIsInstance<CourseSavedEvent>().single()
        assertEquals(CourseVisibility.PUBLIC, saved.newVisibility)
    }

    private fun forkCommand(places: List<CreateCoursePlaceCommand> = commandPlaces()) =
        ForkCourseCommand(
            userId = 1L,
            forkedFromId = 50L,
            title = "포크 코스",
            description = null,
            coverImageUrl = "cover",
            tags = emptyList(),
            visibility = CourseVisibility.PUBLIC,
            isPublished = true,
            places = places,
        )

    private fun originDetail(placeIds: List<Long>) =
        CourseDetailResult(
            id = 50L,
            title = "원본 코스",
            coverImageUrl = "cover",
            theme = null,
            area = null,
            tags = emptyList(),
            description = "",
            visibility = CourseVisibility.PUBLIC,
            authorId = 2L,
            tracingsCnt = 0,
            places =
                placeIds.mapIndexed { idx, pid ->
                    CoursePlaceResult(idx.toLong(), pid, idx, null, null, emptyList())
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

    private fun createCommand(
        isPublished: Boolean,
        forkedFromId: Long? = null,
    ) = CreateCourseCommand(
        1L,
        "성수 코스",
        null,
        if (isPublished) "cover" else null,
        emptyList(),
        CourseVisibility.PUBLIC,
        isPublished,
        forkedFromId,
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
