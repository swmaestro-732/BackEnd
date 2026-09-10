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
import com.example.backend.course.domain.model.CoursePlace
import com.example.backend.course.domain.model.CourseStatus
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 쓰기(커맨드) 유스케이스 — 생성·편집·삭제. 조회는 [CourseQueryService] 가 담당한다(커맨드/쿼리 분리).
 *
 * 생성/편집: 발행·임시저장 공통. 불변식 검증과 카테고리 도출은 [Course] 애그리거트가 수행하고,
 * 서비스는 카테고리 도출에 필요한 place 카테고리(아웃바운드 [PlaceLookupPort], ACL)만 조회해 넘긴다.
 * 한 비즈니스 로직 = 이벤트 하나: 생성·편집은 [CourseSavedEvent], 삭제는 [CourseDeletedEvent] 만 발행한다.
 * 검색 색인과 작성자 코스 개수(user 도메인 ACL) 는 커밋 후 이 이벤트를 각자 소비한다 — 개수에 필요한
 * 이전 공개범위는 서비스가 전달하고, 작성자·새 공개범위는 이벤트가 저장된 Course 에서 도출한다.
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
    override fun create(command: CreateCourseCommand): Course {
        command.requireForkOriginExists()
        val foundPlaces = requirePlacesExist(command.places.map { it.placeId })
        val saved = coursePersistencePort.save(command.toCourse(foundPlaces))

        // 커밋 후(AFTER_COMMIT) 검색 색인 + 작성자 코스 개수 반영. 발행 코스만 개수에 잡힌다(임시저장 제외 → new=null).
        eventPublisher.publishEvent(
            CourseSavedEvent(
                newCourse = saved,
                oldVisibility = null,
            ),
        )
        return saved
    }

    private fun CreateCourseCommand.requireForkOriginExists() {
        val originId = forkedFromId ?: return
        if (!coursePersistencePort.existsById(originId)) {
            throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND)
        }
    }

    private fun CreateCourseCommand.toCourse(foundPlaces: List<PlaceRef>): Course {
        val coursePlaces = places.toCoursePlaces()
        val areaCode =
            Course.deriveAreaCode(
                isPublished = isPublished,
                places = coursePlaces,
                placeAreaCodeByPlaceId = foundPlaces.associate { it.id to it.areaCode },
            )

        return Course.create(
            userId = userId,
            title = title,
            description = description,
            coverImageUrl = coverImageUrl,
            visibility = visibility,
            isPublished = isPublished,
            forkedFromId = forkedFromId,
            tags = tags,
            places = coursePlaces,
            placeCategoryByPlaceId = foundPlaces.associate { it.id to it.category },
            areaCode = areaCode,
            area = resolveAreaName(areaCode),
        )
    }

    private fun List<CreateCoursePlaceCommand>.toCoursePlaces(): List<CoursePlace> =
        map {
            CoursePlace(
                placeId = it.placeId,
                orderNo = it.orderNo,
                caption = it.caption,
                imageUrls = it.imageUrls,
                walkingMinutes = it.walkingMinutes,
            )
        }

    override fun edit(command: EditCourseCommand): Course {
        val existing = requireOwnedCourse(command.courseId, command.userId)

        val newPlaces = command.places.toCoursePlaces()
        if (existing.isPublished &&
            placesStructureChanged(coursePersistencePort.findPlaces(command.courseId), newPlaces)
        ) {
            throw BusinessException(
                CourseErrorCode.PUBLISHED_COURSE_PLACES_IMMUTABLE,
            )
        }
        val foundPlaces = requirePlacesExist(newPlaces.map { it.placeId })

        return updateCourse(
            command.toCourse(existing, newPlaces, foundPlaces),
            removed = Course.countedVisibility(existing.isPublished, existing.visibility),
        )
    }

    private fun updateCourse(
        course: Course,
        removed: CourseVisibility?,
    ): Course {
        val updated = coursePersistencePort.update(course)
        // 편집은 ACTIVE 코스만 통과한다(위 가드). 발행 상태·공개범위 전이(초안→발행, 발행→초안, 공개범위 변경, 변화 없음)를
        // 이벤트에 실어 보내면 user 도메인이 버킷 델타를 계산한다. 검색 색인도 같은 이벤트를 소비한다(커밋 후).
        // 새 상태는 요청이 아니라 저장 결과([updated])에서 도출한다.
        eventPublisher.publishEvent(
            CourseSavedEvent(
                newCourse = updated,
                oldVisibility = removed,
            ),
        )
        return updated
    }

    private fun EditCourseCommand.toCourse(
        existing: CourseDetailRow,
        places: List<CoursePlace>,
        foundPlaces: List<PlaceRef>,
    ): Course {
        val areaCode =
            when {
                !isPublished -> {
                    null
                }

                existing.isPublished && existing.areaCode != null -> {
                    existing.areaCode
                }

                else -> {
                    Course.deriveAreaCode(
                        isPublished = true,
                        places = places,
                        placeAreaCodeByPlaceId = foundPlaces.associate { it.id to it.areaCode },
                    )
                }
            }

        return Course.edit(
            id = courseId,
            userId = userId,
            title = title,
            description = description,
            coverImageUrl = coverImageUrl,
            visibility = visibility,
            isPublished = isPublished,
            tags = tags,
            places = places,
            wasPublished = existing.isPublished,
            existingCategory = existing.category,
            placeCategoryByPlaceId = foundPlaces.associate { it.id to it.category },
            areaCode = areaCode,
            area = resolveAreaName(areaCode),
        )
    }

    override fun fork(command: ForkCourseCommand): Course {
        // 상세와 같은 배치 조회 경로를 쓴다(해시태그를 읽지 않아 단건 조회보다 쿼리가 하나 적다).
        // 원본 장소는 이 결과에 함께 실려 오므로 유지 검증을 위해 따로 조회하지 않는다.
        val origin =
            courseQueryUseCase
                .getDetails(listOf(command.forkedFromId), command.userId)
                .firstOrNull()
                ?: throw BusinessException(
                    CourseErrorCode.COURSE_NOT_FOUND,
                    "원본 코스를 찾을 수 없습니다: id=${command.forkedFromId}",
                )

        requireOriginPlacesKept(origin.places.map(CoursePlaceResult::placeId), command.places)
        return create(command.toCreateCommand())
    }

    private fun requireOriginPlacesKept(
        originPlaceIds: List<Long>,
        forkedPlaces: List<CreateCoursePlaceCommand>,
    ) {
        val originIds = originPlaceIds.distinct()
        val required = Course.requiredKeptPlaceCount(originIds.size)
        val forkedIds = forkedPlaces.map { it.placeId }.toSet()
        val kept = originIds.count { it in forkedIds }
        if (kept < required) {
            throw BusinessException(
                CourseErrorCode.FORK_PLACES_NOT_KEPT,
                "원본 장소 ${originIds.size}곳 중 ${required}곳 이상을 그대로 담아야 합니다(현재 ${kept}곳).",
            )
        }
    }

    /** 코스 소프트 삭제. */
    override fun delete(
        userId: Long,
        courseId: Long,
    ) {
        val existing = requireOwnedCourse(courseId, userId)

        // 동시 삭제 레이스 방지: softDelete 는 deleted_at IS NULL 조건이라 실제 갱신 행이 0이면(이미 다른 요청이 삭제)
        // 이벤트를 발행하지 않는다 — 안 그러면 두 요청이 각기 다른 eventId 로 발행해 작성자 카운터가 두 번 감소한다.
        if (coursePersistencePort.softDelete(courseId) == 0) return
        // 커밋 후(AFTER_COMMIT) 검색 문서 삭제 + 작성자 코스 개수 감소. 발행 코스였다면 해당 공개범위 버킷 −1
        // (임시저장은 애초에 안 잡혀 있었다 → oldVisibility=null).
        eventPublisher.publishEvent(
            CourseDeletedEvent(
                courseId = courseId,
                authorId = userId,
                oldVisibility = Course.countedVisibility(existing.isPublished, existing.visibility),
            ),
        )
    }

    override fun deleteAllByAuthor(authorId: Long) {
        // 회원 탈퇴 정리 — 작성자의 살아있는 코스를 전부 소프트 삭제한다.
        // 작성자 공개범위별 카운터는 user 도메인이 탈퇴 시 users 행과 함께 0으로 리셋하므로 여기선 코스 행만 정리한다.
        coursePersistencePort.softDeleteAllByAuthor(authorId)
        eventPublisher.publishEvent(CourseAuthorWithdrawnEvent(authorId)) // 커밋 후 검색 색인(이벤트 — AFTER_COMMIT 리스너)
    }

    private fun resolveAreaName(areaCode: String?): String? =
        areaCode?.let {
            areaQueryUseCase.findAreaByCode(it)?.shortName
        }

    private fun requirePlacesExist(placeIds: List<Long>): List<PlaceRef> {
        val requestedIds = placeIds.distinct()
        val found = placeLookupPort.findPlacesByIds(requestedIds)
        if (found.size != requestedIds.size) {
            throw BusinessException(PlaceErrorCode.PLACE_NOT_FOUND)
        }
        return found
    }

    private fun requireOwnedCourse(
        courseId: Long,
        userId: Long,
    ): CourseDetailRow {
        val existing =
            coursePersistencePort.findCourseDetail(courseId)
                ?: throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND)
        if (existing.status != CourseStatus.ACTIVE || existing.userId != userId) {
            throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND)
        }
        return existing
    }

    private fun placesStructureChanged(
        stored: List<CoursePlaceRow>,
        newPlaces: List<CoursePlace>,
    ): Boolean {
        if (stored.size != newPlaces.size) return true
        val storedSignature =
            stored
                .sortedBy { it.orderNo }
                .map { Triple(it.placeId, it.orderNo, it.images.map(CoursePlaceImageRow::imageUrl)) }
        val newSignature =
            newPlaces
                .sortedBy { it.orderNo }
                .map { Triple(it.placeId, it.orderNo, it.imageUrls) }
        return storedSignature != newSignature
    }
}
