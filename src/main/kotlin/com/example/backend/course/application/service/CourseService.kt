package com.example.backend.course.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
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
        val foundPlaces = requirePlacesExist(command.places.map { it.placeId })
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
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND)
        }
    }

    private fun CreateCourseCommand.toCourse(foundPlaces: List<PlaceRef>): Course =
        Course.create(
            userId = userId,
            title = title,
            description = description,
            coverImageUrl = coverImageUrl,
            visibility = visibility,
            isPublished = isPublished,
            forkedFromId = forkedFromId,
            tags = tags,
            places = places.toCoursePlaces(),
            placeCategoryByPlaceId = foundPlaces.associate { it.id to it.category },
            placeAreaByPlaceId = foundPlaces.associate { it.id to it.areaCode },
            resolveAreaName = ::resolveAreaName,
        )

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
        val existing =
            coursePersistencePort.findCourseDetail(command.courseId)
                ?: throw BusinessException(ErrorCode.COURSE_NOT_FOUND)
        if (existing.status != CourseStatus.ACTIVE || existing.userId != command.userId) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND)
        }

        val newPlaces = command.places.toCoursePlaces()
        if (existing.isPublished && 장소구성변경여부확인(coursePersistencePort.findPlaces(command.courseId), newPlaces)) {
            throw BusinessException(
                ErrorCode.PUBLISHED_COURSE_PLACES_IMMUTABLE,
            )
        }
        val foundPlaces = requirePlacesExist(newPlaces.map { it.placeId })

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
    ): Course =
        Course.edit(
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
            existingAreaCode = existing.areaCode,
            existingArea = existing.area,
            placeCategoryByPlaceId = foundPlaces.associate { it.id to it.category },
            placeAreaByPlaceId = foundPlaces.associate { it.id to it.areaCode },
            resolveAreaName = ::resolveAreaName,
        )

    /**
     * 코스 포크. 원본은 **장소 구성(어디를 어떤 순서로)만** 물려주고, 그 위의 콘텐츠(장소별 캡션·사진,
     * 제목·설명·커버·태그·공개 설정)는 포크하는 사람이 새로 입력한 값이라 저장은 생성 경로를 그대로 탄다 —
     * 포크라는 사실은 courses.forked_from_id 로만 남는다(출처 표시).
     *
     * 포크 전에 두 가지를 추가로 검증한다.
     * 1. **원본을 볼 수 있는지** — 조회와 같은 규칙([CourseQueryUseCase])이라 볼 수 없는 코스
     *    (없음·삭제·비활성·PRIVATE 타인·FOLLOWER 비팔로워)는 존재를 드러내지 않도록 404 로 막는다.
     *    자기 코스 포크는 막지 않는다(볼 수 있으므로 통과) — 같은 코스를 다시 기록하는 것도 유효한 사용이다.
     * 2. **원본 장소를 충분히 담았는지**([requireOriginPlacesKept]) — 포크가 원본과 다른 코스가 되는 것을 막는다.
     */
    override fun fork(command: ForkCourseCommand): Course {
        // 상세와 같은 배치 조회 경로를 쓴다(해시태그를 읽지 않아 단건 조회보다 쿼리가 하나 적다).
        // 원본 장소는 이 결과에 함께 실려 오므로 유지 검증을 위해 따로 조회하지 않는다.
        val origin =
            courseQueryUseCase
                .getDetails(listOf(command.forkedFromId), command.userId)
                .firstOrNull()
                ?: throw BusinessException(
                    ErrorCode.COURSE_NOT_FOUND,
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
                ErrorCode.FORK_PLACES_NOT_KEPT,
                "원본 장소 ${originIds.size}곳 중 ${required}곳 이상을 그대로 담아야 합니다(현재 ${kept}곳).",
            )
        }
    }

    /**
     * 코스 소프트 삭제. 존재·소유권을 검증한 뒤 deleted_at 스탬프만 찍는다(전체 치환·애그리거트 재구성 없음).
     * 자식(장소·이미지·태그)은 그대로 두며, 모든 조회가 courses.deleted_at 로 걸러 도달 불가하다.
     */
    override fun delete(
        userId: Long,
        courseId: Long,
    ) {
        // 존재·소유권 검증 — 없거나(삭제 포함)·비활성·타인 소유면 존재를 드러내지 않도록 404(COURSE_NOT_FOUND).
        val existing =
            coursePersistencePort.findCourseDetail(courseId)
                ?: throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")
        if (existing.status != CourseStatus.ACTIVE || existing.userId != userId) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")
        }

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

    /** 지역코드(법정동코드)를 표시 이름으로 푼다 — 동 레벨은 동 이름("성수동1가"), 시군구 레벨은 시군구 이름("강남구"). */
    private fun resolveAreaName(areaCode: String?): String? =
        areaCode?.let { areaQueryUseCase.findAreaByCode(it)?.shortName }

    /**
     * 발행 코스가 참조하는 place_id 가 모두 실제로 존재하는지 검증하고, 조회한 장소 요약을 돌려준다.
     */
    private fun requirePlacesExist(placeIds: List<Long>): List<PlaceRef> {
        val requestedIds = placeIds.distinct()
        val found = placeLookupPort.findPlacesByIds(requestedIds)
        val missing = requestedIds.filterNot { id -> found.any { it.id == id } }
        if (missing.isNotEmpty()) {
            throw BusinessException(ErrorCode.PLACE_NOT_FOUND, "존재하지 않는 장소가 포함되어 있습니다: ids=$missing")
        }
        return found
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
