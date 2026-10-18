package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.port.inbound.CourseTraceUseCase
import com.example.backend.course.application.port.inbound.dto.CheckInPlaceCommand
import com.example.backend.course.application.port.inbound.dto.CourseTraceResult
import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseTracePersistencePort
import com.example.backend.course.application.port.outbound.CourseTraceRow
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseTraceStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 코스 따라가기 유스케이스 — 시작(IN_PROGRESS 세션 생성 또는 이어가기)·장소 체크인·종료(COMPLETED + tracings_cnt 원자적 +1)·완주 조회.
 * 열람 권한 검사는 CourseLikeService 와 동일하다.
 */
@Service
@Transactional(readOnly = true)
class CourseTraceService(
    private val courseTracePersistencePort: CourseTracePersistencePort,
    private val coursePersistencePort: CoursePersistencePort,
    private val courseViewPolicy: CourseViewPolicy,
) : CourseTraceUseCase {
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
            courseTracePersistencePort.findInProgress(userId, courseId)
                ?: courseTracePersistencePort
                    .insertInProgress(
                        userId,
                        courseId,
                    )?.let { courseTracePersistencePort.findById(it) }
                ?: courseTracePersistencePort.findInProgress(userId, courseId)
                ?: error("따라가기 세션 생성 실패: userId=$userId, courseId=$courseId")
        return CourseTraceResult(
            traceId = trace.id,
            courseId = trace.courseId,
            startedAt = trace.startedAt,
            visitedPlaces = courseTracePersistencePort.findVisitedPlaces(trace.id),
        )
    }

    @Transactional
    override fun checkIn(command: CheckInPlaceCommand) {
        val trace = ownedInProgress(command.userId, command.traceId)
        requirePlacesInCourse(trace.courseId, listOf(command.placeId))
        courseTracePersistencePort.insertVisitedPlace(trace.id, command.placeId, command.visitedAt)
    }

    @Transactional
    override fun end(command: EndCourseTraceCommand) {
        val trace = ownedInProgress(command.userId, command.traceId)
        requirePlacesInCourse(trace.courseId, command.visitedPlaces.map { it.placeId })
        command.visitedPlaces.forEach {
            courseTracePersistencePort.insertVisitedPlace(trace.id, it.placeId, it.visitedAt)
        }
        // 0행이면 그 사이 다른 요청이 먼저 종료한 것 — 체크인 병합까지 롤백한다.
        if (courseTracePersistencePort.complete(trace.id, command.durationMinutes, command.distanceMeters) == 0) {
            throw BusinessException(CourseErrorCode.COURSE_TRACE_ALREADY_ENDED, "이미 종료된 따라가기입니다: traceId=${trace.id}")
        }
        // 0행이면 코스가 그 사이 비활성된 것 → 종료까지 롤백해 기록·카운터 불일치를 막는다.
        if (coursePersistencePort.increaseTracingsCount(trace.courseId) == 0) {
            throw BusinessException(
                CourseErrorCode.COURSE_NOT_FOUND,
                "따라가기를 종료할 코스를 찾을 수 없습니다: courseId=${trace.courseId}",
            )
        }
    }

    override fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant> = courseTracePersistencePort.findCompletedAt(userId, courseIds)

    override fun findTracedCourseIds(userId: Long): Set<Long> = courseTracePersistencePort.findTracedCourseIds(userId)

    @Transactional
    override fun purgeByUser(userId: Long) = courseTracePersistencePort.deleteAllByUser(userId)

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

    /** 내 세션이 아니면(없음 포함) 404 — 타인 세션의 존재를 드러내지 않는다. 진행 중이 아니면 409. */
    private fun ownedInProgress(
        userId: Long,
        traceId: Long,
    ): CourseTraceRow {
        val trace = courseTracePersistencePort.findById(traceId)
        if (trace == null || trace.userId != userId) {
            throw BusinessException(CourseErrorCode.COURSE_TRACE_NOT_FOUND, "따라가기를 찾을 수 없습니다: traceId=$traceId")
        }
        if (trace.status != CourseTraceStatus.IN_PROGRESS) {
            throw BusinessException(CourseErrorCode.COURSE_TRACE_ALREADY_ENDED, "이미 종료된 따라가기입니다: traceId=$traceId")
        }
        return trace
    }
}
