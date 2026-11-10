package com.example.backend.course.application.service

import com.example.backend.common.domain.CourseTraceStatus
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.course.application.port.inbound.CourseTraceUseCase
import com.example.backend.course.application.port.inbound.dto.CheckInPlaceCommand
import com.example.backend.course.application.port.inbound.dto.CompletedCourseTrace
import com.example.backend.course.application.port.inbound.dto.CoursePlaceImageResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.inbound.dto.CourseTraceDetailResult
import com.example.backend.course.application.port.inbound.dto.CourseTraceResult
import com.example.backend.course.application.port.inbound.dto.CourseTraceSummary
import com.example.backend.course.application.port.inbound.dto.CourseTracesQuery
import com.example.backend.course.application.port.inbound.dto.CourseTracesResult
import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand
import com.example.backend.course.application.port.inbound.dto.TracePlaceResult
import com.example.backend.course.application.port.inbound.dto.TraceRouteGuideResult
import com.example.backend.course.application.port.inbound.dto.TraceRouteResult
import com.example.backend.course.application.port.inbound.dto.TraceWalkingRouteResult
import com.example.backend.course.application.port.inbound.dto.TracedCourseSummary
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceRow
import com.example.backend.course.application.port.outbound.CourseTracePersistencePort
import com.example.backend.course.application.port.outbound.CourseTraceRow
import com.example.backend.course.application.port.outbound.PlaceLookupPort
import com.example.backend.course.application.port.outbound.PlaceRef
import com.example.backend.course.application.port.outbound.WalkingRoutePort
import com.example.backend.course.domain.model.CourseStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 코스 따라가기 유스케이스 — 시작(IN_PROGRESS 세션 생성 또는 이어가기)·장소 체크인·다음 장소 도보 경로·종료(COMPLETED + tracings_cnt 원자적 +1)·
 * 내 완주 목록(커서 페이지)·완주 조회. 진행 중 세션은 사용자당 하나다.
 * 열람 권한 검사는 CourseLikeService 와 동일하다.
 */
@Service
@Transactional(readOnly = true)
class CourseTraceService(
    private val courseTracePersistencePort: CourseTracePersistencePort,
    private val coursePersistencePort: CoursePersistencePort,
    private val courseViewPolicy: CourseViewPolicy,
    private val placeLookupPort: PlaceLookupPort,
    private val walkingRoutePort: WalkingRoutePort,
) : CourseTraceUseCase {
    private val log = KotlinLogging.logger {}

    @Transactional
    override fun start(
        userId: Long,
        courseId: Long,
    ): CourseTraceResult {
        val course = coursePersistencePort.findCourseDetail(courseId)
        if (course == null ||
            course.status != CourseStatus.ACTIVE ||
            !courseViewPolicy.isViewable(course.visibility, course.userId, userId)
        ) {
            throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND, "따라가기를 시작할 코스를 찾을 수 없습니다: courseId=$courseId")
        }

        // 진행 중 세션이 있으면 이어간다. 없으면 만들되, 동시 시작으로 삽입이 무시되면(부분 유니크) 그 사이 생긴 세션을 다시 읽는다.
        val trace =
            courseTracePersistencePort.findInProgress(userId)
                ?: courseTracePersistencePort
                    .insertInProgress(
                        userId,
                        courseId,
                    )?.let { courseTracePersistencePort.findById(it) }
                ?: courseTracePersistencePort.findInProgress(userId)
                ?: error("따라가기 세션 생성 실패: userId=$userId, courseId=$courseId")
        if (trace.courseId != courseId) {
            throw BusinessException(
                CourseErrorCode.COURSE_TRACE_ALREADY_IN_PROGRESS,
                "이미 따라가는 중인 코스가 있습니다: traceId=${trace.id}, courseId=${trace.courseId}",
            )
        }
        return trace.toResult()
    }

    override fun listCompleted(query: CourseTracesQuery): CourseTracesResult {
        val beforeId = CourseTraceCursorCodec.decode(query.cursor)
        // hasNext 판정을 위해 한 개 더 조회한 뒤 페이지 크기만큼 잘라낸다.
        val rows = courseTracePersistencePort.findCompleted(query.userId, beforeId, query.size + 1)
        val hasNext = rows.size > query.size
        val page = rows.take(query.size)
        val courses = findTracedCourses(query.userId, page.map { it.courseId }.distinct())
        return CourseTracesResult(
            nextCursor = if (hasNext) CourseTraceCursorCodec.encode(page.last().id) else null,
            hasNext = hasNext,
            traces =
                page.map {
                    CompletedCourseTrace(
                        trace = it.toSummary(),
                        course = courses[it.courseId] ?: TracedCourseSummary.deleted(it.courseId),
                    )
                },
        )
    }

    override fun findInProgress(userId: Long): CourseTraceDetailResult? =
        courseTracePersistencePort.findInProgress(userId)?.let { toDetail(userId, it) }

    override fun getDetail(
        userId: Long,
        traceId: Long,
    ): CourseTraceDetailResult {
        val trace = requireOwned(courseTracePersistencePort.readById(traceId), userId, traceId)
        return toDetail(userId, trace)
    }

    private fun toDetail(
        viewerId: Long,
        trace: CourseTraceRow,
    ) = CourseTraceDetailResult(
        traceId = trace.id,
        status = trace.status,
        startedAt = trace.startedAt,
        endedAt = trace.endedAt,
        durationMinutes = trace.durationMinutes,
        distanceMeters = trace.distanceMeters,
        course =
            findTracedCourses(viewerId, listOf(trace.courseId))[trace.courseId]
                ?: TracedCourseSummary.deleted(trace.courseId),
        // 코스 장소는 코스 삭제·비공개와 무관하게 내려준다 — 따라가던 화면을 그대로 이어 그리기 위해.
        places = coursePersistencePort.findPlaces(trace.courseId).map { it.toResult() },
        visitedPlaces = courseTracePersistencePort.findVisitedPlaces(trace.id),
    )

    @Transactional
    override fun checkIn(command: CheckInPlaceCommand) {
        val trace = ownedInProgress(command.userId, command.traceId)
        requirePlacesInCourse(trace.courseId, listOf(command.placeId))
        courseTracePersistencePort.insertVisitedPlace(trace.id, command.placeId, command.visitedAt)
    }

    @Transactional
    override fun end(command: EndCourseTraceCommand) {
        val trace = ownedInProgress(command.userId, command.traceId)
        // 따라가는 동안 코스에서 빠진 장소(어드민 편집 등)는 400 으로 완주를 막지 않고 기록에서만 뺀다.
        val coursePlaceIds = coursePersistencePort.findPlaces(trace.courseId).mapTo(mutableSetOf()) { it.placeId }
        val (inCourse, dropped) = command.visitedPlaces.partition { it.placeId in coursePlaceIds }
        if (dropped.isNotEmpty()) {
            log.info { "코스에 없는 방문 장소 제외: traceId=${trace.id}, placeIds=${dropped.map { it.placeId }}" }
        }
        inCourse.forEach {
            courseTracePersistencePort.insertVisitedPlace(trace.id, it.placeId, it.visitedAt)
        }
        // 0행이면 그 사이 다른 요청이 먼저 종료한 것 — 체크인 병합까지 롤백한다.
        if (courseTracePersistencePort.complete(trace.id, command.durationMinutes, command.distanceMeters) == 0) {
            throw BusinessException(CourseErrorCode.COURSE_TRACE_ALREADY_ENDED, "이미 종료된 따라가기입니다: traceId=${trace.id}")
        }
        // 코스가 삭제됐으면 0행 — 카운터만 건너뛰고 종료는 그대로 기록한다.
        coursePersistencePort.increaseTracingsCount(trace.courseId)
    }

    /**
     * 외부 경로 API(Tmap) 호출을 DB 트랜잭션 밖에서 한다 — 응답이 느려도 커넥션을 쥐지 않게.
     * 장소 조회는 place 도메인 서비스가 자체 읽기 트랜잭션을 연다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun findRoute(
        fromPlaceId: Long,
        toPlaceId: Long,
    ): TraceRouteResult {
        if (fromPlaceId == toPlaceId) {
            throw BusinessException(CommonErrorCode.INVALID_INPUT, "출발 장소와 도착 장소가 같습니다: placeId=$fromPlaceId")
        }
        val (from, to) = requirePlacesInOrder(listOf(fromPlaceId, toPlaceId))
        val route = walkingRoutePort.findWalkingRoute(from.coordinate, to.coordinate)
        return TraceRouteResult(
            from = from.toTracePlace(),
            to = to.toTracePlace(),
            route =
                route?.let {
                    TraceWalkingRouteResult(
                        distanceMeters = it.distanceMeters,
                        seconds = it.seconds,
                        path = it.path,
                        guides = it.guides.map { guide -> TraceRouteGuideResult(guide.coordinate, guide.description) },
                    )
                },
        )
    }

    override fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant> = courseTracePersistencePort.findCompletedAt(userId, courseIds)

    override fun findTracedCourseIds(userId: Long): Set<Long> = courseTracePersistencePort.findTracedCourseIds(userId)

    @Transactional
    override fun purgeByUser(userId: Long) = courseTracePersistencePort.deleteAllByUser(userId)

    /** 조회자가 볼 수 있는 코스의 요약만 돌려준다 — 삭제·비활성·비공개 전환된 코스는 결과에서 빠진다. */
    private fun findTracedCourses(
        viewerId: Long,
        courseIds: List<Long>,
    ): Map<Long, TracedCourseSummary> {
        if (courseIds.isEmpty()) return emptyMap()
        val viewable =
            coursePersistencePort.findCourseDetails(courseIds).filter {
                it.status == CourseStatus.ACTIVE && courseViewPolicy.isViewable(it.visibility, it.userId, viewerId)
            }
        val placeStats = coursePersistencePort.findPlaceStats(viewable.map { it.id })
        return viewable.associate {
            it.id to
                TracedCourseSummary(
                    courseId = it.id,
                    title = it.title,
                    coverImageUrl = it.coverImageUrl,
                    area = it.area,
                    placeCount = placeStats[it.id]?.placeCount ?: 0,
                    isDeleted = false,
                )
        }
    }

    /** 방문 기록은 코스 장소만 받는다 — 하나라도 코스에 없으면 400. 게시 코스는 장소 구성이 불변이라 요청 시점 검사로 충분하다. */
    private fun requirePlacesInCourse(
        courseId: Long,
        placeIds: List<Long>,
    ) {
        if (placeIds.isEmpty()) return
        val coursePlaceIds = coursePersistencePort.findPlaces(courseId).mapTo(mutableSetOf()) { it.placeId }
        val outsiders = placeIds.filterNot { it in coursePlaceIds }
        if (outsiders.isNotEmpty()) {
            throw BusinessException(
                CourseErrorCode.PLACE_NOT_IN_COURSE,
                "코스에 없는 장소입니다: courseId=$courseId, placeIds=$outsiders",
            )
        }
    }

    private fun CourseTraceRow.toResult() =
        CourseTraceResult(
            traceId = id,
            courseId = courseId,
            startedAt = startedAt,
            visitedPlaces = courseTracePersistencePort.findVisitedPlaces(id),
        )

    private fun CoursePlaceRow.toResult() =
        CoursePlaceResult(
            id = id,
            placeId = placeId,
            orderNo = orderNo,
            caption = caption,
            walkingMinutesToNext = walkingMinutes,
            images = images.map { CoursePlaceImageResult(it.imageUrl, it.orderNo) },
        )

    private fun CourseTraceRow.toSummary() =
        CourseTraceSummary(
            id = id,
            courseId = courseId,
            startedAt = startedAt,
            endedAt = endedAt,
            durationMinutes = durationMinutes,
            distanceMeters = distanceMeters,
        )

    private fun PlaceRef.toTracePlace() = TracePlaceResult(placeId = id, name = name, coordinate = coordinate)

    /** 요청한 순서대로 [PlaceRef] 를 돌려준다 — 하나라도 없으면 404. (조회 결과 순서는 보장되지 않아 id 로 다시 맞춘다.) */
    private fun requirePlacesInOrder(placeIds: List<Long>): List<PlaceRef> {
        val placesById = placeLookupPort.findPlacesByIds(placeIds).associateBy { it.id }
        return placeIds.map {
            placesById[it] ?: throw BusinessException(PlaceErrorCode.PLACE_NOT_FOUND, "장소를 찾을 수 없습니다: placeId=$it")
        }
    }

    /** 세션을 잠그고 읽어 검사한다 — 체크인·종료가 같은 잠금으로 직렬화된다. */
    private fun ownedInProgress(
        userId: Long,
        traceId: Long,
    ): CourseTraceRow = requireOwnedInProgress(courseTracePersistencePort.findById(traceId), userId, traceId)

    /** 내 세션이 아니면(없음 포함) 404 — 타인 세션의 존재를 드러내지 않는다. */
    private fun requireOwned(
        trace: CourseTraceRow?,
        userId: Long,
        traceId: Long,
    ): CourseTraceRow {
        if (trace == null || trace.userId != userId) {
            throw BusinessException(CourseErrorCode.COURSE_TRACE_NOT_FOUND, "따라가기를 찾을 수 없습니다: traceId=$traceId")
        }
        return trace
    }

    /** [requireOwned] 에 더해 진행 중이 아니면 409. */
    private fun requireOwnedInProgress(
        trace: CourseTraceRow?,
        userId: Long,
        traceId: Long,
    ): CourseTraceRow {
        requireOwned(trace, userId, traceId)
        if (trace!!.status != CourseTraceStatus.IN_PROGRESS) {
            throw BusinessException(CourseErrorCode.COURSE_TRACE_ALREADY_ENDED, "이미 종료된 따라가기입니다: traceId=$traceId")
        }
        return trace
    }
}
