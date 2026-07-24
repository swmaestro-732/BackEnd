package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceImageResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.domain.model.Course
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
    override fun create(command: CreateCourseCommand): Long {
        // 불변식 검증·카테고리 도출은 Course.create 애그리거트 팩토리가 수행한다.
        // 서비스는 카테고리 도출에 필요한 외부 데이터(place 카테고리)만 조회해 넘긴다(발행 코스만 필요).
        val placeCategories =
            if (command.isPublished) {
                placeQueryUseCase.findCategoryNames(command.places.map { it.placeId })
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
}
