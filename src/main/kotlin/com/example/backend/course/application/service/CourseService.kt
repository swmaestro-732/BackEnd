package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceImageResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommand
import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CoursePlace
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.user.application.port.inbound.CourseInteractionUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 유스케이스 — 상세 조회와 생성을 함께 담당한다.
 *
 * 조회(getDetail): status=ACTIVE·미삭제만 반환(그 외 404), PRIVATE 은 소유자만, FRIENDS 는 현재 PUBLIC 취급(후속).
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
) : CourseUseCase {
    override fun getDetail(
        courseId: Long,
        viewerId: Long?,
    ): CourseDetailResult {
        val course =
            coursePersistencePort.findCourseDetail(courseId)
                ?: throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")

        if (course.status != CourseStatus.ACTIVE) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")
        }
        if (course.visibility == CourseVisibility.PRIVATE && viewerId != course.userId) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")
        }
        // 친구 공개 예외처리 필요

        val places =
            coursePersistencePort.findPlaces(courseId).map { place ->
                CoursePlaceResult(
                    id = place.id,
                    placeId = place.placeId,
                    orderNo = place.orderNo,
                    caption = place.caption,
                    walkingMinutesToNext = place.walkingMinutes,
                    images = place.images.map { CoursePlaceImageResult(it.imageUrl, it.orderNo) },
                )
            }

        val viewer = viewerId?.let { courseInteractionUseCase.getViewerState(it, courseId) }

        return CourseDetailResult(
            id = course.id,
            title = course.title,
            coverImageUrl = course.coverImageUrl.orEmpty(),
            theme = course.category?.name,
            description = course.description.orEmpty(),
            authorId = course.userId,
            tracingsCnt = course.tracingsCnt,
            places = places,
            hasSaved = viewer?.hasSaved ?: false,
            hasStartedCourse = viewer?.hasStartedCourse ?: false,
        )
    }

    @Transactional
    override fun create(command: CreateCourseCommand): Course {
        // fork 원본 코스가 실제로 존재하는지 검증(없으면 404).
        command.forkedFromId?.let { forkedFromId ->
            if (!coursePersistencePort.existsById(forkedFromId)) {
                throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "원본 코스를 찾을 수 없습니다: id=$forkedFromId")
            }
        }

        // 불변식 검증·카테고리 도출은 Course.create 애그리거트 팩토리가 수행한다.
        // 서비스는 카테고리 도출에 필요한 외부 데이터(place 카테고리)만 조회해 넘긴다(발행 코스만 필요).
        val placeCategories =
            if (command.isPublished) {
                placeQueryUseCase
                    .findPlacesById(command.places.map { it.placeId })
                    .associate { it.id to it.category }
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
                category = resolveEditedCategory(command, existing, places),
            )
        return coursePersistencePort.update(course)
    }

    /**
     * 편집 코스의 카테고리를 해석한다.
     * - 임시저장이면 null(생성과 동일).
     * - 발행이면서 **기존 카테고리가 있고 장소 구성이 그대로면** 재도출 없이 기존 값을 유지한다 —
     *   장소를 바꾸지 않은 편집(제목·설명 등)에서 외부 place 데이터 드리프트로 카테고리가 바뀌는 것을 막고,
     *   불필요한 place 카테고리 조회도 생략한다.
     * - 그 외(초안→발행, 장소 변경, 카테고리 미지정 코스)에만 place 카테고리를 조회해 도출한다(생성과 동일 규칙).
     */
    private fun resolveEditedCategory(
        command: EditCourseCommand,
        existing: CourseDetailRow,
        places: List<CoursePlace>,
    ): CourseCategory? {
        if (!command.isPublished) return null
        if (existing.category != null && !placesChanged(command.courseId, places)) {
            return existing.category
        }
        val placeCategories =
            placeQueryUseCase
                .findPlacesById(places.map { it.placeId })
                .associate { it.id to it.category }
        return Course.deriveCategory(command.isPublished, places, placeCategories)
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
