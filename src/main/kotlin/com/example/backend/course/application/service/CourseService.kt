package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceImageResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.inbound.dto.CourseSummary
import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommand
import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceImageRow
import com.example.backend.course.application.port.outbound.CoursePlaceRow
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CoursePlace
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.user.application.port.inbound.CourseInteractionUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 유스케이스 — 상세 조회와 생성을 함께 담당한다.
 *
 * 조회(getDetail): status=ACTIVE·미삭제만 반환(그 외 404), PRIVATE 은 소유자만, FOLLOWER 는 소유자·팔로워만(그 외 404).
 * 생성(create): 발행/임시저장 공통. 불변식 검증과 카테고리 도출은 [Course] 애그리거트가 수행하고,
 * 서비스는 카테고리 도출에 필요한 place 카테고리(place 인바운드 포트)만 조회해 넘긴다.
 *
 * 기본은 읽기 전용 트랜잭션이고, 쓰기(create)만 메서드 레벨에서 재정의한다.
 */
@Service
@Transactional(readOnly = true)
class CourseService(
    private val coursePersistencePort: CoursePersistencePort,
    private val placeQueryUseCase: PlaceQueryUseCase,
    private val courseInteractionUseCase: CourseInteractionUseCase,
) : CourseUseCase,
    CourseQueryUseCase {
    override fun getDetail(
        courseId: Long,
        viewerId: Long?,
    ): CourseDetailResult =
        getDetails(listOf(courseId), viewerId).firstOrNull()
            ?: throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")

    override fun getDetails(
        courseIds: List<Long>,
        viewerId: Long?,
    ): List<CourseDetailResult> {
        if (courseIds.isEmpty()) return emptyList()

        // status=ACTIVE·미삭제·조회 가능(PRIVATE 소유자, FOLLOWER 소유자·팔로워)만 남긴다.
        val viewableById =
            coursePersistencePort
                .findCourseDetails(courseIds)
                .filter { it.status == CourseStatus.ACTIVE && isViewable(it.visibility, it.userId, viewerId) }
                .associateBy { it.id }
        if (viewableById.isEmpty()) return emptyList()

        val viewableIds = viewableById.keys.toList()
        val placesByCourse = coursePersistencePort.findPlacesByCourseIds(viewableIds)
        val viewerByCourse =
            viewerId
                ?.let { courseInteractionUseCase.getViewerStates(it, viewableIds) }
                .orEmpty()
                .associateBy { it.courseId }

        // 입력 순서를 유지해 반환한다(볼 수 없는 코스는 제외됨).
        return courseIds.mapNotNull { viewableById[it] }.map { course ->
            val places =
                placesByCourse[course.id].orEmpty().map { place ->
                    CoursePlaceResult(
                        id = place.id,
                        placeId = place.placeId,
                        orderNo = place.orderNo,
                        caption = place.caption,
                        walkingMinutesToNext = place.walkingMinutes,
                        images = place.images.map { CoursePlaceImageResult(it.imageUrl, it.orderNo) },
                    )
                }
            val viewer = viewerByCourse[course.id]
            CourseDetailResult(
                id = course.id,
                title = course.title,
                coverImageUrl = course.coverImageUrl.orEmpty(),
                theme = course.category?.name,
                area = course.area,
                description = course.description.orEmpty(),
                visibility = course.visibility,
                authorId = course.userId,
                tracingsCnt = course.tracingsCnt,
                places = places,
                hasSaved = viewer?.hasSaved ?: false,
                hasStartedCourse = viewer?.hasStartedCourse ?: false,
            )
        }
    }

    /**
     * 작성자의 발행 코스 요약 목록 — 조회자(viewerId) 기준 공개범위(isViewable) 통과분만 내려준다.
     * 본인 조회(viewerId==authorId)면 발행 코스 전체가 통과한다.
     */
    override fun listByAuthor(
        authorId: Long,
        viewerId: Long?,
    ): List<CourseSummary> =
        coursePersistencePort
            .findPublishedByAuthor(authorId)
            .filter { isViewable(it.visibility, ownerId = it.userId, viewerId = viewerId) }
            .map {
                CourseSummary(
                    id = it.id,
                    authorId = it.userId,
                    title = it.title,
                    coverImageUrl = it.coverImageUrl,
                    theme = it.category?.name,
                    likesCnt = it.likesCnt,
                    savesCnt = it.savesCnt,
                    createdAt = it.createdAt,
                )
            }

    /** 미삭제 코스 존재 확인(크로스 도메인) — 다른 도메인(user 저장함 등)이 이 포트로만 접근한다. */
    override fun existsById(courseId: Long): Boolean = coursePersistencePort.existsById(courseId)

    private fun isViewable(
        visibility: CourseVisibility,
        ownerId: Long,
        viewerId: Long?,
    ): Boolean =
        when (visibility) {
            CourseVisibility.PUBLIC -> {
                true
            }

            CourseVisibility.FOLLOWER -> {
                viewerId == ownerId ||
                    (viewerId != null && courseInteractionUseCase.isFollowing(viewerId, ownerId))
            }

            CourseVisibility.PRIVATE -> {
                viewerId == ownerId
            }
        }

    @Transactional
    override fun create(command: CreateCourseCommand): Course {
        // fork 원본 코스가 실제로 존재하는지 검증(없으면 404).
        command.forkedFromId?.let { forkedFromId ->
            if (!coursePersistencePort.existsById(forkedFromId)) {
                throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "원본 코스를 찾을 수 없습니다: id=$forkedFromId")
            }
        }

        // 참조 place 존재 검증(발행·임시저장 공통 — place_id 는 FK 가 없어 여기서만 걸러진다).
        val foundPlaces = requirePlacesExist(command.places.map { it.placeId })
        // 불변식 검증·카테고리 도출은 Course.create 애그리거트 팩토리가 수행한다.
        // 카테고리 도출은 발행 코스만 필요 — 검증차 조회한 place 요약을 그대로 재사용한다.
        val placeCategories =
            if (command.isPublished) {
                foundPlaces.associate { it.id to it.category }
            } else {
                emptyMap()
            }
        val course =
            Course.create(
                userId = command.userId,
                title = command.title,
                description = command.description,
                coverImageUrl = command.coverImageUrl,
                visibility = command.visibility,
                isPublished = command.isPublished,
                forkedFromId = command.forkedFromId,
                tags = command.tags,
                places =
                    command.places.map {
                        CoursePlace(
                            placeId = it.placeId,
                            orderNo = it.orderNo,
                            caption = it.caption,
                            imageUrls = it.imageUrls,
                        )
                    },
                placeCategoryByPlaceId = placeCategories,
            )
        return coursePersistencePort.save(course)
    }

    @Transactional
    override fun edit(command: EditCourseCommand): Course {
        // 존재·소유권 검증 — 없거나(삭제 포함)·비활성·타인 소유면 존재를 드러내지 않도록 404(COURSE_NOT_FOUND).
        val existing =
            coursePersistencePort.findCourseDetail(command.courseId)
                ?: throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=${command.courseId}")
        if (existing.status != CourseStatus.ACTIVE || existing.userId != command.userId) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=${command.courseId}")
        }

        val places =
            command.places.map {
                CoursePlace(
                    placeId = it.placeId,
                    orderNo = it.orderNo,
                    caption = it.caption,
                    imageUrls = it.imageUrls,
                )
            }

        // 이미 게시된 코스는 장소 구성(place_id·순서·사진)을 바꿀 수 없고 캡션만 수정할 수 있다.
        // (임시저장→발행 전환은 existing.isPublished 가 false 라 이 제약에서 자유롭다.)
        if (existing.isPublished) {
            val storedPlaces = coursePersistencePort.findPlaces(command.courseId)
            if (placesStructureChanged(storedPlaces, places)) {
                throw BusinessException(
                    ErrorCode.PUBLISHED_COURSE_PLACES_IMMUTABLE,
                    "게시된 코스는 장소를 추가·삭제·교체하거나 순서·사진을 바꿀 수 없습니다: id=${command.courseId}",
                )
            }
        }

        // 참조 place 존재 검증(발행·임시저장 공통 — place_id 는 FK 가 없어 여기서만 걸러진다).
        // 발행 코스의 장소 구성 불변(4003) 검증 뒤에 둬 기존 오류 우선순위를 유지한다.
        val foundPlaces = requirePlacesExist(places.map { it.placeId })

        val course =
            Course.edit(
                id = command.courseId,
                userId = command.userId,
                title = command.title,
                description = command.description,
                coverImageUrl = command.coverImageUrl,
                visibility = command.visibility,
                isPublished = command.isPublished,
                tags = command.tags,
                places = places,
                category = resolveEditedCategory(command, existing, places, foundPlaces),
            )
        return coursePersistencePort.update(course)
    }

    /**
     * 편집 코스의 카테고리를 해석한다. place 존재 검증은 호출부(edit)에서 이미 끝났고, 그 결과([foundPlaces])를 재사용한다.
     * - 임시저장이면 null(생성과 동일).
     * - 발행이면서 **기존 카테고리가 있고 장소 구성이 그대로면** 재도출 없이 기존 값을 유지한다 —
     *   장소를 바꾸지 않은 편집(제목·설명 등)에서 외부 place 데이터 드리프트로 카테고리가 바뀌는 것을 막는다.
     * - 그 외(초안→발행, 장소 변경, 카테고리 미지정 코스)에만 place 카테고리로 도출한다(생성과 동일 규칙).
     */
    private fun resolveEditedCategory(
        command: EditCourseCommand,
        existing: CourseDetailRow,
        places: List<CoursePlace>,
        foundPlaces: List<PlaceSummary>,
    ): CourseCategory? {
        if (!command.isPublished) return null
        if (existing.category != null && !placesChanged(command.courseId, places)) {
            return existing.category
        }
        val placeCategories = foundPlaces.associate { it.id to it.category }
        return Course.deriveCategory(command.isPublished, places, placeCategories)
    }

    /**
     * 발행 코스가 참조하는 place_id 가 모두 실제로 존재하는지 검증하고, 조회한 장소 요약을 돌려준다.
     */
    private fun requirePlacesExist(placeIds: List<Long>): List<PlaceSummary> {
        val requestedIds = placeIds.distinct()
        val found = placeQueryUseCase.findPlacesById(requestedIds)
        val missing = requestedIds.filterNot { id -> found.any { it.id == id } }
        if (missing.isNotEmpty()) {
            throw BusinessException(ErrorCode.PLACE_NOT_FOUND, "존재하지 않는 장소가 포함되어 있습니다: ids=$missing")
        }
        return found
    }

    /**
     * 게시된 코스 편집에서 **캡션을 제외한** 장소 구성이 바뀌었는지 판정한다.
     * place_id·순서(orderNo)·사진(imageUrls)이 저장본과 완전히 같아야 false(변경 없음)다.
     * 저장본 이미지는 orderNo 오름차순으로 조회되므로 imageUrl 나열이 요청 imageUrls 순서와 그대로 대응한다.
     */
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

    /** 저장된 코스의 장소 구성이 요청과 다른지 판정한다 — 카테고리는 orderNo 순 placeId 나열에만 의존한다. */
    private fun placesChanged(
        courseId: Long,
        newPlaces: List<CoursePlace>,
    ): Boolean {
        val existingPlaceIds =
            coursePersistencePort.findPlaces(courseId).sortedBy { it.orderNo }.map { it.placeId }
        val newPlaceIds = newPlaces.sortedBy { it.orderNo }.map { it.placeId }
        return existingPlaceIds != newPlaceIds
    }

    /**
     * 코스 소프트 삭제. 존재·소유권을 검증한 뒤 deleted_at 스탬프만 찍는다(전체 치환·애그리거트 재구성 없음).
     * 자식(장소·이미지·태그)은 그대로 두며, 모든 조회가 courses.deleted_at 로 걸러 도달 불가하다.
     */
    @Transactional
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
    }
}
