package com.example.backend.course.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.common.domain.CourseVisibility
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.course.application.event.CourseAuthorWithdrawnEvent
import com.example.backend.course.application.event.CourseDeletedEvent
import com.example.backend.course.application.event.CourseSavedEvent
import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.inbound.dto.DuplicateCourseCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommand
import com.example.backend.course.application.port.inbound.dto.toCourse
import com.example.backend.course.application.port.inbound.dto.toCoursePlaces
import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceImageRow
import com.example.backend.course.application.port.outbound.PlaceLookupPort
import com.example.backend.course.application.port.outbound.PlaceRef
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CoursePlace
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 쓰기(커맨드) 유스케이스 — 생성·편집·삭제. 조회는 [CourseQueryService] 가 담당한다(커맨드/쿼리 분리).
 *
 * 생성/편집: 발행·임시저장 공통. 불변식 검증과 카테고리 도출은 [Course] 애그리거트가 수행하고
 * 서비스는 카테고리 도출에 필요한 place 카테고리(아웃바운드 [PlaceLookupPort], ACL)만 조회해 넘긴다.
 * 한 비즈니스 로직 = 이벤트 하나: 생성·편집은 [CourseSavedEvent], 삭제는 [CourseDeletedEvent] 만 발행한다.
 * 검색 색인과 작성자 코스 개수(user 도메인 ACL) 는 커밋 후 이 이벤트를 각자 소비한다 — 개수에 필요한
 * 이전 공개범위는 서비스가 전달하고 작성자·새 공개범위는 이벤트가 저장된 Course 에서 도출한다.
 */
@Service
@Transactional
class CourseService(
    private val coursePersistencePort: CoursePersistencePort,
    private val courseQueryUseCase: CourseQueryUseCase,
    private val placeLookupPort: PlaceLookupPort,
    private val areaQueryUseCase: AreaQueryUseCase,
    private val eventPublisher: ApplicationEventPublisher,
) : CourseUseCase {
    /** 코스 생성(발행·임시저장 공통) — 장소 검증·지역코드 도출 후 저장하고 [CourseSavedEvent] 를 발행한다. */
    override fun create(command: CreateCourseCommand): Course {
        val duplication = command.resolveDuplicationSnapshot()
        val foundPlaces = requirePlacesExist(command.places.map { it.placeId })
        val places = command.places.toCoursePlaces()
        val areaCode =
            Course.deriveAreaCode(
                isPublished = command.isPublished,
                places = places,
                placeAreaCodeByPlaceId = foundPlaces.associate { it.id to it.areaCode },
            )
        val savedCourse =
            coursePersistencePort.save(
                command.toCourse(
                    places,
                    foundPlaces,
                    areaCode,
                    resolveAreaName(areaCode),
                    originalPlaceCount = duplication?.originalPlaceCount,
                    sharedPlaceCount = duplication?.sharedPlaceCount,
                ),
            )

        // 커밋 후(AFTER_COMMIT) 검색 색인 + 작성자 코스 개수 반영. 발행 코스만 개수에 잡힌다(임시저장 제외 → new=null).
        eventPublisher.publishEvent(
            CourseSavedEvent(
                newCourse = savedCourse,
                oldVisibility = null,
            ),
        )
        return savedCourse
    }

    /** 코스 편집(전체 치환) — 소유·발행 불변식 검증 후 저장하고 [CourseSavedEvent] 를 발행한다. */
    override fun edit(command: EditCourseCommand): Course {
        val existingCourse = requireOwnedCourse(command.courseId, command.userId)

        val newPlaces = command.places.toCoursePlaces()
        Course.ensurePublishedPlacesUnchanged(
            wasPublished = existingCourse.isPublished,
            storedPlaces =
                coursePersistencePort.findPlaces(command.courseId).map {
                    CoursePlace(
                        it.placeId,
                        it.orderNo,
                        it.caption,
                        it.images.map(CoursePlaceImageRow::imageUrl),
                        it.walkingMinutes,
                    )
                },
            newPlaces = newPlaces,
        )
        val foundPlaces = requirePlacesExist(newPlaces.map { it.placeId })
        val areaCode =
            Course.editAreaCode(
                isPublished = command.isPublished,
                wasPublished = existingCourse.isPublished,
                existingAreaCode = existingCourse.areaCode,
                places = newPlaces,
                placeAreaCodeByPlaceId = foundPlaces.associate { it.id to it.areaCode },
            )

        return updateCourse(
            command.toCourse(existingCourse, newPlaces, foundPlaces, areaCode, resolveAreaName(areaCode)),
            removed = Course.countedVisibility(existingCourse.isPublished, existingCourse.visibility),
        )
    }

    /** 일반 생성의 원본 참조 경로와 동일한 접근·장소 검증 및 스냅샷 저장을 적용한다. */
    override fun duplicate(command: DuplicateCourseCommand): Course = create(command.toCreateCommand())

    /** 코스 소프트 삭제. */
    override fun delete(
        userId: Long,
        courseId: Long,
    ) {
        val existingCourse = requireOwnedCourse(courseId, userId)

        // 동시 삭제 레이스 방지: softDelete 는 deleted_at IS NULL 조건이라 실제 갱신 행이 0이면(이미 다른 요청이 삭제)
        // 이벤트를 발행하지 않는다 — 안 그러면 두 요청이 각기 다른 eventId 로 발행해 작성자 카운터가 두 번 감소한다.
        if (coursePersistencePort.softDelete(courseId) == 0) return
        // 커밋 후(AFTER_COMMIT) 검색 문서 삭제 + 작성자 코스 개수 감소. 발행 코스였다면 해당 공개범위 버킷 −1
        // (임시저장은 애초에 안 잡혀 있었다 → oldVisibility=null).
        eventPublisher.publishEvent(
            CourseDeletedEvent(
                courseId = courseId,
                authorId = userId,
                oldVisibility = Course.countedVisibility(existingCourse.isPublished, existingCourse.visibility),
            ),
        )
    }

    /** 편집한 코스를 저장하고 발행 상태·공개범위 전이를 담은 [CourseSavedEvent] 를 발행한다. */
    private fun updateCourse(
        course: Course,
        removed: CourseVisibility?,
    ): Course {
        val updatedCourse = coursePersistencePort.update(course)
        // 편집은 ACTIVE 코스만 통과한다(위 가드). 발행 상태·공개범위 전이(초안→발행, 발행→초안, 공개범위 변경, 변화 없음)를
        // 이벤트에 실어 보내면 user 도메인이 버킷 델타를 계산한다. 검색 색인도 같은 이벤트를 소비한다(커밋 후).
        // 새 상태는 요청이 아니라 저장 결과([updatedCourse])에서 도출한다.
        eventPublisher.publishEvent(
            CourseSavedEvent(
                newCourse = updatedCourse,
                oldVisibility = removed,
            ),
        )
        return updatedCourse
    }

    /** 원본과 겹치는 서로 다른 장소 수를 검증하고 복제 당시 개수를 계산한다. */
    private fun CreateCourseCommand.resolveDuplicationSnapshot(): DuplicationSnapshot? {
        val originCourseId = duplicatedFromId ?: return null
        val originCourse =
            courseQueryUseCase
                .getDetails(listOf(originCourseId), userId)
                .firstOrNull()
                ?: throw BusinessException(
                    CourseErrorCode.COURSE_NOT_FOUND,
                    "원본 코스를 찾을 수 없습니다: id=$originCourseId",
                )
        val originIds = originCourse.places.map(CoursePlaceResult::placeId).toSet()
        val duplicatedIds = places.map { it.placeId }.toSet()
        val sharedCount = originIds.count { it in duplicatedIds }
        if (sharedCount < Course.MIN_SHARED_PLACES) {
            throw BusinessException(
                CourseErrorCode.DUPLICATE_PLACES_NOT_KEPT,
                "원본 장소 ${originIds.size}곳 중 ${Course.MIN_SHARED_PLACES}곳 이상을 그대로 담아야 합니다(현재 ${sharedCount}곳).",
            )
        }
        return DuplicationSnapshot(originIds.size, sharedCount)
    }

    private data class DuplicationSnapshot(
        val originalPlaceCount: Int,
        val sharedPlaceCount: Int,
    )

    override fun deleteAllByAuthor(authorId: Long) {
        // 회원 탈퇴 정리 — 작성자의 살아있는 코스를 전부 소프트 삭제한다.
        // 작성자 공개범위별 카운터는 user 도메인이 탈퇴 시 users 행과 함께 0으로 리셋하므로 여기선 코스 행만 정리한다.
        coursePersistencePort.softDeleteAllByAuthor(authorId)
        eventPublisher.publishEvent(CourseAuthorWithdrawnEvent(authorId)) // 커밋 후 검색 색인(이벤트 — AFTER_COMMIT 리스너)
    }

    /** 법정동코드로 지역 표시명(shortName)을 조회한다. 코드가 null 이면 null. */
    private fun resolveAreaName(areaCode: String?): String? =
        areaCode?.let {
            areaQueryUseCase.findAreaByCode(it)?.shortName
        }

    /** 요청한 장소가 모두 존재하는지 확인하고 [PlaceRef] 목록으로 반환한다(하나라도 없으면 예외). */
    private fun requirePlacesExist(placeIds: List<Long>): List<PlaceRef> {
        val requestedIds = placeIds.distinct()
        val foundPlaces = placeLookupPort.findPlacesByIds(requestedIds)
        if (foundPlaces.size != requestedIds.size) {
            throw BusinessException(PlaceErrorCode.PLACE_NOT_FOUND)
        }
        return foundPlaces
    }

    /** 코스를 조회하고 편집·삭제 접근 정책([Course.ensureModifiable])을 통과시킨 뒤 상세 행을 반환한다. */
    private fun requireOwnedCourse(
        courseId: Long,
        userId: Long,
    ): CourseDetailRow {
        val existingCourse =
            coursePersistencePort.findCourseDetail(courseId)
                ?: throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND)
        Course.ensureModifiable(existingCourse.status, existingCourse.userId, userId)
        return existingCourse
    }
}
