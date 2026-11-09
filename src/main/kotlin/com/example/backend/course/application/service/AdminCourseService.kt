package com.example.backend.course.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.common.domain.CourseVisibility
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.event.CourseDeletedEvent
import com.example.backend.course.application.event.CourseModeratedEvent
import com.example.backend.course.application.event.CourseSavedEvent
import com.example.backend.course.application.port.inbound.AdminCourseUseCase
import com.example.backend.course.application.port.inbound.CourseModerationAction
import com.example.backend.course.application.port.inbound.dto.AdminCourseAuthor
import com.example.backend.course.application.port.inbound.dto.AdminCourseAuthorType
import com.example.backend.course.application.port.inbound.dto.AdminCourseDetail
import com.example.backend.course.application.port.inbound.dto.AdminCourseItem
import com.example.backend.course.application.port.inbound.dto.AdminCoursePage
import com.example.backend.course.application.port.inbound.dto.AdminCoursePlace
import com.example.backend.course.application.port.inbound.dto.AdminCourseQuery
import com.example.backend.course.application.port.inbound.dto.AdminCourseStatus
import com.example.backend.course.application.port.inbound.dto.AdminEditCourseCommand
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceImageResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.inbound.dto.CoursePreviewCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommand
import com.example.backend.course.application.port.inbound.dto.toCoursePlaces
import com.example.backend.course.application.port.outbound.AdminActionPort
import com.example.backend.course.application.port.outbound.AdminCourseFilter
import com.example.backend.course.application.port.outbound.AdminCoursePersistencePort
import com.example.backend.course.application.port.outbound.AdminCourseRow
import com.example.backend.course.application.port.outbound.CourseAuthorPort
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseTagQueryPort
import com.example.backend.course.application.port.outbound.PlaceLookupPort
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CourseStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.temporal.ChronoUnit

/**
 * 어드민 코스 관리: 목록, 상세, 큐레이터 코스 편집, 숨김과 해제, 삭제와 복원, 미리보기.
 *
 * 상태 전이는 사용자 경로와 같은 이벤트를 발행한다: 숨김, 해제, 복원은 [CourseSavedEvent], 삭제는 [CourseDeletedEvent].
 * 검색 색인은 [Course.isPubliclyVisible] 로 저장과 삭제를 고르고, 작성자 코스 개수는 [Course.countedVisibility] 전이로 움직인다.
 * 조치마다 [AdminActionPort] 로 같은 트랜잭션에 감사 기록을 남기고, 사용자 코스를 숨기거나 지우면 작성자에게 알린다.
 * 일괄 조치는 컨트롤러가 건마다 이 서비스를 불러 건별 트랜잭션으로 처리한다.
 */
@Service
@Transactional
class AdminCourseService(
    private val adminCoursePersistencePort: AdminCoursePersistencePort,
    private val coursePersistencePort: CoursePersistencePort,
    private val courseTagQueryPort: CourseTagQueryPort,
    private val placeLookupPort: PlaceLookupPort,
    private val courseAuthorPort: CourseAuthorPort,
    private val adminActionPort: AdminActionPort,
    private val areaQueryUseCase: AreaQueryUseCase,
    private val courseService: CourseService,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
    @Value("\${admin.curator-user-id:2}") private val curatorUserId: Long,
) : AdminCourseUseCase {
    @Transactional(readOnly = true)
    override fun list(query: AdminCourseQuery): AdminCoursePage {
        val keyword = query.q?.trim()?.takeIf { it.isNotEmpty() }
        val filter =
            AdminCourseFilter(
                status = query.status,
                authorId = curatorUserId.takeIf { query.author == AdminCourseAuthorType.CURATOR },
                excludeAuthorId = curatorUserId.takeIf { query.author == AdminCourseAuthorType.USER },
                areaCodePrefix = query.areaCode?.trim()?.takeIf { it.isNotEmpty() },
                category = query.theme,
                keyword = keyword,
                keywordAuthorIds =
                    keyword
                        ?.let {
                            courseAuthorPort.findIdsByNickname(
                                it,
                                MAX_NICKNAME_MATCHES,
                            )
                        }.orEmpty(),
                keywordCourseId = keyword?.toLongOrNull(),
            )
        val rows = adminCoursePersistencePort.findPage(filter, query.page.toLong() * query.size, query.size)
        val stats = coursePersistencePort.findPlaceStats(rows.map { it.id })
        val nicknames = courseAuthorPort.findNicknames(rows.map { it.userId }.toSet())
        return AdminCoursePage(
            items =
                rows.map {
                    AdminCourseItem(
                        id = it.id,
                        title = it.title,
                        coverImageUrl = it.coverImageUrl,
                        themes = it.categories.map(CourseCategory::name),
                        author = AdminCourseAuthor(it.userId, nicknames[it.userId]),
                        areaCode = it.areaCode,
                        areaName = it.area,
                        placeCount = stats[it.id]?.placeCount ?: 0,
                        status = AdminCourseStatus.of(it.status, it.isPublished),
                        isPublished = it.isPublished,
                        visibility = it.visibility,
                        likesCnt = it.likesCnt,
                        savesCnt = it.savesCnt,
                        // ponytail: 신고 집계는 BE-4(신고 관리)에서 report 도메인 포트로 채운다. 그 전까지 0.
                        pendingReportCount = 0,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                    )
                },
            page = query.page,
            size = query.size,
            total = adminCoursePersistencePort.count(filter),
        )
    }

    @Transactional(readOnly = true)
    override fun getDetail(courseId: Long): AdminCourseDetail {
        val course = requireCourse(courseId)
        val coursePlaces = coursePersistencePort.findPlaces(courseId)
        val placesById =
            placeLookupPort
                .findPlacesByIds(
                    coursePlaces.map { it.placeId }.distinct(),
                ).associateBy { it.id }
        return AdminCourseDetail(
            id = course.id,
            title = course.title,
            description = course.description,
            coverImageUrl = course.coverImageUrl,
            themes = course.categories.map(CourseCategory::name),
            companions = course.categories.filter { it in CourseCategory.COMPANIONS }.map(CourseCategory::name),
            tags = courseTagQueryPort.findTagNamesByCourseId(courseId),
            areaCode = course.areaCode,
            areaName = course.area,
            status = AdminCourseStatus.of(course.status, course.isPublished),
            isPublished = course.isPublished,
            visibility = course.visibility,
            author =
                AdminCourseAuthor(
                    course.userId,
                    courseAuthorPort.findNicknames(listOf(course.userId))[course.userId],
                ),
            places =
                coursePlaces.map {
                    val place = placesById[it.placeId]
                    AdminCoursePlace(
                        placeId = it.placeId,
                        orderNo = it.orderNo,
                        name = place?.name,
                        category = place?.category,
                        latitude = place?.coordinate?.latitude,
                        longitude = place?.coordinate?.longitude,
                        address = place?.address,
                        caption = it.caption,
                        imageUrls = it.images.sortedBy { image -> image.orderNo }.map { image -> image.imageUrl },
                        walkingMinutes = it.walkingMinutes,
                    )
                },
            likesCnt = course.likesCnt,
            savesCnt = course.savesCnt,
            createdAt = course.createdAt,
            updatedAt = course.updatedAt,
        )
    }

    /** 사용자 편집 경로([CourseService.editCourse])를 작성자 검사 없이 탄다. 발행 규칙(장소 수, 커버, 장소 사진)은 그대로다. */
    override fun edit(command: AdminEditCourseCommand): AdminCourseDetail {
        val course = requireCourse(command.courseId, lock = true)
        Course.ensureAdminEditable(course.status, course.userId, curatorUserId)
        if (course.updatedAt.truncatedTo(ChronoUnit.MICROS) !=
            command.expectedUpdatedAt.truncatedTo(ChronoUnit.MICROS)
        ) {
            throw BusinessException(CourseErrorCode.COURSE_MODIFIED_CONCURRENTLY)
        }
        val existing =
            coursePersistencePort.findCourseDetail(command.courseId)
                ?: throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND)
        courseService.editCourse(
            existing,
            EditCourseCommand(
                courseId = command.courseId,
                userId = course.userId,
                title = command.title,
                description = command.description,
                coverImageUrl = command.coverImageUrl,
                tags = command.tags,
                visibility = command.visibility,
                isPublished = command.isPublished,
                companions = command.companions,
                places = command.places,
            ),
            byAdmin = true,
        )
        adminActionPort.record(command.adminEmail, command.courseId, ACTION_UPDATE)
        return getDetail(command.courseId)
    }

    override fun hide(
        adminEmail: String,
        courseId: Long,
    ) {
        val course = requireCourse(courseId, lock = true)
        val hidden =
            adminCoursePersistencePort.changeStatus(courseId, CourseStatus.ACTIVE, CourseStatus.HIDDEN)
                ?: throw BusinessException(CourseErrorCode.COURSE_STATUS_CONFLICT, "활성 코스만 숨길 수 있습니다: id=$courseId")
        eventPublisher.publishEvent(CourseSavedEvent(hidden, oldVisibility = course.countedVisibility()))
        adminActionPort.record(adminEmail, courseId, ACTION_HIDE)
        notifyAuthor(course, CourseModerationAction.HIDDEN)
    }

    override fun unhide(
        adminEmail: String,
        courseId: Long,
    ) {
        requireCourse(courseId, lock = true)
        val unhidden =
            adminCoursePersistencePort.changeStatus(courseId, CourseStatus.HIDDEN, CourseStatus.ACTIVE)
                ?: throw BusinessException(CourseErrorCode.COURSE_STATUS_CONFLICT, "숨긴 코스만 해제할 수 있습니다: id=$courseId")
        // 숨김 동안은 개수에 잡히지 않았으므로 old=null 에서 다시 들어온다.
        eventPublisher.publishEvent(CourseSavedEvent(unhidden, oldVisibility = null))
        adminActionPort.record(adminEmail, courseId, ACTION_UNHIDE)
    }

    /** 사용자 삭제와 같은 소프트 삭제와 [CourseDeletedEvent]: 숨김 코스는 이미 개수에서 빠져 old=null 이다. */
    override fun delete(
        adminEmail: String,
        courseId: Long,
    ) {
        val course = requireCourse(courseId, lock = true)
        if (course.status == CourseStatus.DELETED || coursePersistencePort.softDelete(courseId) == 0) {
            throw BusinessException(CourseErrorCode.COURSE_STATUS_CONFLICT, "이미 삭제된 코스입니다: id=$courseId")
        }
        eventPublisher.publishEvent(CourseDeletedEvent(courseId, course.userId, course.countedVisibility()))
        adminActionPort.record(adminEmail, courseId, ACTION_DELETE)
        notifyAuthor(course, CourseModerationAction.DELETED)
    }

    override fun restore(
        adminEmail: String,
        courseId: Long,
    ) {
        requireCourse(courseId, lock = true)
        val restored =
            adminCoursePersistencePort.restore(courseId)
                ?: throw BusinessException(CourseErrorCode.COURSE_STATUS_CONFLICT, "삭제된 코스만 복원할 수 있습니다: id=$courseId")
        eventPublisher.publishEvent(CourseSavedEvent(restored, oldVisibility = null))
        adminActionPort.record(adminEmail, courseId, ACTION_RESTORE)
    }

    /**
     * 실제 코스 상세(CourseQueryService.getDetails)와 같은 모양으로 계산만 한다. 읽기 전용 트랜잭션이라 쓰기가 없고,
     * 도보 시간은 입력값을 그대로 쓴다(외부 도보 API 미호출). 없는 장소는 테마 계산에서 빠지고 화면에서 이름이 비어 보인다.
     */
    @Transactional(readOnly = true)
    override fun preview(command: CoursePreviewCommand): CourseDetailResult {
        val places = command.places.toCoursePlaces().sortedBy { it.orderNo }
        val foundPlaces = placeLookupPort.findPlacesByIds(places.map { it.placeId }.distinct())
        val areaCode =
            Course.deriveAreaCode(
                isPublished = true,
                places = places,
                placeAreaCodeByPlaceId = foundPlaces.associate { it.id to it.areaCode },
            )
        return CourseDetailResult(
            id = command.courseId ?: 0L,
            title = command.title,
            coverImageUrl = command.coverImageUrl.orEmpty(),
            themes =
                Course
                    .publishedCategories(
                        command.companions.map(::toCategory),
                        places,
                        foundPlaces.associate { it.id to it.category },
                    ).map(CourseCategory::name),
            area = areaCode?.let { areaQueryUseCase.findAreaByCode(it)?.shortName },
            tags =
                command.tags
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct(),
            description = command.description.orEmpty(),
            visibility = CourseVisibility.PUBLIC,
            isPublished = true,
            authorId = command.authorId ?: curatorUserId,
            tracingsCnt = 0,
            places =
                places.map {
                    CoursePlaceResult(
                        id = 0L,
                        placeId = it.placeId,
                        orderNo = it.orderNo,
                        caption = it.caption,
                        walkingMinutesToNext = it.walkingMinutes,
                        images = it.imageUrls.mapIndexed { index, url -> CoursePlaceImageResult(url, index) },
                    )
                },
            hasSaved = false,
            hasLiked = false,
            hasStartedCourse = false,
        )
    }

    private fun toCategory(name: String): CourseCategory =
        CourseCategory.entries.find { it.name == name }
            ?: throw BusinessException(CommonErrorCode.INVALID_INPUT, "알 수 없는 테마입니다: $name")

    private fun requireCourse(
        courseId: Long,
        lock: Boolean = false,
    ): AdminCourseRow =
        adminCoursePersistencePort.findById(courseId, lock)
            ?: throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")

    private fun AdminCourseRow.countedVisibility(): CourseVisibility? =
        Course.countedVisibility(status, isPublished, visibility)

    /** 사용자 코스만 작성자에게 알린다(큐레이터 코스는 운영 계정이라 제외). 알림은 커밋 후 알림 도메인이 남긴다. */
    private fun notifyAuthor(
        course: AdminCourseRow,
        action: CourseModerationAction,
    ) {
        if (course.userId == curatorUserId) return
        eventPublisher.publishEvent(
            CourseModeratedEvent(
                authorId = course.userId,
                courseId = course.id,
                courseTitle = course.title,
                coverImageUrl = course.coverImageUrl,
                action = action,
                occurredAt = clock.instant(),
            ),
        )
    }

    private companion object {
        /** 닉네임 검색으로 모을 작성자 수 상한(검색어가 짧아 너무 많이 맞는 경우 IN 목록을 제한한다). */
        const val MAX_NICKNAME_MATCHES = 100

        const val ACTION_UPDATE = "UPDATE"
        const val ACTION_HIDE = "HIDE"
        const val ACTION_UNHIDE = "UNHIDE"
        const val ACTION_DELETE = "DELETE"
        const val ACTION_RESTORE = "RESTORE"
    }
}
