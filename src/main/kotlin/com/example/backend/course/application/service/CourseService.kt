package com.example.backend.course.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
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
import com.example.backend.course.application.port.outbound.AuthorCourseCountPort
import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceImageRow
import com.example.backend.course.application.port.outbound.CoursePlaceRow
import com.example.backend.course.application.port.outbound.PlaceLookupPort
import com.example.backend.course.application.port.outbound.PlaceRef
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CoursePlace
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 쓰기(커맨드) 유스케이스 — 생성·편집·삭제. 조회는 [CourseQueryService] 가 담당한다(커맨드/쿼리 분리).
 */
@Service
@Transactional
class CourseService(
    private val coursePersistencePort: CoursePersistencePort,
    private val courseQueryUseCase: CourseQueryUseCase,
    private val placeLookupPort: PlaceLookupPort,
    private val areaQueryUseCase: AreaQueryUseCase,
    private val authorCourseCountPort: AuthorCourseCountPort,
    private val eventPublisher: ApplicationEventPublisher,
) : CourseUseCase {
    override fun 코스생성(command: CreateCourseCommand): Course {
        command.포크원본검증()
        val foundPlaces = 장소존재검증(command.places.map { it.placeId })
        val saved = coursePersistencePort.save(command.toCourse(foundPlaces))

        if (command.isPublished) {
            adjustAuthorCourseCount(command.userId, removed = null, added = command.visibility)
        }
        eventPublisher.publishEvent(CourseSavedEvent(saved))
        return saved
    }

    private fun CreateCourseCommand.포크원본검증() {
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
            area = 지역이름조회(areaCode),
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

    override fun 코스수정(command: EditCourseCommand): Course {
        val existing = 코스존재검증(command.courseId, command.userId)

        val newPlaces = command.places.toCoursePlaces()
        if (existing.isPublished && 장소구성변경여부확인(coursePersistencePort.findPlaces(command.courseId), newPlaces)) {
            throw BusinessException(
                CourseErrorCode.PUBLISHED_COURSE_PLACES_IMMUTABLE,
            )
        }
        val foundPlaces = 장소존재검증(newPlaces.map { it.placeId })

        return 코스갱신(
            command.toCourse(existing, newPlaces, foundPlaces),
            removed = existing.visibility.takeIf { existing.isPublished },
            added = command.visibility.takeIf { command.isPublished },
        )
    }

    private fun 코스갱신(
        course: Course,
        removed: CourseVisibility?,
        added: CourseVisibility?,
    ): Course {
        val updated = coursePersistencePort.update(course)
        adjustAuthorCourseCount(course.userId, removed, added)
        eventPublisher.publishEvent(CourseSavedEvent(updated)) // 커밋 후 검색 색인(AFTER_COMMIT 리스너)
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
            area = 지역이름조회(areaCode),
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
        return 코스생성(command.toCreateCommand())
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
    override fun 코스삭제(
        userId: Long,
        courseId: Long,
    ) {
        val existing = 코스존재검증(courseId, userId)

        coursePersistencePort.softDelete(courseId)
        // 발행 코스였다면 삭제로 해당 공개범위 버킷 −1(임시저장은 애초에 안 잡혀 있었다).
        adjustAuthorCourseCount(
            userId = userId,
            removed = if (existing.isPublished) existing.visibility else null,
            added = null,
        )
        eventPublisher.publishEvent(CourseDeletedEvent(courseId)) // 커밋 후 검색 색인(이벤트 — AFTER_COMMIT 리스너)
    }

    override fun deleteAllByAuthor(authorId: Long) {
        // 회원 탈퇴 정리 — 작성자의 살아있는 코스를 전부 소프트 삭제한다.
        // 작성자 공개범위별 카운터는 user 도메인이 탈퇴 시 users 행과 함께 0으로 리셋하므로 여기선 코스 행만 정리한다.
        coursePersistencePort.softDeleteAllByAuthor(authorId)
        eventPublisher.publishEvent(CourseAuthorWithdrawnEvent(authorId)) // 커밋 후 검색 색인(이벤트 — AFTER_COMMIT 리스너)
    }

    private fun 지역이름조회(areaCode: String?): String? = areaCode?.let { areaQueryUseCase.findAreaByCode(it)?.shortName }

    private fun 장소존재검증(placeIds: List<Long>): List<PlaceRef> {
        val requestedIds = placeIds.distinct()
        val found = placeLookupPort.findPlacesByIds(requestedIds)
        if (found.size != requestedIds.size) {
            throw BusinessException(PlaceErrorCode.PLACE_NOT_FOUND)
        }
        return found
    }

    private fun 코스존재검증(
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

    private fun 장소구성변경여부확인(
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

    /**
     * 코스 상태 변화에 따른 작성자의 공개범위별 코스 개수 델타를 반영한다.
     * [removed]/[added] 는 "카운트되는 상태(발행·활성·미삭제)"의 공개범위이고, 그 상태가 아니면 null.
     * 크로스 도메인 경계라 공개범위 enum 대신 버킷별 원시 int 델타로 넘긴다([AuthorCourseCountPort]).
     */
    private fun adjustAuthorCourseCount(
        userId: Long,
        removed: CourseVisibility?,
        added: CourseVisibility?,
    ) {
        fun delta(v: CourseVisibility) = (if (added == v) 1 else 0) - (if (removed == v) 1 else 0)
        authorCourseCountPort.applyDelta(
            authorId = userId,
            publicDelta = delta(CourseVisibility.PUBLIC),
            followerDelta = delta(CourseVisibility.FOLLOWER),
            privateDelta = delta(CourseVisibility.PRIVATE),
        )
    }
}
