package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.dto.AuthorCourseCursor
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceImageResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.inbound.dto.CourseSummary
import com.example.backend.course.application.port.inbound.dto.CourseSummaryPage
import com.example.backend.course.application.port.inbound.dto.FeedCursor
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseSummaryRow
import com.example.backend.course.application.port.outbound.CourseTagQueryPort
import com.example.backend.course.application.port.outbound.ViewerInteractionPort
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 조회 유스케이스 — 상세(단건·배치)와 목록(작성자별·공개 피드) 조회를 담당한다(읽기 전용).
 * 쓰기(생성·편집·삭제)는 [com.example.backend.course.application.service.CourseService] 가 맡는다(커맨드/쿼리 분리).
 *
 * 조회 규칙: status=ACTIVE·미삭제만 반환, PRIVATE 은 소유자만, FOLLOWER 는 소유자·팔로워만(그 외 제외/404).
 * 조회자 상태(저장·완주)·팔로우 여부는 user 도메인 소유라 [ViewerInteractionPort](아웃바운드)로만 접근한다.
 */
@Service
@Transactional(readOnly = true)
class CourseQueryService(
    private val coursePersistencePort: CoursePersistencePort,
    private val courseTagQueryPort: CourseTagQueryPort,
    private val viewerInteractionPort: ViewerInteractionPort,
) : CourseQueryUseCase {
    override fun getDetail(
        courseId: Long,
        viewerId: Long?,
    ): CourseDetailResult =
        getDetails(listOf(courseId), viewerId)
            .firstOrNull()
            // 해시태그는 코스 상세 화면에서만 쓰므로 단건 조회에서만 읽는다 — 배치([getDetails])는 쿼리를 늘리지 않는다.
            ?.copy(tags = courseTagQueryPort.findTagNamesByCourseId(courseId))
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
                ?.let { viewerInteractionPort.getViewerStates(it, viewableIds) }
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
                // 배치 조회는 태그를 읽지 않는다(코스 상세 화면 전용) — [getDetail] 이 단건으로 채운다.
                tags = emptyList(),
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

    /** 미삭제 코스 존재 확인(크로스 도메인) — 다른 도메인(user 저장함 등)이 이 포트로만 접근한다. */
    override fun existsById(courseId: Long): Boolean = coursePersistencePort.existsById(courseId)

    /**
     * 작성자의 발행 코스 요약 목록 — 조회자(viewerId) 기준 공개범위(isViewable) 통과분만 내려준다.
     * 본인 조회(viewerId==authorId)면 발행 코스 전체가 통과한다.
     */
    override fun listByAuthor(
        authorId: Long,
        viewerId: Long?,
        cursor: AuthorCourseCursor?,
        size: Int,
    ): CourseSummaryPage {
        val rows =
            coursePersistencePort.findPublishedByAuthor(
                authorId = authorId,
                visibilities = viewableVisibilities(ownerId = authorId, viewerId = viewerId),
                cursor = cursor,
                size = size,
            )
        return CourseSummaryPage(
            items = rows.take(size).map(::toCourseSummary),
            hasNext = rows.size > size,
        )
    }

    /** 작성자 본인의 임시저장 코스 요약 목록 — 공개범위 필터 없이 영속 포트의 최근 수정순을 유지한다. */
    override fun listDraftsByAuthor(authorId: Long): List<CourseSummary> =
        coursePersistencePort
            .findDraftsByAuthor(authorId)
            .map(::toCourseSummary)

    /**
     * 전체 공개(PUBLIC) 발행 코스를 저장수+최신순 복합 커서 페이지로 내려준다(피드용).
     * 정렬은 findPublishedPublic 이 SQL(saves_cnt DESC, created_at DESC, id DESC)로 수행한다.
     * 영속 포트가 [size]보다 한 건 더 조회한 결과로 hasNext를 판정하고 초과분을 잘라낸다.
     * 모두 PUBLIC 이라 공개범위(isViewable) 필터 없이 그대로 매핑한다.
     */
    override fun listPublic(
        cursor: FeedCursor?,
        size: Int,
    ): CourseSummaryPage {
        val effectiveSize = size.coerceAtLeast(1)
        val rows = coursePersistencePort.findPublishedPublic(cursor, effectiveSize)
        return CourseSummaryPage(
            items = rows.take(effectiveSize).map(::toCourseSummary),
            hasNext = rows.size > effectiveSize,
        )
    }

    /** 조회자에게 허용된 공개범위를 SQL 조건으로 전달해 필터 이후에도 페이지 크기와 hasNext가 정확하게 유지되게 한다. */
    private fun viewableVisibilities(
        ownerId: Long,
        viewerId: Long?,
    ): Set<CourseVisibility> =
        when {
            viewerId == ownerId -> {
                CourseVisibility.entries.toSet()
            }

            viewerId != null && viewerInteractionPort.isFollowing(viewerId, ownerId) -> {
                setOf(CourseVisibility.PUBLIC, CourseVisibility.FOLLOWER)
            }

            else -> {
                setOf(CourseVisibility.PUBLIC)
            }
        }

    private fun toCourseSummary(row: CourseSummaryRow) =
        CourseSummary(
            id = row.id,
            authorId = row.userId,
            title = row.title,
            coverImageUrl = row.coverImageUrl,
            theme = row.category?.name,
            likesCnt = row.likesCnt,
            savesCnt = row.savesCnt,
            createdAt = row.createdAt,
        )

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
                    (viewerId != null && viewerInteractionPort.isFollowing(viewerId, ownerId))
            }

            CourseVisibility.PRIVATE -> {
                viewerId == ownerId
            }
        }
}
