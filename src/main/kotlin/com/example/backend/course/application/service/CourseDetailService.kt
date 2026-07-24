package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.course.application.port.inbound.CourseDetailUseCase
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceImageResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.outbound.CourseQueryPort
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import com.example.backend.user.application.port.inbound.CourseInteractionUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 상세 조회 유스케이스. course 아웃바운드 포트(코스·장소·태그)와
 * user 인바운드 포트(조회자 저장/따라가기 상태)를 조합한다.
 *
 * 노출 규칙: status=ACTIVE 이며 삭제되지 않은 코스만 반환한다(그 외는 404 COURSE_NOT_FOUND).
 * PRIVATE 코스는 소유자만 조회할 수 있고, FRIENDS 는 현재 PUBLIC 과 동일하게 취급한다(팔로우 검사 후속).
 */
@Service
@Transactional(readOnly = true)
class CourseDetailService(
    private val courseQueryPort: CourseQueryPort,
    private val courseInteractionUseCase: CourseInteractionUseCase,
) : CourseDetailUseCase {
    override fun getDetail(
        courseId: Long,
        viewerId: Long?,
    ): CourseDetailResult {
        val course =
            courseQueryPort.findCourseDetail(courseId)
                ?: throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")

        if (course.status != CourseStatus.ACTIVE) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")
        }
        if (course.visibility == CourseVisibility.PRIVATE && viewerId != course.userId) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")
        }
        // 친구 공개 예외처리 필요

        val places =
            courseQueryPort.findPlaces(courseId).map { place ->
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
}
